package ch.obermuhlner.aitutor.admin.controller

import ch.obermuhlner.aitutor.admin.dto.UpdateSubscriptionPlanRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST API controller for admin operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val authorizationService: AuthorizationService,
    private val userRepository: UserRepository,
    private val rateLimitingService: RateLimitingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Update a user's subscription plan.
     * Only accessible by admins.
     *
     * @param userId The user ID to update
     * @param request The subscription plan update request
     * @return Updated user information
     */
    @PatchMapping("/users/{userId}/subscription-plan")
    fun updateUserSubscriptionPlan(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateSubscriptionPlanRequest
    ): ResponseEntity<UserResponse> {
        // Check admin permission
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.info("Admin updating subscription plan for user {} to {}", userId, request.subscriptionPlan)

        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found: $userId")
        }

        val oldPlan = user.subscriptionPlan
        user.subscriptionPlan = request.subscriptionPlan
        userRepository.save(user)

        // Reset rate limit bucket when subscription changes
        rateLimitingService.resetRateLimit(userId)

        logger.info(
            "Subscription plan updated for user {}: {} -> {}",
            userId,
            oldPlan,
            request.subscriptionPlan
        )

        val response = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            enabled = user.enabled,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan
        )

        return ResponseEntity.ok(response)
    }
}
