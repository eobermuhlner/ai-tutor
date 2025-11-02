package ch.obermuhlner.aitutor.payment.service

import ch.obermuhlner.aitutor.user.domain.UserEntity
import com.stripe.model.checkout.Session
import com.stripe.model.billingportal.Session as PortalSession
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * No-op implementation of StripeService used when Stripe integration is disabled.
 * Logs all operations instead of calling Stripe API.
 */
@Service
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpStripeService : StripeServiceInterface {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getOrCreateCustomer(user: UserEntity): String {
        logger.warn("Stripe is disabled. Skipping customer creation for user ${user.id}")
        return "cus_disabled_${user.id}"
    }

    override fun createCheckoutSession(userId: UUID, userEmail: String, customerId: String): Session {
        logger.warn("Stripe is disabled. Cannot create checkout session for user $userId")
        throw UnsupportedOperationException("Stripe payment integration is not enabled. Please configure Stripe or use the 'stripe' profile.")
    }

    override fun createBillingPortalSession(customerId: String): PortalSession {
        logger.warn("Stripe is disabled. Cannot create billing portal session for customer $customerId")
        throw UnsupportedOperationException("Stripe billing portal is not enabled. Please configure Stripe or use the 'stripe' profile.")
    }
}
