package ch.obermuhlner.aitutor.testharness.executor

import ch.obermuhlner.aitutor.testharness.client.ApiClient
import ch.obermuhlner.aitutor.testharness.domain.*
import ch.obermuhlner.aitutor.testharness.services.JudgeService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Executes test scenarios by following the conversation script and evaluating conversation quality
 */
@Service
class TestExecutor(
    private val apiClient: ApiClient,
    private val judgeService: JudgeService
) {

    /**
     * Execute a single test scenario
     */
    fun executeScenario(scenario: TestScenario): ScenarioResult {
        val conversationHistory = mutableListOf<String>()
        val evaluationResults = mutableListOf<EvaluationResult>()
        var sessionId: String? = null

        try {
            // Create a new session for the test using tutorConfig if available
            sessionId = createSessionFromScenario(scenario)

            println("Created session: $sessionId for scenario: ${scenario.name}")
            println("Focus: ${scenario.evaluationFocus.joinToString(", ")}")

            // Execute each message in the conversation script
            scenario.conversationScript.forEachIndexed { index, message ->
                println("\n=== Message ${index + 1}/${scenario.conversationScript.size} ===")
                println("Learner: ${message.content}")
                if (message.notes != null) {
                    println("Note: ${message.notes}")
                }

                conversationHistory.add("Learner: ${message.content}")

                // Send learner message and get tutor response
                val tutorResponseMap = apiClient.sendMessage(sessionId, message.content)
                val tutorResponse = extractTutorMessage(tutorResponseMap)

                println("Tutor: $tutorResponse")

                conversationHistory.add("Tutor: $tutorResponse")

                // Evaluate the conversation quality
                val evaluation = judgeService.evaluateConversationQuality(
                    scenario = scenario,
                    tutorResponse = tutorResponse,
                    learnerInput = message.content,
                    conversationHistory = conversationHistory.map { it.substringAfter(": ").trim() },
                    messageIndex = index,
                    sessionId = sessionId
                )
                evaluationResults.add(evaluation)

                println("Quality Rating: ${evaluation.rating}/10")
                println("  Pedagogical: ${evaluation.pedagogicalScore}/10")
                println("  Content Accuracy: ${evaluation.accuracyScore}/10")
                println("  Engagement: ${evaluation.engagementScore}/10")
                if (evaluation.feedback.isNotEmpty()) {
                    println("  Feedback: ${evaluation.feedback}")
                }
            }

            // Get final session state for validation
            val validationResults = validateScenarioOutcomes(scenario, sessionId, evaluationResults)

            // Calculate overall results
            val overallRating = if (evaluationResults.isNotEmpty()) {
                evaluationResults.sumOf { it.rating }.toDouble() / evaluationResults.size
            } else 0.0

            val pedagogicalScore = if (evaluationResults.isNotEmpty()) {
                evaluationResults.sumOf { it.pedagogicalScore }.toDouble() / evaluationResults.size
            } else 0.0

            val accuracyScore = if (evaluationResults.isNotEmpty()) {
                evaluationResults.sumOf { it.accuracyScore }.toDouble() / evaluationResults.size
            } else 0.0

            val engagementScore = if (evaluationResults.isNotEmpty()) {
                evaluationResults.sumOf { it.engagementScore }.toDouble() / evaluationResults.size
            } else 0.0

            // Overall success based on quality thresholds
            val qualityThresholdMet = overallRating >= 6.0 && pedagogicalScore >= 6.0
            val validationsPassed = validationResults.all { it.passed }
            val success = qualityThresholdMet && validationsPassed

            val message = buildString {
                if (success) {
                    append("Scenario passed quality standards.\n")
                } else {
                    append("Scenario quality issues detected:\n")
                    if (!qualityThresholdMet) {
                        append("  - Quality ratings below threshold (6.0)\n")
                    }
                    if (!validationsPassed) {
                        append("  - Validation failures:\n")
                        validationResults.filter { !it.passed }.forEach {
                            append("    • ${it.message}\n")
                        }
                    }
                }
                append("Average ratings: Overall=${String.format("%.1f", overallRating)}, ")
                append("Pedagogical=${String.format("%.1f", pedagogicalScore)}, ")
                append("Accuracy=${String.format("%.1f", accuracyScore)}, ")
                append("Engagement=${String.format("%.1f", engagementScore)}")
            }

            return ScenarioResult(
                scenarioId = scenario.id,
                success = success,
                message = message,
                conversationHistory = conversationHistory.toList(),
                evaluationResults = evaluationResults,
                overallRating = overallRating,
                pedagogicalScore = pedagogicalScore,
                accuracyScore = accuracyScore,
                engagementScore = engagementScore,
                validationResults = validationResults
            )

        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            e.printStackTrace()

            return ScenarioResult(
                scenarioId = scenario.id,
                success = false,
                message = "Error executing scenario: ${e.message}",
                conversationHistory = conversationHistory.toList(),
                evaluationResults = evaluationResults,
                overallRating = 0.0,
                pedagogicalScore = 0.0,
                accuracyScore = 0.0,
                engagementScore = 0.0,
                validationResults = emptyList()
            )
        }
    }

    /**
     * Create a session configured according to the scenario
     */
    private fun createSessionFromScenario(scenario: TestScenario): String {
        val tutorConfig = scenario.tutorConfig

        return if (tutorConfig != null) {
            // Use tutor configuration from scenario
            apiClient.createSession(
                userId = "test_user_${UUID.randomUUID()}",
                language = scenario.language,
                level = scenario.level,
                tutorName = tutorConfig.tutorName,
                tutorPersona = tutorConfig.teachingStyle,
                initialPhase = tutorConfig.initialPhase
            )
        } else {
            // Use default configuration
            apiClient.createSession(
                userId = "test_user_${UUID.randomUUID()}",
                language = scenario.language,
                level = scenario.level
            )
        }
    }

    /**
     * Validate scenario-specific outcomes (phase transitions, teaching style adherence, etc.)
     */
    private fun validateScenarioOutcomes(
        scenario: TestScenario,
        sessionId: String,
        evaluations: List<EvaluationResult>
    ): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        try {
            val session = apiClient.getSession(sessionId)

            // Validate teaching style adherence
            scenario.tutorConfig?.let { config ->
                val teachingStyle = session["teachingStyle"] as? String
                results.add(
                    ValidationResult(
                        category = "Teaching Style",
                        passed = teachingStyle != null,
                        message = "Teaching style: ${teachingStyle ?: "not set"} (expected: ${config.teachingStyle})"
                    )
                )
            }

            // Validate phase appropriateness
            val currentPhase = session["currentPhase"] as? String
            scenario.expectedOutcomes?.let { outcomes ->
                if (outcomes.phaseTransitions.isNotEmpty()) {
                    val finalExpectedPhase = outcomes.phaseTransitions.maxByOrNull { it.afterMessageIndex }?.toPhase
                    if (finalExpectedPhase != null) {
                        results.add(
                            ValidationResult(
                                category = "Conversation Phase",
                                passed = currentPhase.equals(finalExpectedPhase, ignoreCase = true),
                                message = "Final phase: $currentPhase (expected: $finalExpectedPhase)"
                            )
                        )
                    }
                }
            }

            // Validate overall conversation quality
            val avgPedagogicalScore = evaluations.map { it.pedagogicalScore }.average()
            results.add(
                ValidationResult(
                    category = "Pedagogical Quality",
                    passed = avgPedagogicalScore >= 6.0,
                    message = "Average pedagogical score: ${String.format("%.1f", avgPedagogicalScore)}/10 (threshold: 6.0)"
                )
            )

            val avgAccuracyScore = evaluations.map { it.accuracyScore }.average()
            results.add(
                ValidationResult(
                    category = "Content Accuracy",
                    passed = avgAccuracyScore >= 6.0,
                    message = "Average accuracy score: ${String.format("%.1f", avgAccuracyScore)}/10 (threshold: 6.0)"
                )
            )

        } catch (e: Exception) {
            results.add(
                ValidationResult(
                    category = "Session Validation",
                    passed = false,
                    message = "Could not validate session: ${e.message}"
                )
            )
        }

        return results
    }

    private fun extractTutorMessage(response: Map<String, Any>): String {
        // Extract the tutor's response from the API response
        return response["content"] as? String
            ?: "No response from tutor"
    }
}

data class ScenarioResult(
    val scenarioId: String,
    val success: Boolean,
    val message: String,
    val conversationHistory: List<String>,
    val evaluationResults: List<EvaluationResult>,
    val overallRating: Double,
    val pedagogicalScore: Double,
    val accuracyScore: Double,
    val engagementScore: Double,
    val validationResults: List<ValidationResult>
)

data class ValidationResult(
    val category: String,
    val passed: Boolean,
    val message: String
)
