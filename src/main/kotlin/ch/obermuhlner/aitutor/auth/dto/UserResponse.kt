package ch.obermuhlner.aitutor.auth.dto

import ch.obermuhlner.aitutor.user.domain.UserRole
import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val roles: Set<UserRole>,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val createdAt: Instant?,
    val lastLoginAt: Instant?
)
