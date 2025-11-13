package ch.obermuhlner.aitutor.auth.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [
        Index(name = "idx_password_reset_token", columnList = "token"),
        Index(name = "idx_password_reset_user_id", columnList = "user_id")
    ]
)
class PasswordResetTokenEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "token", nullable = false, unique = true, length = 64)
    val token: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "used_at", nullable = true)
    var usedAt: Instant? = null
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    fun isUsed(): Boolean = usedAt != null
    fun isValid(): Boolean = !isExpired() && !isUsed()
}
