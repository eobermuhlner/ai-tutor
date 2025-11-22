package ch.obermuhlner.aitutor.testharness.executor

import ch.obermuhlner.aitutor.testharness.client.ApiClient
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.conversation.LlmConversationGenerator
import ch.obermuhlner.aitutor.testharness.domain.*
import ch.obermuhlner.aitutor.testharness.judge.JudgeService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Executes test scenarios by orchestrating API calls and collecting results.
 */
class TestExecutor(private val config: TestHarnessConfig) {
    private val logger = LoggerFactory.getLogger(TestExecutor::class.java)
    private val apiClient = ApiClient(config.apiBaseUrl, config = config)
    private val judgeService = JudgeService(config)

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    /**
     * Creates a course-based session for scenarios that specify a course.
     */
    private fun createCourseBasedSession(scenario: TestScenario, userId: UUID): ch.obermuhlner.aitutor.chat.dto.SessionResponse {
        val courseSlug = scenario.llmConversationConfig?.course
            ?: throw IllegalArgumentException("Course slug must be specified for course-based sessions")

        logger.info("  Creating course-based session for course: $courseSlug")

        // Find course by slug using the new API client method
        val course = apiClient.findCourseBySlug(courseSlug, scenario.learnerPersona.sourceLanguage)
            ?: throw IllegalArgumentException("Course not found with slug: $courseSlug")

        val courseId = UUID.fromString(course["id"] as? String ?: throw IllegalArgumentException("Invalid course ID"))
        val courseName = try {
            val nameMap = objectMapper.readValue(course["nameJson"] as String, object : com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {})
            nameMap["en"] ?: "Unknown Course"
        } catch (e: Exception) {
            "Unknown Course"
        }

        logger.info("  Found course: $courseName (ID: $courseId, Slug: $courseSlug)")

        // Find a tutor for this language
        val courseLanguageCode = course["languageCode"] as? String ?: throw IllegalArgumentException("Course language code is missing")
        val tutors = apiClient.getTutorsForLanguage(courseLanguageCode, scenario.learnerPersona.sourceLanguage)
        if (tutors.isEmpty()) {
            throw IllegalArgumentException("No tutors found for language: $courseLanguageCode")
        }

        // Use the first available tutor for this language
        val tutor = tutors.first()
        val tutorId = UUID.fromString(tutor["id"] as? String ?: throw IllegalArgumentException("Invalid tutor ID"))
        val tutorName = tutor["name"] as? String ?: "Unknown Tutor"

        logger.info("  Using tutor: $tutorName (ID: $tutorId)")

        // Create course-based session
        return apiClient.createSessionFromCourse(
            userId = userId,
            courseTemplateId = courseId,
            tutorProfileId = tutorId,
            sourceLanguageCode = scenario.learnerPersona.sourceLanguage
        )
    }

    /**
     * Execute multiple scenarios and return results.
     */
    fun executeScenarios(scenarios: List<TestScenario>): List<ScenarioResult> {
        logger.info("📋 Executing ${scenarios.size} scenario(s)...")

        // Login
        apiClient.login(config.apiUsername, config.apiPassword)
        val userId = apiClient.getCurrentUserId()

        return scenarios.mapIndexed { index, scenario ->
            logger.info("▶️  Executing scenario ${index + 1}/${scenarios.size}: ${scenario.name}")
            try {
                executeScenario(scenario, userId)
            } catch (e: Exception) {
                logger.error("❌ Scenario '${scenario.name}' failed: ${e.message}", e)
                createFailedResult(scenario, e)
            }
        }
    }

