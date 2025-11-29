package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.config.RateLimitProperties
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import ch.obermuhlner.aitutor.user.dto.RateLimitStatusResponse
import ch.obermuhlner.aitutor.user.dto.SubscriptionPlanLimitsResponse
import ch.obermuhlner.aitutor.user.dto.UpdateUserSubscriptionPlanRequest
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST API controller for rate limiting status and information.
 */
@RestController
@RequestMapping("/api/v1/rate-limits")
class RateLimitController(
    private val authorizationService: AuthorizationService,
    private val rateLimitingService: RateLimitingService,
    private val userRepository: UserRepository,
    private val rateLimitProperties: RateLimitProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get the current user's rate limit status.
     * Shows available tokens, limits, and usage percentage.
     *
     * @return Rate limit status for the current user
     */
    @GetMapping("/status")
    fun getRateLimitStatus(): ResponseEntity<RateLimitStatusResponse> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found")
        }

        val status = rateLimitingService.getRateLimitStatus(userId, user.subscriptionPlan)

        val response = RateLimitStatusResponse(
            availableTokens = status.availableTokens,
            hourlyLimit = status.hourlyLimit,
            dailyLimit = status.dailyLimit,
            hourlyRemaining = status.hourlyRemaining,
            dailyRemaining = status.dailyRemaining,
            hourlyResetSeconds = status.hourlyResetSeconds,
            dailyResetSeconds = status.dailyResetSeconds,
            percentageUsed = status.percentageUsed,
            planName = status.planName,
            subscriptionPlan = user.subscriptionPlan.name
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Update the current user's subscription plan.
     * Currently only allows upgrading to higher-tier plans.
     *
     * @param request The subscription plan update request
     * @return Updated rate limit status
     */
    @PatchMapping("/subscription-plan")
    fun updateUserSubscriptionPlan(@RequestBody request: UpdateUserSubscriptionPlanRequest): ResponseEntity<RateLimitStatusResponse> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId).orElseThrow {
            throw RuntimeException("User not found")
        }

        // Only allow upgrading to higher tier plans
        val oldPlan = user.subscriptionPlan
        val newPlan = SubscriptionPlan.valueOf(request.subscriptionPlan)

        when {
            newPlan == SubscriptionPlan.FREE_BYOK && oldPlan == SubscriptionPlan.FREE -> {
                // Upgrade from FREE to FREE_BYOK is allowed
            }
            newPlan == SubscriptionPlan.SUBSCRIPTION_10 && (oldPlan == SubscriptionPlan.FREE || oldPlan == SubscriptionPlan.FREE_BYOK) -> {
                // Upgrade from FREE/FREE_BYOK to SUBSCRIPTION_10 is allowed
            }
            else -> {
                // Downgrading is not allowed
                throw IllegalArgumentException("Cannot downgrade from ${oldPlan.name} to ${newPlan.name}")
            }
        }

        logger.info("User {} updating subscription plan from {} to {}", userId, oldPlan, newPlan)

        user.subscriptionPlan = newPlan
        userRepository.save(user)

        // Reset rate limit buckets to apply new limits immediately
        rateLimitingService.resetRateLimit(userId)

        val status = rateLimitingService.getRateLimitStatus(userId, user.subscriptionPlan)

        val response = RateLimitStatusResponse(
            availableTokens = status.availableTokens,
            hourlyLimit = status.hourlyLimit,
            dailyLimit = status.dailyLimit,
            hourlyRemaining = status.hourlyRemaining,
            dailyRemaining = status.dailyRemaining,
            hourlyResetSeconds = status.hourlyResetSeconds,
            dailyResetSeconds = status.dailyResetSeconds,
            percentageUsed = status.percentageUsed,
            planName = status.planName,
            subscriptionPlan = user.subscriptionPlan.name
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get the configured rate limits for all subscription plans.
     * This allows the frontend to display accurate plan information.
     *
     * @return Configured rate limits for all subscription plans
     */
    @GetMapping("/plan-limits")
    fun getPlanLimits(): ResponseEntity<SubscriptionPlanLimitsResponse> {
        val response = SubscriptionPlanLimitsResponse(
            free = SubscriptionPlanLimitsResponse.PlanLimits(
                hourlyLimit = rateLimitProperties.free.messagesPerHour,
                dailyLimit = rateLimitProperties.free.messagesPerDay
            ),
            freeByok = SubscriptionPlanLimitsResponse.PlanLimits(
                hourlyLimit = rateLimitProperties.freeByok.messagesPerHour,
                dailyLimit = rateLimitProperties.freeByok.messagesPerDay
            ),
            premium = SubscriptionPlanLimitsResponse.PlanLimits(
                hourlyLimit = rateLimitProperties.premium.messagesPerHour,
                dailyLimit = rateLimitProperties.premium.messagesPerDay
            )
        )

        return ResponseEntity.ok(response)
    }
}
