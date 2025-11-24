package ch.obermuhlner.aitutor.testharness.services

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import ch.obermuhlner.aitutor.testharness.domain.EvaluationResult
import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Service that evaluates the quality of the AI tutor's responses using AI.
 */
@Service
class JudgeService(
    private val chatModel: ChatModel,  // This will get the primary ChatModel bean
    private val objectMapper: ObjectMapper
) {

    /**
     * Evaluate the quality of a tutor response based on pedagogical criteria
     */
    fun evaluateTutorResponse(
        scenario: TestScenario,
        tutorResponse: String,
        learnerInput: String,
        conversationHistory: List<String>
    ): EvaluationResult {
        val evaluationPrompt = """
            You are an expert language pedagogy evaluator. Evaluate the quality of the AI tutor's response in the context of language learning.

            Test Scenario:
            - Language: ${scenario.language}
            - Level: ${scenario.level}
            - Topic: ${scenario.topic}
            - Objective: ${scenario.objective}

            Learner Input: $learnerInput
            Tutor Response: $tutorResponse

            Conversation History:
            ${conversationHistory.joinToString("\n") { "- $it" }}

            Evaluate the tutor response based on these criteria:
            1. Pedagogical appropriateness: Does the response match the learner's level and needs?
            2. Accuracy: Is the information provided correct?
            3. Engagement: Is the response likely to keep the learner engaged?
            4. Error correction: If errors were made, were they addressed appropriately?
            5. Language learning value: Does the response help the learner improve?

            Provide your evaluation as a rating from 1-10 (with 10 being excellent) and specific feedback.

            Format your response as JSON:
            {
              "rating": [number from 1-10],
              "feedback": "[specific feedback about the tutor response]",
              "strengths": ["[list of strengths]"],
              "improvements": ["[list of suggested improvements]"],
              "pedagogicalScore": [number from 1-10],
              "accuracyScore": [number from 1-10],
              "engagementScore": [number from 1-10]
            }
        """.trimIndent()

        val response = chatModel.call(Prompt(UserMessage(evaluationPrompt)))
        val result = response.result.output.text?.trim() ?: ""

        // Extract JSON from the response
        val jsonStart = result.indexOf('{')
        val jsonEnd = result.lastIndexOf('}') + 1

        if (jsonStart != -1 && jsonEnd > jsonStart) {
            val json = result.substring(jsonStart, jsonEnd)
            return parseEvaluationResult(json)
        } else {
            // If parsing fails, return a default evaluation
            return EvaluationResult(
                rating = 5,
                feedback = "Could not parse AI evaluation. Raw response: $result",
                strengths = listOf(),
                improvements = listOf(),
                pedagogicalScore = 5,
                accuracyScore = 5,
                engagementScore = 5
            )
        }
    }

    /**
     * Evaluate whether a tutor response appropriately handles a learner mistake
     */
    fun evaluateErrorCorrection(
        learnerMistake: String,
        tutorCorrection: String,
        expectedCorrectionApproach: String = "constructive and educational"
    ): EvaluationResult {
        val evaluationPrompt = """
            You are an expert language pedagogy evaluator. Evaluate how well the AI tutor corrected the learner's mistake.

            Learner Mistake: $learnerMistake
            Tutor Correction: $tutorCorrection
            Expected Approach: $expectedCorrectionApproach

            Evaluate the correction based on these pedagogical principles:
            1. Clarity: Is the correction clear and understandable?
            2. Constructiveness: Is it delivered in a supportive way?
            3. Educational value: Does it help the learner understand the error?
            4. Appropriateness: Is the correction method appropriate for the situation?

            Rate from 1-10 and provide specific feedback.

            Format your response as JSON:
            {
              "rating": [number from 1-10],
              "feedback": "[specific feedback]",
              "strengths": ["[list of strengths]"],
              "improvements": ["[list of suggested improvements]"],
              "pedagogicalScore": [number from 1-10],
              "constructivenessScore": [number from 1-10],
              "educationalValueScore": [number from 1-10]
            }
        """.trimIndent()

        val response = chatModel.call(Prompt(UserMessage(evaluationPrompt)))
        val result = response.result.output.text?.trim() ?: ""

        val jsonStart = result.indexOf('{')
        val jsonEnd = result.lastIndexOf('}') + 1

        if (jsonStart != -1 && jsonEnd > jsonStart) {
            val json = result.substring(jsonStart, jsonEnd)
            return parseEvaluationResult(json)
        } else {
            return EvaluationResult(
                rating = 5,
                feedback = "Could not parse AI evaluation. Raw response: $result",
                strengths = listOf(),
                improvements = listOf(),
                pedagogicalScore = 5,
                accuracyScore = 5,
                engagementScore = 5
            )
        }
    }

    private fun parseEvaluationResult(jsonString: String): EvaluationResult {
        try {
            val evaluationData = objectMapper.readValue<Map<String, Any>>(jsonString)
            
            val rating = (evaluationData["rating"] as? Number)?.toInt() ?: 5
            val feedback = evaluationData["feedback"] as? String ?: "No feedback provided"
            val pedagogicalScore = (evaluationData["pedagogicalScore"] as? Number)?.toInt() ?: 5
            val accuracyScore = (evaluationData["accuracyScore"] as? Number)?.toInt() ?: 5
            val engagementScore = (evaluationData["engagementScore"] as? Number)?.toInt() ?: 5
            
            @Suppress("UNCHECKED_CAST")
            val strengths = (evaluationData["strengths"] as? List<String>) ?: listOf()
            @Suppress("UNCHECKED_CAST")
            val improvements = (evaluationData["improvements"] as? List<String>) ?: listOf()

            return EvaluationResult(
                rating = rating,
                feedback = feedback,
                strengths = strengths,
                improvements = improvements,
                pedagogicalScore = pedagogicalScore,
                accuracyScore = accuracyScore,
                engagementScore = engagementScore
            )
        } catch (e: Exception) {
            // If JSON parsing fails, return a default evaluation
            return EvaluationResult(
                rating = 5,
                feedback = "Could not parse AI evaluation result: ${e.message}",
                strengths = listOf(),
                improvements = listOf(),
                pedagogicalScore = 5,
                accuracyScore = 5,
                engagementScore = 5
            )
        }
    }
}