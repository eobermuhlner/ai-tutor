package ch.obermuhlner.aitutor.payment.service

import ch.obermuhlner.aitutor.user.domain.UserEntity
import com.stripe.model.Subscription
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session
import java.util.UUID

/**
 * Interface for Stripe payment operations.
 * Allows for different implementations (real Stripe API vs no-op) based on configuration.
 */
interface StripeServiceInterface {
    fun getOrCreateCustomer(user: UserEntity): String
    fun createCheckoutSession(userId: UUID, userEmail: String, customerId: String): Session
    fun createBillingPortalSession(customerId: String): PortalSession
    
    // Add cancel subscription method
    fun cancelSubscription(subscriptionId: String): Subscription
}
