package ch.obermuhlner.aitutor.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "payment_events",
    indexes = [
        Index(name = "idx_payment_events_user_id", columnList = "user_id"),
        Index(name = "idx_payment_events_event_type", columnList = "event_type"),
        Index(name = "idx_payment_events_processed_at", columnList = "processed_at")
    ]
)
class PaymentEventEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "stripe_event_id", nullable = false, unique = true, length = 255)
    val stripeEventId: String,

    @Column(name = "event_type", nullable = false, length = 64)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant = Instant.now()
)
