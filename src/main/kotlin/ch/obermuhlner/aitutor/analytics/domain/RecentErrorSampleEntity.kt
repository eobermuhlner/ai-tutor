package ch.obermuhlner.aitutor.analytics.domain

import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp

@Entity
@Table(name = "recent_error_samples")
class RecentErrorSampleEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 32)
    val lang: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 32)
    val errorType: ErrorType,

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    val severity: ErrorSeverity,

    @Column(name = "message_id", nullable = false)
    val messageId: UUID,

    @Column(name = "error_span", length = 256)
    val errorSpan: String?,

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant? = null
)
