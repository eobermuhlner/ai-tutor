package ch.obermuhlner.aitutor.user.dto

/**
 * Response DTO for rate limit status.
 * Provides information about the user's current rate limit usage.
 */
data class RateLimitStatusResponse(
    val availableTokens: Long,
    val hourlyLimit: Long,
    val dailyLimit: Long,
    val hourlyRemaining: Long,
    val dailyRemaining: Long,
    val hourlyResetSeconds: Long,
    val dailyResetSeconds: Long,
    val percentageUsed: Double,
    val planName: String,
    val subscriptionPlan: String
)
