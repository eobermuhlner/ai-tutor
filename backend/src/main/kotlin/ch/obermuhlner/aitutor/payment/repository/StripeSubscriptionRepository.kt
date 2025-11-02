package ch.obermuhlner.aitutor.payment.repository

import ch.obermuhlner.aitutor.payment.domain.StripeSubscriptionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StripeSubscriptionRepository : JpaRepository<StripeSubscriptionEntity, UUID> {
    fun findByUserId(userId: UUID): StripeSubscriptionEntity?
    fun findByStripeSubscriptionId(stripeSubscriptionId: String): StripeSubscriptionEntity?
    fun findByUserIdAndStatus(userId: UUID, status: String): StripeSubscriptionEntity?
}
