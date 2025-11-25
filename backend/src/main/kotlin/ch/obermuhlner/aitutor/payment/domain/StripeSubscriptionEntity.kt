package ch.obermuhlner.aitutor.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(
    name = "stripe_subscriptions",
    indexes = [
        Index(name = "idx_stripe_subscriptions_user_id", columnList = "user_id"),
        Index(name = "idx_stripe_subscriptions_status", columnList = "status")
    ]
)
class StripeSubscriptionEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "stripe_customer_id", nullable = false, length = 255)
    val stripeCustomerId: String,

    @Column(name = "stripe_subscription_id", nullable = false, unique = true, length = 255)
    val stripeSubscriptionId: String,

    @Column(name = "stripe_price_id", nullable = false, length = 255)
    val stripePriceId: String,

    @Column(name = "status", nullable = false, length = 32)
    var status: String,

    @Column(name = "current_period_start", nullable = false)
    var currentPeriodStart: Instant,

    @Column(name = "current_period_end", nullable = false)
    var currentPeriodEnd: Instant,

    @Column(name = "cancel_at_period_end", nullable = false)
    var cancelAtPeriodEnd: Boolean = false,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
