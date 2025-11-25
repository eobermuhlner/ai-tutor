package ch.obermuhlner.aitutor.payment.repository

import ch.obermuhlner.aitutor.payment.domain.PaymentEventEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentEventRepository : JpaRepository<PaymentEventEntity, UUID> {
    fun existsByStripeEventId(stripeEventId: String): Boolean
}
