package ch.obermuhlner.aitutor.tutor.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.domain.MessageSummaryEntity
import ch.obermuhlner.aitutor.chat.domain.SummarySourceType
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProgressiveSummarizationService(
    private val summarizationService: ConversationSummarizationService,
    private val summaryRepository: MessageSummaryRepository,
    private val messageRepository: ChatMessageRepository,
    @Value("\${ai-tutor.context.summarization.progressive.chunk-size}") private val chunkSize: Int,
    @Value("\${ai-tutor.context.summarization.progressive.chunk-token-threshold}") private val chunkTokenThreshold: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    /**
     * Check if summarization is needed after new messages, and trigger if so.
     * Runs asynchronously to avoid blocking user requests.
     */
    @Async
    @Transactional
    fun checkAndSummarize(sessionId: UUID) {
        try {
            logger.debug("Async summarization check for session $sessionId")

            // 1. Find last summarized sequence
            val lastSummary = summaryRepository
                .findTopBySessionIdAndSummaryLevelAndIsActiveTrueOrderByEndSequenceDesc(sessionId, 1)
            val startSequence = (lastSummary?.endSequence ?: -1) + 1

            // 2. Get unsummarized messages
            val unsummarized = messageRepository
                .findBySessionIdAndSequenceNumberGreaterThanEqualOrderBySequenceNumberAsc(
                    sessionId, startSequence
                )

            if (unsummarized.isEmpty()) {
                logger.debug("No unsummarized messages for session $sessionId")
                return
            }

            // 3. Check trigger conditions
            val tokenCount = estimateTokens(unsummarized.map { it.content })
            logger.debug(
                "Session $sessionId: ${unsummarized.size} unsummarized messages, ~$tokenCount tokens"
            )

            if (unsummarized.size < chunkSize && tokenCount < chunkTokenThreshold) {
                logger.debug("Not yet ready to summarize (size: ${unsummarized.size}/$chunkSize, tokens: $tokenCount/$chunkTokenThreshold)")
                return
            }

            // 4. Summarize all complete chunks (not just first!)
            val chunks = unsummarized.chunked(chunkSize)
            for ((index, chunk) in chunks.withIndex()) {
                if (chunk.size == chunkSize || tokenCount >= chunkTokenThreshold) {
                    logger.info("Summarizing chunk ${index + 1}/${chunks.size} for session $sessionId (${chunk.size} messages)")
                    val level1Summary = summarizeMessageChunk(sessionId, chunk)
                    summaryRepository.save(level1Summary)
                } else {
                    logger.debug("Skipping incomplete chunk ${index + 1} (${chunk.size}/$chunkSize messages)")
                }
            }

            // 5. Recursively check if level-1 summaries need level-2 summarization
            propagateUpward(sessionId, level = 1)

        } catch (e: Exception) {
            logger.error("Error during async summarization for session $sessionId", e)
        }
    }

    /**
     * Recursively create higher-level summaries when lower level has enough entries.
     * Process ALL complete chunks, not just first.
     */
    @Transactional
    private fun propagateUpward(sessionId: UUID, level: Int) {
        val summaries = summaryRepository
            .findBySessionIdAndSummaryLevelAndIsActiveTrueOrderByStartSequenceAsc(sessionId, level)

        if (summaries.size < chunkSize) {
            logger.debug("Level $level: Only ${summaries.size} summaries, need $chunkSize to propagate")
            return
        }

        // Process ALL complete chunks
        val chunks = summaries.chunked(chunkSize)
        for ((index, chunk) in chunks.withIndex()) {
            if (chunk.size == chunkSize) {
                logger.info("Propagating to level ${level + 1}: chunk ${index + 1}, summarizing ${chunk.size} level-$level summaries")
                val higherLevelSummary = summarizeSummaryChunk(sessionId, level + 1, chunk)
                summaryRepository.save(higherLevelSummary)

                // Mark child summaries as superseded
                summaryRepository.markAsSuperseded(
                    chunk.map { it.id },
                    higherLevelSummary.id
                )
            }
        }

        // Recursively propagate to next level
        if (chunks.any { it.size == chunkSize }) {
            propagateUpward(sessionId, level + 1)
        }
    }

    /**
     * Create level-1 summary from raw messages.
     */
    private fun summarizeMessageChunk(
        sessionId: UUID,
        messages: List<ChatMessageEntity>
    ): MessageSummaryEntity {
        // Convert to Spring AI Message objects
        val messageObjects = messages.map { entity ->
            when (entity.role) {
                MessageRole.USER -> UserMessage(entity.content)
                MessageRole.ASSISTANT -> AssistantMessage(entity.content)
            }
        }

        // Call LLM to summarize
        val summaryText = summarizationService.summarizeMessages(messageObjects)
        val tokenCount = estimateTokens(listOf(summaryText))

        // Store source message IDs
        val sourceIds = messages.map { it.id.toString() }
        val sourceIdsJson = objectMapper.writeValueAsString(sourceIds)

        return MessageSummaryEntity(
            sessionId = sessionId,
            summaryLevel = 1,
            startSequence = messages.first().sequenceNumber,
            endSequence = messages.last().sequenceNumber,
            summaryText = summaryText,
            tokenCount = tokenCount,
            sourceType = SummarySourceType.MESSAGE,
            sourceIdsJson = sourceIdsJson
        )
    }

    /**
     * Create higher-level summary from lower-level summaries.
     */
    private fun summarizeSummaryChunk(
        sessionId: UUID,
        level: Int,
        summaries: List<MessageSummaryEntity>
    ): MessageSummaryEntity {
        // Convert summaries to messages for LLM
        val summaryMessages = summaries.mapIndexed { index, summary ->
            SystemMessage("Summary ${index + 1}: ${summary.summaryText}")
        }

        // Call LLM to summarize summaries
        val summaryText = summarizationService.summarizeMessages(summaryMessages)
        val tokenCount = estimateTokens(listOf(summaryText))

        // Store source summary IDs
        val sourceIds = summaries.map { it.id.toString() }
        val sourceIdsJson = objectMapper.writeValueAsString(sourceIds)

        return MessageSummaryEntity(
            sessionId = sessionId,
            summaryLevel = level,
            startSequence = summaries.first().startSequence,
            endSequence = summaries.last().endSequence,
            summaryText = summaryText,
            tokenCount = tokenCount,
            sourceType = SummarySourceType.SUMMARY,
            sourceIdsJson = sourceIdsJson
        )
    }

    /**
     * Get compacted conversation history: summaries + recent messages.
     * Uses existing summaries (may be slightly stale if async summarization in progress).
     */
    fun getCompactedHistory(sessionId: UUID, recentMessageCount: Int): List<Message> {
        // Get all messages
        val allMessages = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId)

        if (allMessages.size <= recentMessageCount) {
            // Short conversation - return all messages
            return allMessages.map { toMessage(it) }
        }

        // Split into old and recent
        val recentMessages = allMessages.takeLast(recentMessageCount)
        val lastOldMessageSequence = allMessages[allMessages.size - recentMessageCount - 1].sequenceNumber

        // Get highest-level active summary that covers old messages
        val summary = findBestSummary(sessionId, endSequence = lastOldMessageSequence)

        return if (summary != null) {
            // Use summary + recent messages
            logger.debug("Using level-${summary.summaryLevel} summary (sequences ${summary.startSequence}-${summary.endSequence}) + ${recentMessages.size} recent messages")
            listOf(SystemMessage("Previous conversation summary: ${summary.summaryText}")) +
                recentMessages.map { toMessage(it) }
        } else {
            // No summary yet - return all messages
            logger.debug("No summary available, returning all ${allMessages.size} messages")
            allMessages.map { toMessage(it) }
        }
    }

    /**
     * Find the best (highest-level, active) summary covering a sequence range.
     */
    private fun findBestSummary(sessionId: UUID, endSequence: Int): MessageSummaryEntity? {
        val allSummaries = summaryRepository
            .findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        // Find highest-level summary that covers the range [0, endSequence]
        return allSummaries
            .filter { it.startSequence == 0 && it.endSequence >= endSequence }
            .maxByOrNull { it.summaryLevel }
    }

    /**
     * Convert ChatMessageEntity to Spring AI Message.
     */
    private fun toMessage(entity: ChatMessageEntity): Message {
        return when (entity.role) {
            MessageRole.USER -> UserMessage(entity.content)
            MessageRole.ASSISTANT -> AssistantMessage(entity.content)
        }
    }

    /**
     * Estimate token count (4 chars ≈ 1 token).
     */
    private fun estimateTokens(texts: List<String>): Int {
        return texts.sumOf { it.length } / 4
    }
}
