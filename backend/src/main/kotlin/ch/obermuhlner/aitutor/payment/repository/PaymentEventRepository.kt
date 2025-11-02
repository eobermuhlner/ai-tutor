package ch.obermuhlner.aitutor.payment.repository

import ch.obermuhlner.aitutor.payment.domain.PaymentEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PaymentEventRepository : JpaRepository<PaymentEventEntity, UUID> {
    fun existsByStripeEventId(stripeEventId: String): Boolean
}
