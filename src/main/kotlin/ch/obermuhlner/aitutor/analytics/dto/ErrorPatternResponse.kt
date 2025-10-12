package ch.obermuhlner.aitutor.analytics.dto

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.analytics.domain.RecentErrorSampleEntity
import java.time.Instant
import java.util.UUID

data class ErrorPatternResponse(
    val errorType: String,
    val totalCount: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val weightedScore: Double,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant
)

fun ErrorPatternEntity.toResponse() = ErrorPatternResponse(
    errorType = errorType.name,
    totalCount = totalCount,
    criticalCount = criticalCount,
    highCount = highCount,
    mediumCount = mediumCount,
    lowCount = lowCount,
    weightedScore = computeWeightedScore(),
    firstSeenAt = firstSeenAt,
    lastSeenAt = lastSeenAt
)

data class ErrorTrendResponse(
    val errorType: String,
    val trend: String  // "IMPROVING", "STABLE", "WORSENING", "INSUFFICIENT_DATA"
)

data class ErrorSampleResponse(
    val id: UUID,
    val errorType: String,
    val severity: String,
    val errorSpan: String?,
    val occurredAt: Instant
)

fun RecentErrorSampleEntity.toResponse() = ErrorSampleResponse(
    id = id,
    errorType = errorType.name,
    severity = severity.name,
    errorSpan = errorSpan,
    occurredAt = occurredAt!!
)
