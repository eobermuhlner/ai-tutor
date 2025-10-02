package ch.obermuhlner.aitutor.user.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_username", columnList = "username"),
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_provider_id", columnList = "provider,provider_id")
    ]
)
class UserEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "username", nullable = false, unique = true, length = 32)
    var username: String,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = true, length = 255)
    var passwordHash: String? = null,

    @Column(name = "first_name", nullable = true, length = 64)
    var firstName: String? = null,

    @Column(name = "last_name", nullable = true, length = 64)
    var lastName: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        indexes = [Index(name = "idx_user_roles_user_id", columnList = "user_id")]
    )
    @Column(name = "role", length = 32)
    @Enumerated(EnumType.STRING)
    var roles: MutableSet<UserRole> = mutableSetOf(UserRole.USER),

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "locked", nullable = false)
    var locked: Boolean = false,

    @Column(name = "account_expired", nullable = false)
    var accountExpired: Boolean = false,

    @Column(name = "credentials_expired", nullable = false)
    var credentialsExpired: Boolean = false,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    var provider: AuthProvider = AuthProvider.CREDENTIALS,

    @Column(name = "provider_id", nullable = true, length = 255)
    var providerId: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,

    @Column(name = "last_login_at", nullable = true)
    var lastLoginAt: Instant? = null,

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: Instant? = null
)
