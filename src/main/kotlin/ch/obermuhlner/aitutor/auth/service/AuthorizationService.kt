package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.service.UserService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get the currently authenticated user's ID from SecurityContext
     * @throws IllegalStateException if no user is authenticated
     */
    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication found in SecurityContext")

        val userDetails = authentication.principal as? UserDetails
            ?: throw IllegalStateException("Authentication principal is not UserDetails")

        val user = userService.findByUsername(userDetails.username)
            ?: throw IllegalStateException("Authenticated user not found: ${userDetails.username}")

        return user.id
    }

    /**
     * Get the currently authenticated user entity
     * @throws IllegalStateException if no user is authenticated
     */
    fun getCurrentUser(): UserEntity {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication found in SecurityContext")

        val userDetails = authentication.principal as? UserDetails
            ?: throw IllegalStateException("Authentication principal is not UserDetails")

        return userService.findByUsername(userDetails.username)
            ?: throw IllegalStateException("Authenticated user not found: ${userDetails.username}")
    }

    /**
     * Check if the currently authenticated user has ADMIN role
     */
    fun isAdmin(): Boolean {
        return try {
            val user = getCurrentUser()
            user.roles.contains(UserRole.ADMIN)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the current user can access data for the target user
     * - Users can access their own data
     * - Admins can access any user's data
     */
    fun canAccessUser(targetUserId: UUID): Boolean {
        return try {
            val currentUserId = getCurrentUserId()
            currentUserId == targetUserId || isAdmin()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate that the current user can access the target user's data
     * @throws InsufficientPermissionsException if access is not allowed
     */
    fun requireAccessToUser(targetUserId: UUID) {
        if (!canAccessUser(targetUserId)) {
            logger.warn("Access denied: user ${getCurrentUserId()} attempted to access user $targetUserId")
            throw InsufficientPermissionsException("You do not have permission to access this user's data")
        }
    }

    /**
     * Get user ID from optional parameter or use authenticated user
     * - If userId is null, returns current user's ID
     * - If userId is provided and current user is admin, returns provided ID
     * - If userId is provided and current user is not admin, validates it matches current user
     * @throws InsufficientPermissionsException if non-admin tries to access other user's data
     */
    fun resolveUserId(requestedUserId: UUID?): UUID {
        val currentUserId = getCurrentUserId()

        return when {
            requestedUserId == null -> currentUserId
            requestedUserId == currentUserId -> currentUserId
            isAdmin() -> requestedUserId
            else -> throw InsufficientPermissionsException("You do not have permission to access this user's data")
        }
    }
}
