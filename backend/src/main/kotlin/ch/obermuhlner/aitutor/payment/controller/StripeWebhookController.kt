package ch.obermuhlner.aitutor.payment.controller

import ch.obermuhlner.aitutor.payment.config.StripeConfig
import ch.obermuhlner.aitutor.payment.domain.PaymentEventEntity
import ch.obermuhlner.aitutor.payment.repository.PaymentEventRepository
import ch.obermuhlner.aitutor.payment.service.SubscriptionService
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Stripe webhook endpoints (unauthenticated, signature-validated)")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeWebhookController(
    private val stripeConfig: StripeConfig,
    private val subscriptionService: SubscriptionService,
    private val paymentEventRepository: PaymentEventRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/stripe")
    @Operation(
        summary = "Stripe webhook handler",
        description = "Handles Stripe webhook events for subscription lifecycle management"
    )
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signatureHeader: String
    ): ResponseEntity<String> {
        val event: Event

        try {
            // Verify webhook signature
            event = Webhook.constructEvent(payload, signatureHeader, stripeConfig.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.error("Invalid Stripe webhook signature", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        } catch (e: Exception) {
            logger.error("Error parsing Stripe webhook", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: ${e.message}")
        }

        // Check for duplicate events (idempotency)
        if (paymentEventRepository.existsByStripeEventId(event.id)) {
            logger.info("Duplicate event received: ${event.id}, skipping")
            return ResponseEntity.ok("Duplicate event")
        }

        logger.info("Processing Stripe event: ${event.type} (${event.id})")

        try {
            // Handle event based on type
            when (event.type) {
                "customer.subscription.created",
                "customer.subscription.updated" -> {
                    val subscription = event.dataObjectDeserializer.getObject().orElse(null) as? Subscription
                    if (subscription != null) {
                        handleSubscriptionCreatedOrUpdated(subscription, event)
                    } else {
                        logger.error("Failed to deserialize subscription from event ${event.id}")
                    }
                }

                "customer.subscription.deleted" -> {
                    val subscription = event.dataObjectDeserializer.getObject().orElse(null) as? Subscription
                    if (subscription != null) {
                        handleSubscriptionDeleted(subscription, event)
                    } else {
                        logger.error("Failed to deserialize subscription from event ${event.id}")
                    }
                }

                "invoice.payment_failed" -> {
                    logger.warn("Payment failed for event ${event.id}")
                    // Could implement email notifications or other handling here
                }

                else -> {
                    logger.info("Unhandled event type: ${event.type}")
                }
            }

            return ResponseEntity.ok("Success")
        } catch (e: Exception) {
            logger.error("Error processing Stripe event ${event.id}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing event: ${e.message}")
        }
    }

    private fun handleSubscriptionCreatedOrUpdated(subscription: Subscription, event: Event) {
        val userId = getUserIdFromSubscription(subscription)
        if (userId != null) {
            subscriptionService.activateSubscription(userId, subscription)

            // Log event
            paymentEventRepository.save(
                PaymentEventEntity(
                    userId = userId,
                    stripeEventId = event.id,
                    eventType = event.type,
                    payload = event.toJson()
                )
            )

            logger.info("Activated subscription for user $userId")
        } else {
            logger.error("Could not extract user_id from subscription ${subscription.id}")
        }
    }

    private fun handleSubscriptionDeleted(subscription: Subscription, event: Event) {
        val userId = getUserIdFromSubscription(subscription)
        if (userId != null) {
            subscriptionService.deactivateSubscription(userId)

            // Log event
            paymentEventRepository.save(
                PaymentEventEntity(
                    userId = userId,
                    stripeEventId = event.id,
                    eventType = event.type,
                    payload = event.toJson()
                )
            )

            logger.info("Deactivated subscription for user $userId")
        } else {
            logger.error("Could not extract user_id from subscription ${subscription.id}")
        }
    }

    private fun getUserIdFromSubscription(subscription: Subscription): UUID? {
        val userIdString = subscription.metadata["user_id"]
        return try {
            UUID.fromString(userIdString)
        } catch (e: Exception) {
            logger.error("Invalid user_id in subscription metadata: $userIdString", e)
            null
        }
    }
}
