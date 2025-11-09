package ch.obermuhlner.aitutor.admin.dto

import ch.obermuhlner.aitutor.user.domain.UserRole

/**
 * Request DTO for updating user details.
 * Admin-only operation.
 */
data class UpdateUserRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val enabled: Boolean? = null,
    val locked: Boolean? = null,
    val roles: Set<UserRole>? = null
)
