package ch.obermuhlner.aitutor.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_token", columnList = "token")
    ]
)
class RefreshTokenEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "token", nullable = false, unique = true, length = 512)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @Column(name = "revoked", nullable = false)
    var revoked: Boolean = false
)
