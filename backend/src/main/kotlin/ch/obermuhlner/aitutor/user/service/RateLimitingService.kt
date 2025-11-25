@file:Suppress("DEPRECATION")

package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.core.exception.RateLimitExceededException
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.Refill
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for rate limiting AI LLM calls using bucket4j token bucket algorithm.
 * Each user has their own bucket configured according to their subscription plan.
 */
@Service
class RateLimitingService {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Cache of buckets per user ID - separate for hourly and daily limits
    private val hourlyBuckets = ConcurrentHashMap<UUID, Bucket>()
    private val dailyBuckets = ConcurrentHashMap<UUID, Bucket>()

    /**
     * Checks if the user can make an AI LLM call and consumes a token if available.
     * Throws RateLimitExceededException if the user has exceeded their rate limit.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkRateLimit(userId: UUID, subscriptionPlan: SubscriptionPlan) {
        val hourlyBucket = getHourlyBucketForUser(userId, subscriptionPlan)
        val dailyBucket = getDailyBucketForUser(userId, subscriptionPlan)
        
        // Check hourly limit first
        val hourlyProbe: ConsumptionProbe = hourlyBucket.tryConsumeAndReturnRemaining(1)
        if (!hourlyProbe.isConsumed) {
            val waitTime = Duration.ofNanos(hourlyProbe.nanosToWaitForRefill)
            logger.warn(
                "Hourly rate limit exceeded for user {} with plan {}. Retry after {} seconds",
                userId,
                subscriptionPlan.name,
                waitTime.seconds
            )

            throw RateLimitExceededException(
                userId = userId.toString(),
                retryAfter = waitTime,
                limitType = "hourly"
            )
        }
        
        // Check daily limit
        val dailyProbe: ConsumptionProbe = dailyBucket.tryConsumeAndReturnRemaining(1)
        if (!dailyProbe.isConsumed) {
            val waitTime = Duration.ofNanos(dailyProbe.nanosToWaitForRefill)
            logger.warn(
                "Daily rate limit exceeded for user {} with plan {}. Retry after {} seconds",
                userId,
                subscriptionPlan.name,
                waitTime.seconds
            )

            throw RateLimitExceededException(
                userId = userId.toString(),
                retryAfter = waitTime,
                limitType = "daily"
            )
        }

        logger.debug(
            "Rate limit check passed for user {}. Hourly remaining: {}, Daily remaining: {}",
            userId,
            hourlyProbe.remainingTokens,
            dailyProbe.remainingTokens
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
        val hourlyBucket = getHourlyBucketForUser(userId, subscriptionPlan)
        val dailyBucket = getDailyBucketForUser(userId, subscriptionPlan)
        
        // Get available tokens for each bucket
        val hourlyAvailable = hourlyBucket.availableTokens
        val dailyAvailable = dailyBucket.availableTokens
        
        // For reset times, we need to determine when the next refill would occur
        // Using a conservative approach: estimate based on refill rate
        val hourlyResetSeconds = estimateResetTime(hourlyAvailable, subscriptionPlan.messagesPerHour, 3600L)
        val dailyResetSeconds = estimateResetTime(dailyAvailable, subscriptionPlan.messagesPerDay, 86400L)
        
        // For available tokens, we return the minimum since that's what limits the user
        val availableTokens = minOf(hourlyAvailable, dailyAvailable)

        return RateLimitStatus(
            availableTokens = availableTokens,
            hourlyLimit = subscriptionPlan.messagesPerHour,
            dailyLimit = subscriptionPlan.messagesPerDay,
            hourlyRemaining = hourlyAvailable,
            dailyRemaining = dailyAvailable,
            hourlyResetSeconds = hourlyResetSeconds,
            dailyResetSeconds = dailyResetSeconds,
            planName = subscriptionPlan.displayName
        )
    }
    
    private fun estimateResetTime(currentTokens: Long, maxTokens: Long, periodSeconds: Long): Long {
        if (currentTokens >= maxTokens) {
            // Bucket is full, next refill will be after the full period
            return periodSeconds
        }
        
        // Estimate based on refill rate: time until bucket is full
        // Refill rate is maxTokens per periodSeconds
        val tokensToRefill = maxTokens - currentTokens
        val timePerToken = periodSeconds.toDouble() / maxTokens.toDouble()
        val estimatedTime = (tokensToRefill * timePerToken).toLong()
        
        // Cap at the maximum period
        return minOf(estimatedTime, periodSeconds)
    }

    /**
     * Resets the rate limit bucket for a user.
     * Useful when a user upgrades their subscription plan.
     *
     * @param userId The user's UUID
     */
    fun resetRateLimit(userId: UUID) {
        hourlyBuckets.remove(userId)
        dailyBuckets.remove(userId)
        logger.info("Rate limit buckets reset for user {}", userId)
    }

    /**
     * Gets or creates an hourly bucket for the specified user.
     * Buckets are cached in memory for the lifetime of the application.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @return The user's hourly rate limiting bucket
     */
    private fun getHourlyBucketForUser(userId: UUID, subscriptionPlan: SubscriptionPlan): Bucket {
        return hourlyBuckets.computeIfAbsent(userId) {
            logger.debug("Creating new hourly rate limit bucket for user {} with plan {}", userId, subscriptionPlan.name)
            createHourlyBucket(subscriptionPlan)
        }
    }
    
    /**
     * Gets or creates a daily bucket for the specified user.
     * Buckets are cached in memory for the lifetime of the application.
     *
     * @param userId The user's UUID
     * @param subscriptionPlan The user's subscription plan
     * @return The user's daily rate limiting bucket
     */
    private fun getDailyBucketForUser(userId: UUID, subscriptionPlan: SubscriptionPlan): Bucket {
        return dailyBuckets.computeIfAbsent(userId) {
            logger.debug("Creating new daily rate limit bucket for user {} with plan {}", userId, subscriptionPlan.name)
            createDailyBucket(subscriptionPlan)
        }
    }
    
    /**
     * Creates a new bucket configured with the hourly rate limit from the subscription plan.
     *
     * @param subscriptionPlan The subscription plan containing rate limits
     * @return A new Bucket instance configured for hourly limits
     */
    @Suppress("DEPRECATION")
    private fun createHourlyBucket(subscriptionPlan: SubscriptionPlan): Bucket {
        val hourlyLimit = Bandwidth.classic(
            subscriptionPlan.messagesPerHour,
            Refill.greedy(subscriptionPlan.messagesPerHour, Duration.ofHours(1))
        )
        return Bucket.builder().addLimit(hourlyLimit).build()
    }
    
    /**
     * Creates a new bucket configured with the daily rate limit from the subscription plan.
     *
     * @param subscriptionPlan The subscription plan containing rate limits
     * @return A new Bucket instance configured for daily limits
     */
    @Suppress("DEPRECATION")
    private fun createDailyBucket(subscriptionPlan: SubscriptionPlan): Bucket {
        val dailyLimit = Bandwidth.classic(
            subscriptionPlan.messagesPerDay,
            Refill.greedy(subscriptionPlan.messagesPerDay, Duration.ofDays(1))
        )
        return Bucket.builder().addLimit(dailyLimit).build()
    }

    /**
     * Data class representing the current rate limit status for a user.
     */
    data class RateLimitStatus(
        val availableTokens: Long,
        val hourlyLimit: Long,
        val dailyLimit: Long,
        val hourlyRemaining: Long,
        val dailyRemaining: Long,
        val hourlyResetSeconds: Long,
        val dailyResetSeconds: Long,
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
