package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.core.exception.RateLimitExceededException
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for rate limiting AI LLM calls using bucket4j token bucket algorithm.
 * Each user has their own bucket configured according to their subscription plan.
 */
@Service
class RateLimitingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Cache of buckets per user ID
    private val buckets = ConcurrentHashMap<UUID, Bucket>()

    /**
     * Checks if the user can make an AI LLM call and consumes a token if available.
     * Throws RateLimitExceededException if the user has exceeded their rate limit.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkRateLimit(userId: UUID, subscriptionPlan: SubscriptionPlan) {
        val bucket = getBucketForUser(userId, subscriptionPlan)
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

        if (!probe.isConsumed) {
            val waitTime = Duration.ofNanos(probe.nanosToWaitForRefill)
            logger.warn(
                "Rate limit exceeded for user {} with plan {}. Retry after {} seconds",
                userId,
                subscriptionPlan.name,
                waitTime.seconds
            )

            // Determine which limit was hit based on wait time
            val limitType = if (waitTime.toHours() < 1) "hourly" else "daily"

            throw RateLimitExceededException(
                userId = userId.toString(),
                retryAfter = waitTime,
                limitType = limitType
            )
        }

        logger.debug(
            "Rate limit check passed for user {}. Tokens remaining: {}",
            userId,
            probe.remainingTokens
        )
    }

    /**
     * Gets the current rate limit status for a user without consuming a token.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @return RateLimitStatus containing available tokens and limits
     */
    fun getRateLimitStatus(userId: UUID, subscriptionPlan: SubscriptionPlan): RateLimitStatus {
        val bucket = getBucketForUser(userId, subscriptionPlan)
        val availableTokens = bucket.availableTokens

        return RateLimitStatus(
            availableTokens = availableTokens,
            hourlyLimit = subscriptionPlan.messagesPerHour,
            dailyLimit = subscriptionPlan.messagesPerDay,
            planName = subscriptionPlan.displayName
        )
    }

    /**
     * Resets the rate limit bucket for a user.
     * Useful when a user upgrades their subscription plan.
     *
     * @param userId The user's UUID
     */
    fun resetRateLimit(userId: UUID) {
        buckets.remove(userId)
        logger.info("Rate limit bucket reset for user {}", userId)
    }

    /**
     * Gets or creates a bucket for the specified user.
     * Buckets are cached in memory for the lifetime of the application.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @return The user's rate limiting bucket
     */
    private fun getBucketForUser(userId: UUID, subscriptionPlan: SubscriptionPlan): Bucket {
        return buckets.computeIfAbsent(userId) {
            logger.debug("Creating new rate limit bucket for user {} with plan {}", userId, subscriptionPlan.name)
            subscriptionPlan.createBucket()
        }
    }

    /**
     * Data class representing the current rate limit status for a user.
     */
    data class RateLimitStatus(
        val availableTokens: Long,
        val hourlyLimit: Long,
        val dailyLimit: Long,
        val planName: String
    ) {
        val percentageUsed: Double
            get() = if (dailyLimit > 0) {
                ((dailyLimit - availableTokens).toDouble() / dailyLimit.toDouble()) * 100.0
            } else {
                0.0
            }
    }
}
