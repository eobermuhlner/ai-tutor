package ch.obermuhlner.aitutor.admin.controller

import ch.obermuhlner.aitutor.admin.dto.UpdateSubscriptionPlanRequest
import ch.obermuhlner.aitutor.admin.dto.UpdateUserRequest
import ch.obermuhlner.aitutor.admin.dto.UsersPageResponse
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API controller for admin operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val authorizationService: AuthorizationService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
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
            locked = user.locked,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan,
            pronunciationPreference = user.pronunciationPreference,
            provider = user.provider
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get all users with pagination and filtering.
     * Only accessible by admins.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @param search Optional search term (searches username, email, firstName, lastName)
     * @param role Optional role filter
     * @param subscriptionPlan Optional subscription plan filter
     * @param enabled Optional enabled status filter
     * @param locked Optional locked status filter
     * @return Paginated list of users
     */
    @GetMapping("/users")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false) subscriptionPlan: SubscriptionPlan?,
        @RequestParam(required = false) enabled: Boolean?,
        @RequestParam(required = false) locked: Boolean?
    ): ResponseEntity<UsersPageResponse> {
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.debug("Admin fetching users list - page: $page, size: $size, search: $search")

        // For simplicity, we'll fetch all and filter in memory
        // In production, you'd want to use Spring Data JPA Specifications for database-level filtering
        var allUsers = userRepository.findAll()

        // Apply filters
        if (!search.isNullOrBlank()) {
            val searchLower = search.lowercase()
            allUsers = allUsers.filter {
                it.username.lowercase().contains(searchLower) ||
                it.email.lowercase().contains(searchLower) ||
                it.firstName?.lowercase()?.contains(searchLower) == true ||
                it.lastName?.lowercase()?.contains(searchLower) == true
            }
        }

        if (role != null) {
            allUsers = allUsers.filter { it.roles.contains(role) }
        }

        if (subscriptionPlan != null) {
            allUsers = allUsers.filter { it.subscriptionPlan == subscriptionPlan }
        }

        if (enabled != null) {
            allUsers = allUsers.filter { it.enabled == enabled }
        }

        if (locked != null) {
            allUsers = allUsers.filter { it.locked == locked }
        }

        // Sort by creation date (newest first)
        allUsers = allUsers.sortedByDescending { it.createdAt }

        // Paginate
        val totalElements = allUsers.size.toLong()
        val totalPages = ((totalElements + size - 1) / size).toInt()
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, allUsers.size)

        val pageUsers = if (startIndex < allUsers.size) {
            allUsers.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        val userResponses = pageUsers.map { toUserResponse(it) }

        val response = UsersPageResponse(
            users = userResponses,
            totalElements = totalElements,
            totalPages = totalPages,
            currentPage = page,
            pageSize = size
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Get a specific user by ID.
     * Only accessible by admins.
     *
     * @param userId The user ID
     * @return User details
     */
    @GetMapping("/users/{userId}")
    fun getUser(@PathVariable userId: UUID): ResponseEntity<UserResponse> {
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.debug("Admin fetching user: $userId")

        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found: $userId")
        }

        return ResponseEntity.ok(toUserResponse(user))
    }

    /**
     * Update a user's details.
     * Only accessible by admins.
     *
     * @param userId The user ID to update
     * @param request The update request
     * @return Updated user information
     */
    @PatchMapping("/users/{userId}")
    fun updateUser(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserResponse> {
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.info("Admin updating user: $userId")

        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found: $userId")
        }

        // Update fields if provided
        request.email?.let {
            // Check if email is already in use by another user
            val existingUser = userRepository.findByEmail(it)
            if (existingUser != null && existingUser.id != userId) {
                throw IllegalArgumentException("Email already in use")
            }
            user.email = it
            user.emailVerified = false  // Reset verification when email changes
        }

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.enabled?.let { user.enabled = it }
        request.locked?.let { user.locked = it }
        request.roles?.let { user.roles = it.toMutableSet() }

        val updatedUser = userRepository.save(user)

        logger.info("User updated successfully: ${updatedUser.username}")

        return ResponseEntity.ok(toUserResponse(updatedUser))
    }

    /**
     * Force logout a user by revoking all their refresh tokens.
     * Only accessible by admins.
     *
     * @param userId The user ID to force logout
     * @return Success message
     */
    @PostMapping("/users/{userId}/force-logout")
    fun forceLogout(@PathVariable userId: UUID): ResponseEntity<Map<String, String>> {
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.info("Admin forcing logout for user: $userId")

        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found: $userId")
        }

        // Revoke all refresh tokens
        val tokens = refreshTokenRepository.findAllByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)

        logger.info("Revoked ${tokens.size} refresh token(s) for user: ${user.username}")

        return ResponseEntity.ok(mapOf("message" to "User logged out successfully"))
    }

    /**
     * Trigger a password reset for a user.
     * Only accessible by admins.
     *
     * NOTE: This is a placeholder endpoint. In a production system, you would:
     * 1. Generate a secure password reset token
     * 2. Send an email to the user with the reset link
     * 3. Implement a separate endpoint for the user to reset their password using the token
     *
     * @param userId The user ID to reset password for
     * @return Success message
     */
    @PostMapping("/users/{userId}/reset-password")
    fun resetPassword(@PathVariable userId: UUID): ResponseEntity<Map<String, String>> {
        if (!authorizationService.isAdmin()) {
            throw InsufficientPermissionsException("Admin role required")
        }

        logger.info("Admin triggering password reset for user: $userId")

        val user = userRepository.findById(userId).orElseThrow {
            RuntimeException("User not found: $userId")
        }

        // TODO: Implement actual password reset logic
        // - Generate secure reset token
        // - Store token with expiration
        // - Send email to user

        logger.info("Password reset triggered for user: ${user.username} (email: ${user.email})")

        return ResponseEntity.ok(mapOf("message" to "Password reset email sent (placeholder)"))
    }

    /**
     * Convert UserEntity to UserResponse.
     */
    private fun toUserResponse(user: ch.obermuhlner.aitutor.user.domain.UserEntity): UserResponse {
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            enabled = user.enabled,
            locked = user.locked,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan,
            pronunciationPreference = user.pronunciationPreference,
            provider = user.provider
        )
    }
}
