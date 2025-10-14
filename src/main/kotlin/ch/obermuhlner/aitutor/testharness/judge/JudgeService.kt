package ch.obermuhlner.aitutor.testharness.judge

import ch.obermuhlner.aitutor.testharness.ai.AiProviderFactory
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * LLM-based judge service for evaluating pedagogical quality of tutor conversations.
 *
 * Supports multiple AI providers (OpenAI, Azure OpenAI, Ollama) to systematically evaluate
 * conversation quality across multiple dimensions:
 * - Error detection accuracy
 * - Phase appropriateness
 * - Correction quality
 * - Encouragement balance
 * - Topic management
 * - Vocabulary teaching
 */
class JudgeService(private val config: TestHarnessConfig) {
    private val logger = LoggerFactory.getLogger(JudgeService::class.java)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    private val aiProvider = AiProviderFactory.create(config.getAiProviderConfig())

    /**
     * Evaluate a completed test scenario.
     */
    fun evaluate(
        scenario: TestScenario,
        transcript: List<ConversationTurn>,
        metrics: TechnicalMetrics
    ): JudgeEvaluation {
        logger.info("🔍 Evaluating scenario: ${scenario.name}")

        val prompt = buildEvaluationPrompt(scenario, transcript, metrics)
        logger.debug("Judge prompt length: ${prompt.length} characters")

        val response = aiProvider.chat(prompt, config.judgeModel, config.judgeTemperature)
        logger.debug("Judge response: $response")

        return parseJudgeResponse(response)
    }

    private fun buildEvaluationPrompt(
        scenario: TestScenario,
        transcript: List<ConversationTurn>,
        metrics: TechnicalMetrics
    ): String {
        val transcriptText = transcript.joinToString("\n\n") { turn ->
            buildString {
                appendLine("**Turn ${turn.turnIndex}:**")
                appendLine("Learner: ${turn.learnerMessage}")
                if (turn.intentionalErrors.isNotEmpty()) {
                    appendLine("  [Intentional errors: ${turn.intentionalErrors.joinToString(", ") { it.span }}]")
                }
                appendLine("Tutor: ${turn.tutorResponse.content}")
                appendLine("  Phase: ${turn.tutorResponse.currentPhase}")
                if (turn.tutorResponse.corrections.isNotEmpty()) {
                    appendLine("  Corrections: ${turn.tutorResponse.corrections.size}")
                    turn.tutorResponse.corrections.forEach { correction ->
                        appendLine("    - '${correction.span}' → '${correction.correctedForm}' (${correction.errorType}, ${correction.severity})")
                    }
                }
                if (turn.tutorResponse.newVocabulary.isNotEmpty()) {
                    appendLine("  New vocabulary: ${turn.tutorResponse.newVocabulary.joinToString(", ")}")
                }
            }
        }

        val expectedOutcomesText = buildString {
            appendLine("Expected Outcomes:")
            if (scenario.expectedOutcomes.shouldTriggerDrillPhase == true) {
                appendLine("- Should transition to Drill phase due to error patterns")
            }
            if (scenario.expectedOutcomes.shouldMaintainFreePhase == true) {
                appendLine("- Should maintain Free phase (fluency focus)")
            }
            if (scenario.expectedOutcomes.minimumCorrectionsDetected > 0) {
                appendLine("- Should detect at least ${scenario.expectedOutcomes.minimumCorrectionsDetected} errors")
            }
            scenario.expectedOutcomes.phaseTransitions.forEach { transition ->
                appendLine("- Should transition to ${transition.toPhase} after turn ${transition.afterMessageIndex}: ${transition.reason}")
            }
        }

        val metricsText = buildString {
            appendLine("Technical Metrics:")
            appendLine("- Total messages: ${metrics.totalMessages}")
            appendLine("- Corrections detected: ${metrics.correctionsDetected} / ${metrics.totalCorrections}")
            appendLine("- Corrections missed: ${metrics.correctionsMissed}")
            appendLine("- False positives: ${metrics.falsePositives}")
            appendLine("- Phase transitions: ${metrics.phaseTransitions.size}")
            metrics.phaseTransitions.forEach { transition ->
                appendLine("  - Turn ${transition.atTurnIndex}: ${transition.fromPhase} → ${transition.toPhase} ${if (transition.wasExpected) "(expected)" else "(unexpected)"}")
            }
            appendLine("- Topic changes: ${metrics.topicChanges}")
            appendLine("- Vocabulary items: ${metrics.vocabularyItemsIntroduced}")
        }

        return """
            You are an expert language pedagogy evaluator. Evaluate the following conversation between a language tutor AI and a learner.

            **Scenario:** ${scenario.name}
            **Description:** ${scenario.description}
            **Learner:** ${scenario.learnerPersona.name} (${scenario.learnerPersona.cefrLevel})
            **Learning Goals:** ${scenario.learnerPersona.learningGoals.joinToString(", ")}
            **Common Errors:** ${scenario.learnerPersona.commonErrors.joinToString(", ")}

            $expectedOutcomesText

            **Conversation Transcript:**
            $transcriptText

            $metricsText

            **Evaluation Focus Areas:** ${scenario.evaluationFocus.joinToString(", ")}

            Evaluate the tutor's performance across the following dimensions (0-100 scale for each):

            1. **Error Detection (errorDetectionScore)**: Did the tutor accurately detect errors? Consider:
               - Detection of all intentional errors embedded in learner messages
               - Appropriate classification (error type, severity)
               - No false positives (flagging correct language as errors)
               - Context-aware severity (e.g., casual chat norms vs formal writing)

            2. **Phase Appropriateness (phaseAppropriatenessScore)**: Was the conversation phase appropriate? Consider:
               - Initial phase selection
               - Phase transitions based on error patterns
               - Alignment with expected outcomes
               - Fossilization detection (repeated high-severity errors → Drill)
               - Fluency focus maintenance when appropriate (Free phase)

            3. **Correction Quality (correctionQualityScore)**: When corrections were provided, were they high quality? Consider:
               - Clear, concise explanations appropriate for CEFR level
               - Dual-language support (source + target language)
               - Actionable guidance for self-correction
               - Timing (immediate in Drill, passive in Correction phase)

            4. **Encouragement Balance (encouragementBalanceScore)**: Did the tutor balance correction with motivation? Consider:
               - Positive reinforcement for correct usage
               - Non-judgmental error feedback
               - Growth mindset messaging
               - Confidence building (especially in Free phase)

            5. **Topic Management (topicManagementScore)**: Was topic management effective? Consider:
               - Natural topic flow and coherence
               - Appropriate variety (avoiding repetition)
               - Stability (not changing topics too frequently)
               - Relevance to learner goals and level

            6. **Vocabulary Teaching (vocabularyTeachingScore)**: Was vocabulary introduction effective? Consider:
               - Appropriate complexity for CEFR level
               - Context-rich introductions
               - Reinforcement and recycling
               - Visual aids (word cards) when helpful

            Provide your evaluation as a JSON object with the following structure:
            {
              "errorDetectionScore": 0-100,
              "errorDetectionFeedback": "detailed feedback...",
              "phaseAppropriatenessScore": 0-100,
              "phaseAppropriatenessFeedback": "detailed feedback...",
              "correctionQualityScore": 0-100,
              "correctionQualityFeedback": "detailed feedback...",
              "encouragementBalanceScore": 0-100,
              "encouragementBalanceFeedback": "detailed feedback...",
              "topicManagementScore": 0-100,
              "topicManagementFeedback": "detailed feedback...",
              "vocabularyTeachingScore": 0-100,
              "vocabularyTeachingFeedback": "detailed feedback...",
              "overallPedagogicalScore": 0-100,
              "overallFeedback": "summary of performance...",
              "strengths": ["strength 1", "strength 2", "strength 3"],
              "improvements": ["improvement 1", "improvement 2", "improvement 3"]
            }

            Be thorough, specific, and cite examples from the transcript. Focus on pedagogical effectiveness, not just technical correctness.
        """.trimIndent()
    }

