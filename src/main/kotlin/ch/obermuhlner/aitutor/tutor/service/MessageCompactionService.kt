package ch.obermuhlner.aitutor.tutor.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MessageCompactionService(
    @Value("\${ai-tutor.context.max-tokens}") private val maxTokens: Int,
    @Value("\${ai-tutor.context.recent-messages}") private val recentMessageCount: Int,
    @Value("\${ai-tutor.context.summarization.enabled}") private val summarizationEnabled: Boolean,
    @Value("\${ai-tutor.prompts.summary-prefix}") private val summaryPrefixPrompt: String,
    private val summarizationService: ConversationSummarizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Compacts message history to fit within token limits.
     *
     * Strategy:
     * - If summarization enabled: System + LLM Summary of old messages + Recent N messages
     * - Otherwise: System + Recent N messages (sliding window)
     *
     * @param systemMessages System prompts that must always be included
     * @param conversationMessages Historical user/assistant messages
     * @return Compacted list of messages that fits within token limits
     */
    fun compactMessages(
        systemMessages: List<Message>,
        conversationMessages: List<Message>
    ): List<Message> {
        logger.debug("Compacting messages: ${conversationMessages.size} conversation messages, max tokens: $maxTokens")

        // Estimate tokens for system messages (rough: 4 chars ≈ 1 token)
        val systemTokens = estimateTokens(systemMessages)

        // Calculate remaining token budget for conversation
        val conversationBudget = maxTokens - systemTokens

        if (conversationBudget <= 0) {
            logger.warn("System messages alone exceed budget (${systemTokens} tokens), returning minimal messages")
            return systemMessages + conversationMessages.takeLast(1)
        }

        // Separate recent and old messages
        val recentMessages = conversationMessages.takeLast(recentMessageCount)
        val oldMessages = conversationMessages.dropLast(recentMessageCount)

        logger.debug("Split: ${oldMessages.size} old messages, ${recentMessages.size} recent messages")

        // LLM-based summarization path
        if (summarizationEnabled) {
            if (oldMessages.isEmpty()) {
                logger.debug("Summarization enabled but no old messages to summarize, using sliding window")
            } else {
                logger.debug("Summarization enabled, summarizing ${oldMessages.size} old messages")
                val summary = summarizationService.summarizeMessages(oldMessages)
                val summaryText = PromptTemplate(summaryPrefixPrompt).render(mapOf(
                    "summary" to summary
                ))
                val summaryMessage = SystemMessage(summaryText)

                val recentTokens = estimateTokens(recentMessages)
                val summaryTokens = estimateTokens(listOf(summaryMessage))

                logger.debug("Summary: ${summaryTokens} tokens, recent: ${recentTokens} tokens, budget: ${conversationBudget}")

                return if (summaryTokens + recentTokens <= conversationBudget) {
                    logger.info("Compaction complete: summary + ${recentMessages.size} recent messages")
                    systemMessages + summaryMessage + recentMessages
                } else {
                    val availableForRecent = (conversationBudget - summaryTokens).coerceAtLeast(0)
                    val trimmedRecent = trimToTokenBudget(recentMessages, availableForRecent)
                    logger.info("Compaction complete: summary + ${trimmedRecent.size} trimmed recent messages (${recentMessages.size - trimmedRecent.size} dropped)")
                    systemMessages + summaryMessage + trimmedRecent
                }
            }
        }

        // Fallback: sliding window (original behavior)
        logger.debug("Using sliding window (summarization disabled)")
        val recentTokens = estimateTokens(recentMessages)
        return if (recentTokens <= conversationBudget) {
            logger.info("Compaction complete: ${recentMessages.size} recent messages fit within budget")
            systemMessages + recentMessages
        } else {
            val trimmedMessages = trimToTokenBudget(recentMessages, conversationBudget)
            logger.info("Compaction complete: ${trimmedMessages.size} messages after trimming (${recentMessages.size - trimmedMessages.size} dropped)")
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
