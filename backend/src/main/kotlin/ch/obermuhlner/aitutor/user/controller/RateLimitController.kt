package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.dto.RateLimitStatusResponse
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
    private val userRepository: UserRepository
) {

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
            percentageUsed = status.percentageUsed,
            planName = status.planName,
            subscriptionPlan = user.subscriptionPlan.name
        )

        return ResponseEntity.ok(response)
    }
}