    private fun parseJudgeResponse(response: String): JudgeEvaluation {
        // Extract JSON from markdown code blocks if present
        val jsonContent = if (response.contains("```json")) {
            response.substringAfter("```json").substringBefore("```").trim()
        } else if (response.contains("```")) {
            response.substringAfter("```").substringBefore("```").trim()
        } else {
            response.trim()
        }

        return try {
            val judgeResponse = objectMapper.readValue(jsonContent, JudgeResponseDto::class.java)
            judgeResponse.toJudgeEvaluation()
        } catch (e: Exception) {
            logger.error("Failed to parse judge response: ${e.message}", e)
            logger.error("Raw response: $response")
            logger.error("Extracted JSON: $jsonContent")

            // Return a default evaluation with error information
            JudgeEvaluation(
                errorDetectionScore = 0.0,
                errorDetectionFeedback = "Failed to parse judge response: ${e.message}",
                phaseAppropriatenessScore = 0.0,
                phaseAppropriatenessFeedback = "Parse error",
                correctionQualityScore = 0.0,
                correctionQualityFeedback = "Parse error",
                encouragementBalanceScore = 0.0,
                encouragementBalanceFeedback = "Parse error",
                topicManagementScore = 0.0,
                topicManagementFeedback = "Parse error",
                vocabularyTeachingScore = 0.0,
                vocabularyTeachingFeedback = "Parse error",
                overallPedagogicalScore = 0.0,
                overallFeedback = "Evaluation failed due to parse error",
                strengths = emptyList(),
                improvements = listOf("Fix judge response parsing")
            )
        }
    }

    // DTO for parsing judge response
    private data class JudgeResponseDto(
        val errorDetectionScore: Double,
        val errorDetectionFeedback: String,
        val phaseAppropriatenessScore: Double,
        val phaseAppropriatenessFeedback: String,
        val correctionQualityScore: Double,
        val correctionQualityFeedback: String,
        val encouragementBalanceScore: Double,
        val encouragementBalanceFeedback: String,
        val topicManagementScore: Double,
        val topicManagementFeedback: String,
        val vocabularyTeachingScore: Double,
        val vocabularyTeachingFeedback: String,
        val overallPedagogicalScore: Double,
        val overallFeedback: String,
        val strengths: List<String>,
        val improvements: List<String>
    ) {
        fun toJudgeEvaluation() = JudgeEvaluation(
            errorDetectionScore = errorDetectionScore,
            errorDetectionFeedback = errorDetectionFeedback,
            phaseAppropriatenessScore = phaseAppropriatenessScore,
            phaseAppropriatenessFeedback = phaseAppropriatenessFeedback,
            correctionQualityScore = correctionQualityScore,
            correctionQualityFeedback = correctionQualityFeedback,
            encouragementBalanceScore = encouragementBalanceScore,
            encouragementBalanceFeedback = encouragementBalanceFeedback,
            topicManagementScore = topicManagementScore,
            topicManagementFeedback = topicManagementFeedback,
            vocabularyTeachingScore = vocabularyTeachingScore,
            vocabularyTeachingFeedback = vocabularyTeachingFeedback,
            overallPedagogicalScore = overallPedagogicalScore,
            overallFeedback = overallFeedback,
            strengths = strengths,
            improvements = improvements
        )
    }
}
