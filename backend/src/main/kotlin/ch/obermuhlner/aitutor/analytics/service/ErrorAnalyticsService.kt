package ch.obermuhlner.aitutor.analytics.service

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.analytics.domain.RecentErrorSampleEntity
import ch.obermuhlner.aitutor.analytics.repository.ErrorPatternRepository
import ch.obermuhlner.aitutor.analytics.repository.RecentErrorSampleRepository
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Service
class ErrorAnalyticsService(
    private val errorPatternRepository: ErrorPatternRepository,
    private val recentErrorSampleRepository: RecentErrorSampleRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_SAMPLES_PER_USER = 100
    }

    /**
     * Record errors from a chat message and update patterns.
     */
    @Transactional
    fun recordErrors(
        userId: UUID,
        lang: String,
        messageId: UUID,
        corrections: List<Correction>
    ) {
        if (corrections.isEmpty()) return

        logger.debug("Recording ${corrections.size} errors for user $userId in $lang")

        corrections.forEach { correction ->
            // Update aggregate pattern
            updatePattern(userId, lang, correction)

            // Store sample
            storeSample(userId, lang, messageId, correction)
        }

        // Prune old samples if exceeds limit
        pruneSamplesIfNeeded(userId)
    }

    private fun updatePattern(userId: UUID, lang: String, correction: Correction) {
        val pattern = errorPatternRepository.findByUserIdAndLangAndErrorType(
            userId, lang, correction.errorType
        ) ?: ErrorPatternEntity(
            userId = userId,
            lang = lang,
            errorType = correction.errorType,
            firstSeenAt = Instant.now(),
            lastSeenAt = Instant.now()
        )

        pattern.totalCount += 1
        pattern.lastSeenAt = Instant.now()

        when (correction.severity) {
            ErrorSeverity.Critical -> pattern.criticalCount += 1
            ErrorSeverity.High -> pattern.highCount += 1
            ErrorSeverity.Medium -> pattern.mediumCount += 1
            ErrorSeverity.Low -> pattern.lowCount += 1
        }

        errorPatternRepository.save(pattern)
    }

    private fun storeSample(
        userId: UUID,
        lang: String,
        messageId: UUID,
        correction: Correction
    ) {
        val sample = RecentErrorSampleEntity(
            userId = userId,
            lang = lang,
            errorType = correction.errorType,
            severity = correction.severity,
            messageId = messageId,
            errorSpan = correction.span.take(256)
        )
        recentErrorSampleRepository.save(sample)
    }

    private fun pruneSamplesIfNeeded(userId: UUID) {
        val count = recentErrorSampleRepository.countByUserId(userId)
        if (count > MAX_SAMPLES_PER_USER) {
            val excessCount = (count - MAX_SAMPLES_PER_USER).toInt()

            // Get oldest samples to delete
            val oldestSamples = recentErrorSampleRepository.findByUserIdOrderByOccurredAtDesc(
                userId,
                PageRequest.of(0, count.toInt())
            ).reversed().take(excessCount)

            recentErrorSampleRepository.deleteAll(oldestSamples)
            logger.debug("Pruned $excessCount old error samples for user $userId")
        }
    }

    /**
     * Get top error patterns by weighted severity score.
     */
    fun getTopPatterns(userId: UUID, lang: String, limit: Int = 5): List<ErrorPatternEntity> {
        return errorPatternRepository.findTopPatternsByWeightedScore(userId, lang)
            .take(limit)
    }

    /**
     * Compute trend for a specific error type.
     * Returns "IMPROVING", "STABLE", "WORSENING", or "INSUFFICIENT_DATA".
     */
    fun computeTrend(userId: UUID, errorType: ErrorType): String {
        val samples = recentErrorSampleRepository.findByUserIdAndErrorTypeOrderByOccurredAtDesc(
            userId, errorType, PageRequest.of(0, MAX_SAMPLES_PER_USER)
        )

        if (samples.size < 20) {
            return "INSUFFICIENT_DATA"
        }

        // Split into two halves (recent vs older)
        val midpoint = samples.size / 2
        val recentHalf = samples.take(midpoint)
        val olderHalf = samples.drop(midpoint)

        val recentScore = recentHalf.sumOf { severityWeight(it.severity) }
        val olderScore = olderHalf.sumOf { severityWeight(it.severity) }

        return when {
            recentScore < olderScore * 0.7 -> "IMPROVING"
            recentScore > olderScore * 1.3 -> "WORSENING"
            else -> "STABLE"
        }
    }

    private fun severityWeight(severity: ErrorSeverity): Double {
        return when (severity) {
            ErrorSeverity.Critical -> 3.0
            ErrorSeverity.High -> 2.0
            ErrorSeverity.Medium -> 1.0
            ErrorSeverity.Low -> 0.3
        }
    }

    /**
     * Get recent error samples for a user (for debugging/UI).
     */
    fun getRecentSamples(userId: UUID, limit: Int = 20): List<RecentErrorSampleEntity> {
        return recentErrorSampleRepository.findByUserIdOrderByOccurredAtDesc(
            userId, PageRequest.of(0, limit)
        )
    }
}
