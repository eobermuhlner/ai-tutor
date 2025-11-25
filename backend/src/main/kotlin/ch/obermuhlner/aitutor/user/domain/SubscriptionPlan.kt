@file:Suppress("DEPRECATION")

package ch.obermuhlner.aitutor.user.domain

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import java.time.Duration

/**
 * Subscription plan with associated rate limiting configurations.
 * Each plan defines hourly and daily limits for AI LLM calls.
 */
enum class SubscriptionPlan(
    val displayName: String,
    val messagesPerHour: Long,
    val messagesPerDay: Long,
    val description: String
) {
    /**
     * Free plan with basic rate limits.
     * Suitable for casual learners trying out the platform.
     */
    FREE(
        displayName = "Free",
        messagesPerHour = 10,
        messagesPerDay = 50,
        description = "Basic free access with limited AI interactions"
    ),

    /**
     * Free plan with user's own API key (Bring Your Own Key).
     * Higher limits since user pays for their own API usage.
     */
    FREE_BYOK(
        displayName = "Free + BYOK",
        messagesPerHour = 60,
        messagesPerDay = 300,
        description = "Free access using your own AI provider API key"
    ),

    /**
     * Premium subscription plan ($10/month).
     * Generous limits for regular learners.
     */
    SUBSCRIPTION_10(
        displayName = "Premium ($10/month)",
        messagesPerHour = 100,
        messagesPerDay = 500,
        description = "Premium subscription with high message limits"
    );

    /**
     * Creates a new Bucket4j bucket configured with this plan's rate limits.
     * Uses both hourly and daily bandwidth limits with greedy refill strategy.
     *
     * @return A new Bucket instance configured for this subscription plan
     */
    fun createBucket(): Bucket {
        // Hourly limit: refills at rate of messagesPerHour tokens per hour
        val hourlyLimit = Bandwidth.classic(
            messagesPerHour,
            Refill.greedy(messagesPerHour, Duration.ofHours(1))
        )

        // Daily limit: refills at rate of messagesPerDay tokens per day
        val dailyLimit = Bandwidth.classic(
            messagesPerDay,
            Refill.greedy(messagesPerDay, Duration.ofDays(1))
        )

        return Bucket.builder()
            .addLimit(hourlyLimit)
            .addLimit(dailyLimit)
            .build()
    }

    /**
     * Returns a human-readable summary of rate limits.
     */
    fun getRateLimitSummary(): String {
        return "$messagesPerHour messages/hour, $messagesPerDay messages/day"
    }
}
