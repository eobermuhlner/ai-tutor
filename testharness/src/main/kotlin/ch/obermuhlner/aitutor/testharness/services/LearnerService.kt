package ch.obermuhlner.aitutor.testharness.services

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service that simulates a language learner using AI to interact with the tutor.
 */
@Service
class LearnerService(
    private val chatModel: ChatModel  // This will get the primary ChatModel bean
) {

    /**
     * Generate a learner response based on the tutor's message and conversation history
     */
    fun generateResponse(tutorMessage: String, conversationHistory: List<String>): String {
        // Combine system instructions with conversation history
        val messages = mutableListOf<UserMessage>()

        // Add a system message with learner persona instructions
        val systemPrompt = """
            You are simulating a language learner in a conversation with an AI tutor.
            Your goal is to respond naturally as a learner would at an intermediate level.
            Take into account the conversation history and respond appropriately to the tutor's message.
            Try to maintain the flow of conversation while making reasonable mistakes that a learner might make.
        """.trimIndent()

        val userMessage = """
            Tutor said: "$tutorMessage"

            Conversation history:
            ${conversationHistory.joinToString("\n") { "- $it" }}

            Your response as the learner:
        """.trimIndent()

        val response = chatModel.call(Prompt(UserMessage(userMessage)))
        return response.result.output.text?.trim() ?: ""
    }

    /**
     * Start a new conversation by generating an initial message
     */
    fun generateInitialMessage(topic: String? = null): String {
        val prompt = if (topic != null) {
            "Generate a natural opening message for a language learning conversation about '$topic' as a learner would say."
        } else {
            "Generate a natural opening message for a language learning conversation as a learner would say."
        }

        val response = chatModel.call(Prompt(UserMessage(prompt)))
        return response.result.output.text?.trim() ?: ""
    }

    /**
     * Generate a response that includes intentional mistakes for the tutor to correct
     */
    fun generateMistakeResponse(tutorMessage: String, conversationHistory: List<String>): String {
        val userMessage = """
            Tutor said: "$tutorMessage"

            Conversation history:
            ${conversationHistory.joinToString("\n") { "- $it" }}

            Your response as the learner (intentionally include a grammatical or vocabulary mistake):
        """.trimIndent()

        val response = chatModel.call(Prompt(UserMessage(userMessage)))
        return response.result.output.text?.trim() ?: ""
    }
}