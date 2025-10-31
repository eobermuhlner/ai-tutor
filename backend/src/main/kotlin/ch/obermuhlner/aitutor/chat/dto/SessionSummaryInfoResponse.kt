package ch.obermuhlner.aitutor.chat.dto

import java.time.Instant
import java.util.UUID

/**
 * High-level summary statistics for a session.
 */
data class SessionSummaryInfoResponse(
    val sessionId: UUID,
    val totalMessages: Int,
    val summaryLevels: List<SummaryLevelInfo>,
    val lastSummarizedSequence: Int?,
    val estimatedTokenSavings: Int,  // Tokens saved by using summaries vs full history
    val compressionRatio: Double     // (original tokens) / (compacted tokens)
)

data class SummaryLevelInfo(
    val level: Int,
    val count: Int,          // Number of summaries at this level
    val totalTokens: Int,    // Sum of tokens in all summaries at this level
    val coveredSequences: IntRange?  // Range of message sequences covered
)

/**
 * Detailed view of a single summary (for admin debugging).
 */
data class SummaryDetailResponse(
    val id: UUID,
    val summaryLevel: Int,
    val startSequence: Int,
    val endSequence: Int,
    val summaryText: String,
    val tokenCount: Int,
    val sourceType: String,
    val sourceIds: List<UUID>,
    val supersededById: UUID?,
    val isActive: Boolean,
    val createdAt: Instant
)
