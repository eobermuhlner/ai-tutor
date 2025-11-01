package ch.obermuhlner.aitutor.admin.dto

import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan

/**
 * Request DTO for updating a user's subscription plan.
 * Admin-only operation.
 */
data class UpdateSubscriptionPlanRequest(
    val subscriptionPlan: SubscriptionPlan
)
