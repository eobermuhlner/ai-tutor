package ch.obermuhlner.aitutor.payment.service

import ch.obermuhlner.aitutor.payment.config.StripeConfig
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import com.stripe.model.Customer
import com.stripe.model.checkout.Session
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.param.CustomerCreateParams
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.billingportal.SessionCreateParams as PortalSessionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeService(
    private val stripeConfig: StripeConfig,
    private val userRepository: UserRepository
) : StripeServiceInterface {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun getOrCreateCustomer(user: UserEntity): String {
        // Return existing customer ID if available
        user.stripeCustomerId?.let { return it }

        // Create new Stripe customer
        logger.info("Creating Stripe customer for user ${user.id}")
        val params = CustomerCreateParams.builder()
            .setEmail(user.email)
            .setName("${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.username })
            .putMetadata("user_id", user.id.toString())
            .build()

        val customer = Customer.create(params)

        // Save customer ID to user entity
        user.stripeCustomerId = customer.id
        userRepository.save(user)

        logger.info("Created Stripe customer ${customer.id} for user ${user.id}")
        return customer.id
    }

    override fun createCheckoutSession(userId: UUID, userEmail: String, customerId: String): Session {
        logger.info("Creating checkout session for user $userId")

        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setSuccessUrl(stripeConfig.successUrl)
            .setCancelUrl(stripeConfig.cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(stripeConfig.priceIdSubscription10)
                    .setQuantity(1L)
                    .build()
            )
            .putMetadata("user_id", userId.toString())
            .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("user_id", userId.toString())
                    .build()
            )
            .build()

        val session = Session.create(params)
        logger.info("Created checkout session ${session.id} for user $userId")
        return session
    }

    override fun createBillingPortalSession(customerId: String): PortalSession {
        logger.info("Creating billing portal session for customer $customerId")

        val params = PortalSessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl(stripeConfig.successUrl)
            .build()

        val session = PortalSession.create(params)
        logger.info("Created billing portal session ${session.id}")
        return session
    }
}
