package ch.obermuhlner.aitutor.user.dto

/**
 * Response DTO for subscription plan limits.
 * Provides information about the configured rate limits for each subscription plan.
 */
data class SubscriptionPlanLimitsResponse(
    val free: PlanLimits,
    val freeByok: PlanLimits,
    val premium: PlanLimits
) {
    data class PlanLimits(
        val hourlyLimit: Long,
        val dailyLimit: Long
    )
}