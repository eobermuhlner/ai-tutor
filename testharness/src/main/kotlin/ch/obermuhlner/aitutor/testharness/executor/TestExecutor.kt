package ch.obermuhlner.aitutor.testharness.executor

import ch.obermuhlner.aitutor.testharness.client.ApiClient
import ch.obermuhlner.aitutor.testharness.domain.EvaluationResult
import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import ch.obermuhlner.aitutor.testharness.services.JudgeService
import ch.obermuhlner.aitutor.testharness.services.LearnerService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Executes test scenarios by coordinating the learner, the backend API, and the judge
 * This is a simplified implementation that can be enhanced to use the actual conversation script from scenario files.
 */
@Service
class TestExecutor(
    private val apiClient: ApiClient,
    private val learnerService: LearnerService,
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
            // Create a new session for the test
            sessionId = apiClient.createSession(
                userId = "test_user_${UUID.randomUUID()}",
                language = scenario.language,
                level = scenario.level
            )

            // For testing, let's try to access the actual conversation script
            // For now, we'll simulate a basic conversation using the scenario data
            var tutorResponse: String

            // Send the first message based on the scenario's conversation script
            // In real scenario files, the first content would be in the conversation script
            // For now, we'll use a simulated approach
            val initialLearnerMessage = "Hello, how are you today?"
            conversationHistory.add("Learner: $initialLearnerMessage")

            // Send the initial message to the tutor
            val initialResponse = apiClient.sendMessage(sessionId, initialLearnerMessage)
            tutorResponse = extractTutorMessage(initialResponse)
            conversationHistory.add("Tutor: $tutorResponse")

            // Evaluate the initial interaction
            val initialEvaluation = judgeService.evaluateTutorResponse(
                scenario = scenario,
                tutorResponse = tutorResponse,
                learnerInput = initialLearnerMessage,
                conversationHistory = conversationHistory.map { it.substringAfter(": ").trim() }
            )
            evaluationResults.add(initialEvaluation)

            // Execute the test steps defined in the scenario (using the conversation script content)
            // We'll try to get actual conversation content from the scenario's testSteps
            // The testSteps were populated from the conversationScript in the scenario loader
            scenario.testSteps.forEachIndexed { index, step ->
                val learnerMessage = if (index == 0 && step.expectedOutcome.isNotEmpty()) {
                    // Use the actual message content from the scenario file if available
                    step.expectedOutcome
                } else {
                    // Generate a response based on the tutor's message and conversation history
                    learnerService.generateResponse(tutorResponse, conversationHistory.map { it.substringAfter(": ").trim() })
                }

                conversationHistory.add("Learner: $learnerMessage")

                val tutorResponseNew = apiClient.sendMessage(sessionId, learnerMessage)
                val newTutorResponse = extractTutorMessage(tutorResponseNew)
                conversationHistory.add("Tutor: $newTutorResponse")

                val evaluation = judgeService.evaluateTutorResponse(
                    scenario = scenario,
                    tutorResponse = newTutorResponse,
                    learnerInput = learnerMessage,
                    conversationHistory = conversationHistory.map { it.substringAfter(": ").trim() }
                )
                evaluationResults.add(evaluation)

                // Update for next iteration
                tutorResponse = newTutorResponse
            }

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

            return ScenarioResult(
                scenarioId = scenario.id,
                success = true,
                message = "Scenario executed successfully",
                conversationHistory = conversationHistory.toList(),
                evaluationResults = evaluationResults,
                overallRating = overallRating,
                pedagogicalScore = pedagogicalScore,
                accuracyScore = accuracyScore,
                engagementScore = engagementScore
            )

        } catch (e: Exception) {
            return ScenarioResult(
                scenarioId = scenario.id,
                success = false,
                message = "Error executing scenario: ${e.message}",
                conversationHistory = conversationHistory.toList(),
                evaluationResults = evaluationResults,
                overallRating = 0.0,
                pedagogicalScore = 0.0,
                accuracyScore = 0.0,
                engagementScore = 0.0
            )
        } finally {
            // Clean up session if created
            sessionId?.let {
                // In a real implementation, you might want to delete the session
                // apiClient.deleteSession(it)
            }
        }
    }

    private fun extractTutorMessage(response: Map<String, Any>): String {
        // Extract the tutor's response from the API response
        // This depends on the actual structure of the backend response
        return response["content"] as? String
            ?: (response["message"] as? String)
            ?: (response["response"] as? String)
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
    val engagementScore: Double
)