    /**
     * Execute a single scenario.
     */
    private fun executeScenario(scenario: TestScenario, userId: UUID): ScenarioResult {
        val startTime = System.currentTimeMillis()

        // Create session - either course-based or regular based on scenario configuration
        val session = if (scenario.llmConversationConfig?.course != null) {
            // Create a course-based session
            createCourseBasedSession(scenario, userId)
        } else {
            // Create a regular session
            apiClient.createSession(
                userId = userId,
                tutorName = scenario.tutorConfig.tutorName,
                sourceLanguageCode = scenario.learnerPersona.sourceLanguage,
                targetLanguageCode = scenario.learnerPersona.targetLanguage,
                conversationPhase = scenario.tutorConfig.initialPhase,
                estimatedCEFRLevel = scenario.learnerPersona.cefrLevel,
                teachingStyle = scenario.tutorConfig.teachingStyle
            )
        }

        logger.info("  Created session: ${session.id}")

        // Execute conversation - either with predefined script or using LLM conversation loop
        val conversationTurns = mutableListOf<ConversationTurn>()

        if (scenario.llmConversationConfig != null) {
            // LLM-based conversation: generate messages on-the-fly, allowing response context
            logger.info("  Using LLM to generate conversation messages in conversational loop")
            val generator = LlmConversationGenerator(config)
            
            var previousTutorResponse: String? = null
            
            for (index in 0 until scenario.llmConversationConfig.messageCount) {
                logger.debug("  Generating turn ${index + 1} with context of previous tutor response")
                
                // Generate next learner message with context of previous tutor response
                val learnerMessage = generator.generateSingleConversationMessage(
                    scenario, 
                    scenario.llmConversationConfig, 
                    previousTutorResponse
                )
                
                logger.debug("  Turn ${index + 1}: ${learnerMessage.content}")

                val response = apiClient.sendMessage(session.id, learnerMessage.content)

                // Fetch updated session state to track phase transitions
                val sessionWithMessages = apiClient.getSessionWithMessages(session.id)
                val updatedSession = sessionWithMessages.session

                val turn = ConversationTurn(
                    turnIndex = index + 1,
                    learnerMessage = learnerMessage.content,
                    tutorResponse = TutorResponse(
                        content = response.content,
                        corrections = response.corrections?.map { correction ->
                            DetectedCorrection(
                                span = correction.span,
                                errorType = correction.errorType.name,
                                severity = correction.severity.name,
                                correctedForm = correction.correctedTargetLanguage,
                                explanation = correction.whySourceLanguage
                            )
                        } ?: emptyList(),
                        newVocabulary = response.newVocabulary?.map { it.lemma } ?: emptyList(),
                        wordCards = response.wordCards?.map { it.titleTargetLanguage } ?: emptyList(),
                        currentPhase = updatedSession.effectivePhase.name,  // Use effectivePhase, not conversationPhase
                        currentTopic = updatedSession.currentTopic
                    ),
                    intentionalErrors = emptyList() // LLM-generated messages don't have predefined intentional errors
                )

                conversationTurns.add(turn)
                previousTutorResponse = response.content

                // Rate limiting: delay between requests (except after last message)
                if (index < scenario.llmConversationConfig.messageCount - 1) {
                    logger.debug("  Waiting ${config.delayBetweenRequestsMs}ms before next request (rate limiting)...")
                    Thread.sleep(config.delayBetweenRequestsMs)
                }
            }
        } else {
            // Predefined script: run as before
            logger.info("  Using hardcoded conversation script")
            scenario.conversationScript.forEachIndexed { index, learnerMessage ->
                logger.debug("  Turn ${index + 1}: ${learnerMessage.content}")

                val response = apiClient.sendMessage(session.id, learnerMessage.content)

                // Fetch updated session state to track phase transitions
                val sessionWithMessages = apiClient.getSessionWithMessages(session.id)
                val updatedSession = sessionWithMessages.session

                val turn = ConversationTurn(
                    turnIndex = index + 1,
                    learnerMessage = learnerMessage.content,
                    tutorResponse = TutorResponse(
                        content = response.content,
                        corrections = response.corrections?.map { correction ->
                            DetectedCorrection(
                                span = correction.span,
                                errorType = correction.errorType.name,
                                severity = correction.severity.name,
                                correctedForm = correction.correctedTargetLanguage,
                                explanation = correction.whySourceLanguage
                            )
                        } ?: emptyList(),
                        newVocabulary = response.newVocabulary?.map { it.lemma } ?: emptyList(),
                        wordCards = response.wordCards?.map { it.titleTargetLanguage } ?: emptyList(),
                        currentPhase = updatedSession.effectivePhase.name,  // Use effectivePhase, not conversationPhase
                        currentTopic = updatedSession.currentTopic
                    ),
                    intentionalErrors = learnerMessage.intentionalErrors
                )

                conversationTurns.add(turn)

                // Rate limiting: delay between requests (except after last message)
                if (index < scenario.conversationScript.size - 1) {
                    logger.debug("  Waiting ${config.delayBetweenRequestsMs}ms before next request (rate limiting)...")
                    Thread.sleep(config.delayBetweenRequestsMs)
                }
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        logger.info("  Conversation completed: ${conversationTurns.size} turns in ${executionTime}ms")

        // Calculate technical metrics
        val dummyFinalSession = ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse(
            session = ch.obermuhlner.aitutor.chat.dto.SessionResponse(
                id = session.id,
                userId = userId,
                tutorName = scenario.tutorConfig.tutorName,
                tutorPersona = "", // We don't have this in scenario
                tutorDomain = "", // We don't have this in scenario
                tutorTeachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.valueOf(scenario.tutorConfig.teachingStyle),
                sourceLanguageCode = scenario.learnerPersona.sourceLanguage,
                targetLanguageCode = scenario.learnerPersona.targetLanguage,
                conversationPhase = scenario.tutorConfig.initialPhase,
                effectivePhase = scenario.tutorConfig.initialPhase, // Use initial phase as default
                estimatedCEFRLevel = scenario.learnerPersona.cefrLevel,
                currentTopic = null, // We don't have the final topic
                createdAt = java.time.Instant.now(),
                updatedAt = java.time.Instant.now()
            ),
            messages = emptyList() // Not used in calculation anyway
        )
        val metrics = calculateMetrics(scenario, conversationTurns, dummyFinalSession)

        // Evaluate with LLM judge
        val judgeEvaluation = judgeService.evaluate(scenario, conversationTurns, metrics)
        logger.info("  Judge score: ${"%.1f".format(judgeEvaluation.overallPedagogicalScore)}/100")

        // Clean up session
        apiClient.deleteSession(session.id)

        return ScenarioResult(
            scenarioId = scenario.id,
            scenarioName = scenario.name,
            sessionId = session.id,
            executionTime = Instant.now(),
            conversationTranscript = conversationTurns,
            judgeEvaluation = judgeEvaluation,
            technicalMetrics = metrics,
            overallScore = judgeEvaluation.overallPedagogicalScore
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun calculateMetrics(
        scenario: TestScenario,
        turns: List<ConversationTurn>,
        finalSession: ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse
    ): TechnicalMetrics {
        // Count intentional errors in scenario
        val intentionalErrors = if (scenario.llmConversationConfig != null) {
            // For LLM-generated scenarios, there are no predefined intentional errors
            emptyList()
        } else {
            // For hardcoded scenarios, use the intentional errors from the script
            scenario.conversationScript.flatMap { it.intentionalErrors }
        }
        val totalIntentionalErrors = intentionalErrors.size

        // Count detected corrections
        val allCorrections = turns.flatMap { it.tutorResponse.corrections }
        val detectedErrorSpans = allCorrections.map { it.span.lowercase().trim() }.toSet()
        val intentionalErrorSpans = intentionalErrors.map { it.span.lowercase().trim() }.toSet()

        val correctionsDetected = detectedErrorSpans.intersect(intentionalErrorSpans).size
        val correctionsMissed = intentionalErrorSpans.minus(detectedErrorSpans).size
        val falsePositives = detectedErrorSpans.minus(intentionalErrorSpans).size

        // Track phase transitions
        val phaseTransitions = mutableListOf<PhaseTransitionMetric>()
        var currentPhase = scenario.tutorConfig.initialPhase.name

        turns.forEachIndexed { index, turn ->
            if (turn.tutorResponse.currentPhase != currentPhase) {
                val expectedTransition = scenario.expectedOutcomes.phaseTransitions
                    .find { it.afterMessageIndex == index + 1 }

                phaseTransitions.add(
                    PhaseTransitionMetric(
                        atTurnIndex = index + 1,
                        fromPhase = currentPhase,
                        toPhase = turn.tutorResponse.currentPhase,
                        wasExpected = expectedTransition != null
                    )
                )
                currentPhase = turn.tutorResponse.currentPhase
            }
        }

        // Count topic changes and vocabulary
        val uniqueTopics = turns.mapNotNull { it.tutorResponse.currentTopic }.distinct()
        val totalVocabulary = turns.flatMap { it.tutorResponse.newVocabulary }.distinct()

        return TechnicalMetrics(
            totalMessages = turns.size,
            totalCorrections = totalIntentionalErrors,
            correctionsDetected = correctionsDetected,
            correctionsMissed = correctionsMissed,
            falsePositives = falsePositives,
            phaseTransitions = phaseTransitions,
            topicChanges = maxOf(0, uniqueTopics.size - 1),
            vocabularyItemsIntroduced = totalVocabulary.size,
            averageResponseTimeMs = 0L // Would need to track per-message timing
        )
    }

    private fun createFailedResult(scenario: TestScenario, error: Exception): ScenarioResult {
        return ScenarioResult(
            scenarioId = scenario.id,
            scenarioName = scenario.name,
            sessionId = UUID.randomUUID(),
            executionTime = Instant.now(),
            conversationTranscript = emptyList(),
            judgeEvaluation = JudgeEvaluation(
                errorDetectionScore = 0.0,
                errorDetectionFeedback = "Scenario execution failed",
                phaseAppropriatenessScore = 0.0,
                phaseAppropriatenessFeedback = "Scenario execution failed",
                correctionQualityScore = 0.0,
                correctionQualityFeedback = "Scenario execution failed",
                encouragementBalanceScore = 0.0,
                encouragementBalanceFeedback = "Scenario execution failed",
                topicManagementScore = 0.0,
                topicManagementFeedback = "Scenario execution failed",
                vocabularyTeachingScore = 0.0,
                vocabularyTeachingFeedback = "Scenario execution failed",
                overallPedagogicalScore = 0.0,
                overallFeedback = "Execution failed: ${error.message}",
                strengths = emptyList(),
                improvements = listOf("Fix scenario execution: ${error.message}")
            ),
            technicalMetrics = TechnicalMetrics(
                totalMessages = 0,
                totalCorrections = 0,
                correctionsDetected = 0,
                correctionsMissed = 0,
                falsePositives = 0,
                phaseTransitions = emptyList(),
                topicChanges = 0,
                vocabularyItemsIntroduced = 0,
                averageResponseTimeMs = 0
            ),
            overallScore = 0.0
        )
    }
}
