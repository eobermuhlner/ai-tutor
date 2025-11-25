package ch.obermuhlner.aitutor.payment.controller

import ch.obermuhlner.aitutor.payment.config.StripeConfig
import ch.obermuhlner.aitutor.payment.domain.PaymentEventEntity
import ch.obermuhlner.aitutor.payment.repository.PaymentEventRepository
import ch.obermuhlner.aitutor.payment.service.SubscriptionService
import ch.obermuhlner.aitutor.user.repository.UserRepository
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
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Stripe webhook endpoints (unauthenticated, signature-validated)")
@ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
class StripeWebhookController(
    private val stripeConfig: StripeConfig,
    private val subscriptionService: SubscriptionService,
    private val paymentEventRepository: PaymentEventRepository,
    private val userRepository: UserRepository
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
        logger.info("=== WEBHOOK RECEIVED === Event size: ${payload.length} chars")

        // 1. Verify webhook signature - Critical security step
        val event: Event
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeConfig.webhookSecret)
            logger.info("=== WEBHOOK SIGNATURE VALID === Event: ${event.type}, ID: ${event.id}")
        } catch (e: SignatureVerificationException) {
            logger.error("=== INVALID SIGNATURE === Event ID: unknown", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature")
        } catch (e: Exception) {
            logger.error("=== WEBHOOK CONSTRUCTION ERROR ===", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error: ${e.message}")
        }

        // 2. Check for duplicate events using idempotency
        if (paymentEventRepository.existsByStripeEventId(event.id)) {
            logger.warn("Duplicate event received: ${event.id}, skipping processing")
            return ResponseEntity.ok("Duplicate event")
        }

        logger.info("Processing Stripe event: ${event.type} (${event.id}) at ${Instant.now()}")

        // 3. Process the event based on type with proper validation
        return try {
            val processed = processEvent(event)
            
            if (processed) {
                // 4. Log successful processing
                val userId = extractUserIdIfAvailable(event)
                if (userId != null) {
                    paymentEventRepository.save(
                        PaymentEventEntity(
                            userId = userId,
                            stripeEventId = event.id,
                            eventType = event.type,
                            payload = event.toJson(),
                            processedAt = Instant.now()
                        )
                    )
                    logger.info("Successfully processed event ${event.id}")
                }
            } else {
                logger.warn("Event ${event.id} was not processed (handled as no-op)")
            }
            
            ResponseEntity.ok("Success")
        } catch (e: Exception) {
            logger.error("Failed to process Stripe event ${event.id}", e)
            
            // 5. Log failed processing for debugging
            val userId = extractUserIdIfAvailable(event)
            if (userId != null) {
                // Log the event with current timestamp even if processing failed
                paymentEventRepository.save(
                    PaymentEventEntity(
                        userId = userId,
                        stripeEventId = event.id,
                        eventType = event.type,
                        payload = event.toJson(),
                        processedAt = Instant.now()
                    )
                )
            }
            
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Event processing failed")
        }
    }

    private fun processEvent(event: Event): Boolean {
        return when (event.type) {
            "customer.subscription.created",
            "customer.subscription.updated" -> {
                handleSubscriptionEvent(event, "CREATED/UPDATED")
            }
            
            "customer.subscription.deleted" -> {
                handleSubscriptionEvent(event, "DELETED")
            }
            
            "invoice.payment_succeeded" -> {
                handleInvoicePaymentSucceeded(event)
            }
            
            "invoice.payment_failed" -> {
                handleInvoicePaymentFailed(event)
            }
            
            "customer.subscription.trial_will_end" -> {
                handleTrialWillEnd(event)
            }
            
            "customer.subscription.paused",
            "customer.subscription.resumed" -> {
                handleSubscriptionPausedOrResumed(event)
            }
            
            else -> {
                logger.info("Unhandled event type: ${event.type}, ID: ${event.id}")
                false
            }
        }
    }

    private fun handleSubscriptionEvent(event: Event, eventType: String): Boolean {
        logger.info("Handling subscription $eventType event: ${event.id}")
        
        // Extract subscription from event data with proper validation
        val subscription = extractSubscriptionFromEvent(event) ?: run {
            logger.error("Could not extract subscription from ${eventType} event ${event.id}")
            return false
        }

        // Validate required fields exist
        if (!validateSubscriptionData(subscription)) {
            logger.error("Invalid subscription data in $eventType event ${event.id}")
            return false
        }

        // Extract and validate user ID
        val userId = extractUserIdFromSubscription(subscription) ?: run {
            logger.error("Could not extract user_id from subscription in $eventType event ${event.id}")
            return false
        }

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User $userId does not exist for subscription event ${event.id}")
            return false
        }

        logger.info("Processing subscription $eventType for user $userId, subscription ${subscription.id}")

        when (eventType) {
            "DELETED" -> {
                subscriptionService.deactivateSubscription(userId)
                logger.info("Successfully deactivated subscription for user $userId")
            }
            else -> {
                // For created/updated, check if status permits processing
                if (subscription.status in listOf("active", "trialing")) {
                    subscriptionService.activateSubscription(userId, subscription)
                    logger.info("Successfully activated subscription for user $userId")
                } else {
                    logger.info("Subscription ${subscription.id} has status '${subscription.status}', not processing for user $userId")
                }
            }
        }
        
        return true
    }

    private fun handleInvoicePaymentSucceeded(event: Event): Boolean {
        logger.info("Handling invoice payment succeeded event: ${event.id}")
        
        // This event confirms payment, could be used to update user status
        // For now, just log it - the subscription events should handle the main logic
        logger.info("Invoice payment succeeded: ${event.id}")
        return false // Not a main processing event
    }

    private fun handleInvoicePaymentFailed(event: Event): Boolean {
        logger.warn("Payment failed for event ${event.id}")
        // Could implement email notifications or other handling here
        return false
    }

    private fun handleTrialWillEnd(event: Event): Boolean {
        logger.info("Trial will end for event ${event.id}")
        // Could implement trial end notifications
        return false
    }

    private fun handleSubscriptionPausedOrResumed(event: Event): Boolean {
        logger.info("${event.type} for event ${event.id}")
        // Could implement paused/resumed handling
        return false
    }

    private fun extractSubscriptionFromEvent(event: Event): Subscription? {
        // Primary approach: try the dataObjectDeserializer
        var subscription = try {
            val deserializedObject = event.dataObjectDeserializer.getObject().orElse(null)
            if (deserializedObject is Subscription) {
                deserializedObject
            } else {
                logger.debug("Event ${event.id} deserializer returned non-Subscription: ${deserializedObject?.javaClass?.name}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to deserialize subscription via deserializer for event ${event.id}", e)
            null
        }

        // Fallback: try to access from event data object
        if (subscription == null) {
            try {
                val eventData = event.data
                if (eventData != null) {
                    @Suppress("DEPRECATION")
                    val dataObject = eventData.`object`
                    if (dataObject is Subscription) {
                        subscription = dataObject
                        logger.debug("Successfully extracted subscription from event.data.`object` for event ${event.id}")
                    } else {
                        logger.debug("Event ${event.id} data.`object` is not a Subscription: ${dataObject?.javaClass?.name}")
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to extract subscription from event data for event ${event.id}", e)
            }
        }

        return subscription
    }

    private fun validateSubscriptionData(subscription: Subscription): Boolean {
        return subscription.id != null && 
               subscription.status != null &&
               subscription.metadata != null
    }

    private fun extractUserIdFromSubscription(subscription: Subscription): UUID? {
        val userIdString = subscription.metadata?.get("user_id")
        return try {
            userIdString?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            logger.error("Invalid user_id in subscription metadata: $userIdString", e)
            null
        }
    }

    private fun extractUserIdIfAvailable(event: Event): UUID? {
        return try {
            val subscription = extractSubscriptionFromEvent(event)
            subscription?.let { extractUserIdFromSubscription(it) }
        } catch (e: Exception) {
            logger.debug("Could not extract user ID from event ${event.id}", e)
            null
        }
    }
}
