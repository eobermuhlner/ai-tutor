package ch.obermuhlner.aitutor.testharness.executor

import ch.obermuhlner.aitutor.testharness.client.ApiClient
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.*
import ch.obermuhlner.aitutor.testharness.judge.JudgeService
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

        // Create session
        val session = apiClient.createSession(
            userId = userId,
            tutorName = scenario.tutorConfig.tutorName,
            sourceLanguageCode = scenario.learnerPersona.sourceLanguage,
            targetLanguageCode = scenario.learnerPersona.targetLanguage,
            conversationPhase = scenario.tutorConfig.initialPhase,
            estimatedCEFRLevel = scenario.learnerPersona.cefrLevel,
            teachingStyle = scenario.tutorConfig.teachingStyle
        )

        logger.info("  Created session: ${session.id}")

        // Execute conversation
        val conversationTurns = mutableListOf<ConversationTurn>()
        var previousPhase = scenario.tutorConfig.initialPhase.name

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
            previousPhase = updatedSession.effectivePhase.name

            // Rate limiting: delay between requests (except after last message)
            if (index < scenario.conversationScript.size - 1) {
                logger.debug("  Waiting ${config.delayBetweenRequestsMs}ms before next request (rate limiting)...")
                Thread.sleep(config.delayBetweenRequestsMs)
            }
        }

        val executionTime = System.currentTimeMillis() - startTime

        // Retrieve final session state
        val finalSession = apiClient.getSessionWithMessages(session.id)
        logger.info("  Conversation completed: ${conversationTurns.size} turns in ${executionTime}ms")

        // Calculate technical metrics
        val metrics = calculateMetrics(scenario, conversationTurns, finalSession)

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

    private fun calculateMetrics(
        scenario: TestScenario,
        turns: List<ConversationTurn>,
        finalSession: ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse
    ): TechnicalMetrics {
        // Count intentional errors in scenario
        val intentionalErrors = scenario.conversationScript.flatMap { it.intentionalErrors }
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
