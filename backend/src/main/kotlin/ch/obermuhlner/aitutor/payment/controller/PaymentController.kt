package ch.obermuhlner.aitutor.payment.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.payment.service.StripeServiceInterface
import ch.obermuhlner.aitutor.payment.service.SubscriptionService
import ch.obermuhlner.aitutor.payment.service.SubscriptionStatusResponse
import ch.obermuhlner.aitutor.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Payment", description = "Stripe payment and subscription management endpoints")
class PaymentController(
    private val authorizationService: AuthorizationService,
    private val stripeService: StripeServiceInterface,
    private val subscriptionService: SubscriptionService,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/checkout-session")
    @Operation(
        summary = "Create Stripe Checkout session",
        description = "Creates a Stripe Checkout session for subscribing to SUBSCRIPTION_10 plan"
    )
    fun createCheckoutSession(): ResponseEntity<CheckoutSessionResponse> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Get or create Stripe customer
        val customerId = stripeService.getOrCreateCustomer(user)

        // Create checkout session
        val session = stripeService.createCheckoutSession(userId, user.email, customerId)

        return ResponseEntity.ok(
            CheckoutSessionResponse(
                sessionId = session.id,
                url = session.url
            )
        )
    }

    @PostMapping("/billing-portal")
    @Operation(
        summary = "Create Stripe Billing Portal session",
        description = "Creates a Stripe Billing Portal session for managing subscription"
    )
    fun createBillingPortalSession(): ResponseEntity<BillingPortalSessionResponse> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Require existing Stripe customer
        val customerId = user.stripeCustomerId
            ?: throw IllegalStateException("User does not have a Stripe customer ID")

        try {
            // Create billing portal session
            val session = stripeService.createBillingPortalSession(customerId)

            return ResponseEntity.ok(
                BillingPortalSessionResponse(
                    url = session.url
                )
            )
        } catch (e: RuntimeException) {
            logger.error("Failed to create billing portal session for user $userId", e)
            // Provide more specific guidance for this common configuration issue
            if (e.message?.contains("configuration") == true) {
                throw IllegalStateException("Stripe Customer Portal is not configured. Please configure it in the Stripe Dashboard at https://dashboard.stripe.com/test/settings/billing/portal")
            }
            throw IllegalStateException("Unable to create billing portal session: ${e.message}")
        }
    }

    @PostMapping("/cancel-subscription")
    @Operation(
        summary = "Cancel active subscription",
        description = "Cancels the user's active subscription in Stripe"
    )
    fun cancelSubscription(): ResponseEntity<CancelSubscriptionResponse> {
        val userId = authorizationService.getCurrentUserId()

        // Get the current subscription to cancel
        val subscriptionStatus = subscriptionService.getSubscriptionStatus(userId)
        if (!subscriptionStatus.hasActiveSubscription || subscriptionStatus.stripeSubscriptionId == null) {
            throw IllegalStateException("User does not have an active subscription to cancel")
        }

        // Cancel the subscription in Stripe
        val canceledSubscription = stripeService.cancelSubscription(subscriptionStatus.stripeSubscriptionId)

        return ResponseEntity.ok(
            CancelSubscriptionResponse(
                subscriptionId = canceledSubscription.id,
                status = canceledSubscription.status,
                canceledAt = canceledSubscription.canceledAt?.let { java.time.Instant.ofEpochSecond(it) },
                cancelAtPeriodEnd = canceledSubscription.cancelAtPeriodEnd,
                currentPeriodEnd = canceledSubscription.currentPeriodEnd?.let { java.time.Instant.ofEpochSecond(it) }
            )
        )
    }

    @GetMapping("/subscription-status")
    @Operation(
        summary = "Get subscription status",
        description = "Retrieves the current subscription status for the authenticated user"
    )
    fun getSubscriptionStatus(): ResponseEntity<SubscriptionStatusResponse> {
        val userId = authorizationService.getCurrentUserId()
        val status = subscriptionService.getSubscriptionStatus(userId)
        return ResponseEntity.ok(status)
    }
}

data class CheckoutSessionResponse(
    val sessionId: String,
    val url: String
)

data class BillingPortalSessionResponse(
    val url: String
)

data class CancelSubscriptionResponse(
    val subscriptionId: String,
    val status: String,
    val canceledAt: java.time.Instant?,
    val cancelAtPeriodEnd: Boolean,
    val currentPeriodEnd: java.time.Instant?
)
