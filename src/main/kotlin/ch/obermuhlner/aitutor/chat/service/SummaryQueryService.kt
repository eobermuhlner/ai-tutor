package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryLevelInfo
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.repository.MessageSummaryRepository
import ch.obermuhlner.aitutor.tutor.service.ProgressiveSummarizationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SummaryQueryService(
    private val summaryRepository: MessageSummaryRepository,
    private val messageRepository: ChatMessageRepository,
    private val sessionRepository: ChatSessionRepository,
    private val progressiveSummarizationService: ProgressiveSummarizationService
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Get summary statistics for a session.
     */
    fun getSessionSummaryInfo(sessionId: UUID): SessionSummaryInfoResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }

        val allSummaries = summaryRepository
            .findBySessionIdAndIsActiveTrueOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        val allMessages = messageRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId)

        // Group summaries by level
        val levelGroups = allSummaries.groupBy { it.summaryLevel }

        val summaryLevels = levelGroups.map { (level, summaries) ->
            SummaryLevelInfo(
                level = level,
                count = summaries.size,
                totalTokens = summaries.sumOf { it.tokenCount },
                coveredSequences = if (summaries.isNotEmpty()) {
                    summaries.minOf { it.startSequence }..summaries.maxOf { it.endSequence }
                } else null
            )
        }.sortedBy { it.level }

        // Calculate token savings
        val lastSummarizedSequence = allSummaries.maxOfOrNull { it.endSequence }
        val originalTokens = estimateTokens(allMessages.map { it.content })

        // Compacted tokens = tokens from ONLY the highest level summaries that would be used in practice
        // In a hierarchical system, we use summaries from a single level, not all levels summed together
        val compactedTokens = if (allSummaries.isNotEmpty() && lastSummarizedSequence != null) {
            // Use only the highest level summaries (most compressed)
            val highestLevel = allSummaries.maxOf { it.summaryLevel }
            val highestLevelSummaries = allSummaries.filter { it.summaryLevel == highestLevel }
            val summaryTokens = highestLevelSummaries.sumOf { it.tokenCount }

            // Add tokens from messages after the last summarized sequence
            val unsummarizedMessages = allMessages.filter { it.sequenceNumber > lastSummarizedSequence }
            val unsummarizedTokens = estimateTokens(unsummarizedMessages.map { it.content })

            summaryTokens + unsummarizedTokens
        } else {
            originalTokens
        }

        return SessionSummaryInfoResponse(
            sessionId = sessionId,
            totalMessages = allMessages.size,
            summaryLevels = summaryLevels,
            lastSummarizedSequence = lastSummarizedSequence,
            estimatedTokenSavings = originalTokens - compactedTokens,
            compressionRatio = if (originalTokens > 0) compactedTokens.toDouble() / originalTokens else 1.0
        )
    }

    /**
     * Get detailed view of all summaries for a session.
     */
    fun getSessionSummaryDetails(sessionId: UUID): List<SummaryDetailResponse> {
        val allSummaries = summaryRepository
            .findBySessionIdOrderBySummaryLevelAscStartSequenceAsc(sessionId)

        return allSummaries.map { summary ->
            val sourceIds: List<String> = objectMapper.readValue(summary.sourceIdsJson)
            SummaryDetailResponse(
                id = summary.id,
                summaryLevel = summary.summaryLevel,
                startSequence = summary.startSequence,
                endSequence = summary.endSequence,
                summaryText = summary.summaryText,
                tokenCount = summary.tokenCount,
                sourceType = summary.sourceType.name,
                sourceIds = sourceIds.map { UUID.fromString(it) },
                supersededById = summary.supersededById,
                isActive = summary.isActive,
                createdAt = summary.createdAt ?: Instant.now()
            )
        }
    }

    /**
     * Trigger manual summarization for a session.
     */
    fun triggerManualSummarization(sessionId: UUID) {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found: $sessionId") }

        progressiveSummarizationService.checkAndSummarize(sessionId)
    }

    /**
     * Get global summarization statistics.
     */
    fun getGlobalStats(): Map<String, Any> {
        val allSummaries = summaryRepository.findAll()
        val allMessages = messageRepository.findAll()

        val summariesByLevel = allSummaries.groupBy { it.summaryLevel }

        val totalTokensOriginal = estimateTokens(allMessages.map { it.content })
        val totalTokensCompacted = allSummaries
            .filter { it.isActive }
            .sumOf { it.tokenCount }

        return mapOf(
            "totalSummaries" to allSummaries.size,
            "activeSummaries" to allSummaries.count { it.isActive },
            "summariesByLevel" to summariesByLevel.mapValues { it.value.size },
            "totalMessages" to allMessages.size,
            "totalTokensOriginal" to totalTokensOriginal,
            "totalTokensCompacted" to totalTokensCompacted,
            "globalCompressionRatio" to if (totalTokensOriginal > 0) {
                totalTokensCompacted.toDouble() / totalTokensOriginal
            } else 1.0
        )
    }

    private fun estimateTokens(texts: List<String>): Int {
        return texts.sumOf { it.length } / 4
    }
}
