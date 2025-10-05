package ch.obermuhlner.aitutor.tutor.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ConversationSummarizationService(
    private val chatModel: ChatModel,
    @Value("\${ai-tutor.context.summarization.batch-size-tokens}") private val batchSizeTokens: Int,
    @Value("\${ai-tutor.context.summarization.compression-ratio}") private val compressionRatio: Double,
    @Value("\${ai-tutor.context.summarization.tokens-per-word}") private val tokensPerWord: Double,
    @Value("\${ai-tutor.context.summarization.min-summary-tokens}") private val minSummaryTokens: Int,
    @Value("\${ai-tutor.context.summarization.max-summary-tokens}") private val maxSummaryTokens: Int,
    @Value("\${ai-tutor.context.summarization.prompt}") private val summarizationPrompt: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Summarizes a list of messages using LLM, with automatic batching for large histories.
     *
     * If messages exceed batch size, splits into chunks, summarizes each, then recursively
     * summarizes the summaries to produce a final concise summary.
     */
    fun summarizeMessages(messages: List<Message>): String {
        if (messages.isEmpty()) {
            logger.debug("No messages to summarize")
            return ""
        }

        val totalTokens = estimateTokens(messages)
        logger.debug("Summarizing ${messages.size} messages (~$totalTokens tokens)")

        return if (totalTokens <= batchSizeTokens) {
            // Single batch - summarize directly
            logger.debug("Single batch summarization")
            summarizeBatch(messages)
        } else {
            // Multiple batches - split, summarize each, then summarize summaries
            val batches = splitIntoBatches(messages, batchSizeTokens)
            logger.info("Multi-batch summarization: ${batches.size} batches")
            val batchSummaries = batches.map { batch -> summarizeBatch(batch) }

            // Recursively summarize the summaries
            if (batchSummaries.size == 1) {
                batchSummaries.first()
            } else {
                logger.debug("Recursively summarizing ${batchSummaries.size} batch summaries")
                val summaryMessages = batchSummaries.mapIndexed { index, summary ->
                    SystemMessage("Batch ${index + 1} summary: $summary")
                }
                summarizeMessages(summaryMessages)
            }
        }
    }

    /**
     * Summarizes a single batch of messages using the configured LLM.
     */
    private fun summarizeBatch(messages: List<Message>): String {
        logger.debug("Summarizing batch of ${messages.size} messages")

        // Build conversation text from messages
        val conversationText = messages.joinToString("\n") { message ->
            val role = when (message) {
                is UserMessage -> "Learner"
                is AssistantMessage -> "Tutor"
                is SystemMessage -> "System"
                else -> "Unknown"
            }
            val text = getMessageText(message)
            "$role: $text"
        }

        // Calculate dynamic token budget based on input size
        val inputTokens = estimateTokens(messages)
        val targetSummaryTokens = (inputTokens * compressionRatio).toInt()
        val budgetTokens = targetSummaryTokens.coerceIn(minSummaryTokens, maxSummaryTokens)
        val targetWords = (budgetTokens / tokensPerWord).toInt() // Convert tokens to words

        logger.debug("Input: $inputTokens tokens, target summary: $budgetTokens tokens ($targetWords words)")

        // Build prompt with dynamic target using PromptTemplate
        val promptText = PromptTemplate(summarizationPrompt).render(mapOf(
            "targetWords" to targetWords.toString()
        ))
        val promptMessages = listOf(
            SystemMessage(promptText),
            UserMessage("Conversation to summarize:\n\n$conversationText")
        )

        // Call LLM to generate summary
        logger.debug("Calling LLM for summary generation")
        val response = chatModel.call(Prompt(promptMessages))
        val summary = response.result.output.text ?: ""
        logger.debug("Summary generated: ${summary.length} chars (~${estimateTokens(listOf(SystemMessage(summary)))} tokens)")
        logger.trace("Summary: {}", summary)
        return summary
    }

    /**
     * Splits messages into batches that fit within the token limit.
     */
    private fun splitIntoBatches(messages: List<Message>, batchSizeTokens: Int): List<List<Message>> {
        val batches = mutableListOf<List<Message>>()
        val currentBatch = mutableListOf<Message>()
        var currentTokens = 0

        for (message in messages) {
            val messageTokens = estimateTokens(listOf(message))

            if (currentTokens + messageTokens > batchSizeTokens && currentBatch.isNotEmpty()) {
                // Current batch is full, start new batch
                batches.add(currentBatch.toList())
                currentBatch.clear()
                currentTokens = 0
            }

            currentBatch.add(message)
            currentTokens += messageTokens
        }

        // Add remaining messages
        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch.toList())
        }

        return batches
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
     * Helper function to get message text content.
     */
    private fun getMessageText(message: Message): String {
        return when (message) {
            is SystemMessage -> message.text
            is UserMessage -> message.text
            is AssistantMessage -> message.text ?: ""
            else -> ""
        }
    }
}
