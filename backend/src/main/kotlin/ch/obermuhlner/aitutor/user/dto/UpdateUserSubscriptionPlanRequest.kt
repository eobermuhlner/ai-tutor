package ch.obermuhlner.aitutor.user.dto

/**
 * Request DTO for updating a user's own subscription plan.
 */
data class UpdateUserSubscriptionPlanRequest(
    val subscriptionPlan: String
)