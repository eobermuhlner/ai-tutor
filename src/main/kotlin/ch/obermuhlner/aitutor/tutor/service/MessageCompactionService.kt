package ch.obermuhlner.aitutor.tutor.service

import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MessageCompactionService(
    @Value("\${ai-tutor.context.max-tokens}") private val maxTokens: Int,
    @Value("\${ai-tutor.context.recent-messages}") private val recentMessageCount: Int
) {

    /**
     * Compacts message history to fit within token limits using a sliding window strategy.
     *
     * Strategy:
     * - Keeps system messages (they're always needed)
     * - Keeps the most recent N user/assistant messages at full fidelity
     * - Drops older messages beyond the sliding window
     *
     * @param systemMessages System prompts that must always be included
     * @param conversationMessages Historical user/assistant messages
     * @return Compacted list of messages that fits within token limits
     */
    fun compactMessages(
        systemMessages: List<Message>,
        conversationMessages: List<Message>
    ): List<Message> {
        // Estimate tokens for system messages (rough: 4 chars ≈ 1 token)
        val systemTokens = estimateTokens(systemMessages)

        // Calculate remaining token budget for conversation
        val conversationBudget = maxTokens - systemTokens

        if (conversationBudget <= 0) {
            // System messages alone exceed budget - return just system messages
            return systemMessages
        }

        // Take most recent messages up to the configured count
        val recentMessages = conversationMessages.takeLast(recentMessageCount)
        val recentTokens = estimateTokens(recentMessages)

        return if (recentTokens <= conversationBudget) {
            // Recent messages fit within budget
            systemMessages + recentMessages
        } else {
            // Even recent messages exceed budget - trim further
            val trimmedMessages = trimToTokenBudget(recentMessages, conversationBudget)
            systemMessages + trimmedMessages
        }
    }

    /**
     * Estimates token count for a list of messages.
     * Uses rough heuristic: 4 characters ≈ 1 token
     */
    private fun estimateTokens(messages: List<Message>): Int {
        val totalChars = messages.sumOf { getMessageText(it).length }
        return totalChars / 4
    }

    /**
     * Trims messages to fit within a token budget by taking the most recent ones.
     */
    private fun trimToTokenBudget(messages: List<Message>, budget: Int): List<Message> {
        val result = mutableListOf<Message>()
        var currentTokens = 0

        // Work backwards from most recent
        for (message in messages.reversed()) {
            val messageTokens = getMessageText(message).length / 4
            if (currentTokens + messageTokens <= budget) {
                result.add(0, message)
                currentTokens += messageTokens
            } else {
                break
            }
        }

        return result
    }

    /**
     * Helper function to get message text content.
     */
    private fun getMessageText(message: Message): String {
        return when (message) {
            is SystemMessage -> message.text ?: ""
            is UserMessage -> message.text ?: ""
            is AssistantMessage -> message.text ?: ""
            else -> ""
        }
    }
}
