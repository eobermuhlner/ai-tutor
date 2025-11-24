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
     * Evaluate the overall conversation quality focusing on pedagogical effectiveness
     */
    fun evaluateConversationQuality(
        scenario: TestScenario,
        tutorResponse: String,
        learnerInput: String,
        conversationHistory: List<String>,
        messageIndex: Int = 0,
        sessionId: String
    ): EvaluationResult {
        // Build comprehensive focus areas based on scenario
        val tutorConfig = scenario.tutorConfig
        val focusAreas = buildString {
            append("Evaluate conversation quality focusing on:\n")

            // Teaching Style Adherence
            if (tutorConfig != null) {
                append("- Teaching Style: Does the tutor follow '${tutorConfig.teachingStyle}' style?\n")
                append("  * Guided: Structured, asks clarifying questions, provides clear explanations\n")
                append("  * Reactive: Minimal intervention, lets learner lead, corrects only when necessary\n")
                append("  * Directive: Active instruction, provides examples, more teaching-focused\n")
            }

            // CEFR Level Appropriateness
            append("- CEFR Level Appropriateness: Is the conversation suitable for ${scenario.level} level?\n")
            append("  * Vocabulary complexity\n")
            append("  * Sentence structure complexity\n")
            append("  * Topic depth\n")

            // Conversation Phase Appropriateness
            if (tutorConfig != null) {
                append("- Phase Appropriateness: Does the tutor use the '${tutorConfig.initialPhase}' phase correctly?\n")
                append("  * Free: Pure fluency focus, encouraging natural conversation\n")
                append("  * Correction: Balanced feedback without disrupting flow\n")
                append("  * Drill: Focused practice on specific patterns\n")
                append("  * Auto: Adaptive approach based on learner needs\n")
            }

            // Scenario-specific focus areas
            if (scenario.evaluationFocus.isNotEmpty()) {
                append("- Scenario-Specific Focus:\n")
                scenario.evaluationFocus.forEach { focus ->
                    append("  * $focus\n")
                }
            }

            // General quality factors
            append("- Content Quality:\n")
            append("  * Accuracy of language instruction\n")
            append("  * Cultural appropriateness\n")
            append("  * Relevance to learner's goals\n")
            append("- Pedagogical Effectiveness:\n")
            append("  * Clear explanations\n")
            append("  * Appropriate pacing\n")
            append("  * Learner engagement\n")
        }

        val evaluationPrompt = """
            You are an expert language pedagogy evaluator. Evaluate the quality and effectiveness of the AI tutor's conversation.

            Test Scenario:
            - Name: ${scenario.name}
            - Description: ${scenario.description}
            - Target Language: ${scenario.language} (${scenario.level} level)
            - Topic: ${scenario.topic}
            - Learner Goals: ${scenario.learnerPersona.learningGoals.joinToString(", ")}
            - Message ${messageIndex + 1} of ${scenario.conversationScript.size}

            Tutor Configuration:
            ${if (tutorConfig != null) """
            - Tutor Name: ${tutorConfig.tutorName}
            - Teaching Style: ${tutorConfig.teachingStyle}
            - Initial Phase: ${tutorConfig.initialPhase}
            """.trimIndent() else "- Default configuration"}

            Current Exchange:
            Learner: $learnerInput
            Tutor: $tutorResponse

            Full Conversation Context:
            ${conversationHistory.takeLast(10).joinToString("\n") { "- $it" }}

            $focusAreas

            Provide your evaluation as a rating from 1-10 (with 10 being excellent) and specific feedback.
            Consider the entire conversation context, not just this single exchange.

            Format your response as JSON:
            {
              "rating": [number from 1-10 - overall conversation quality],
              "feedback": "[specific feedback about quality, teaching effectiveness, and adherence to requirements]",
              "strengths": ["[what the tutor is doing well]"],
              "improvements": ["[what could be improved]"],
              "pedagogicalScore": [1-10 - teaching effectiveness and style adherence],
              "accuracyScore": [1-10 - content accuracy and cultural appropriateness],
              "engagementScore": [1-10 - learner engagement and conversation flow]
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