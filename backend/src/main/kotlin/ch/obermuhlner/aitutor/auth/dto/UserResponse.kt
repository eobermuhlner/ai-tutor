package ch.obermuhlner.aitutor.auth.dto

import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.domain.PronunciationPreference
import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val roles: Set<UserRole>,
    val enabled: Boolean,
    val locked: Boolean,
    val emailVerified: Boolean,
    val createdAt: Instant?,
    val lastLoginAt: Instant?,
    val subscriptionPlan: SubscriptionPlan,
    val pronunciationPreference: PronunciationPreference
)
