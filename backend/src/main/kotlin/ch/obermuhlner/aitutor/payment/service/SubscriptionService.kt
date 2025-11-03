package ch.obermuhlner.aitutor.payment.service

import ch.obermuhlner.aitutor.payment.domain.StripeSubscriptionEntity
import ch.obermuhlner.aitutor.payment.repository.StripeSubscriptionRepository
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import com.stripe.model.Subscription
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SubscriptionService(
    private val stripeSubscriptionRepository: StripeSubscriptionRepository,
    private val userRepository: UserRepository,
    private val rateLimitingService: RateLimitingService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun activateSubscription(userId: UUID, stripeSubscription: Subscription) {
        logger.info("Activating subscription for user $userId with Stripe subscription ${stripeSubscription.id}, status: ${stripeSubscription.status}")

        val user = userRepository.findById(userId).orElseThrow {
            logger.error("User not found: $userId when activating subscription")
            IllegalArgumentException("User not found: $userId")
        }

        logger.info("Current user subscription plan: ${user.subscriptionPlan}, Stripe subscription status: ${stripeSubscription.status}")

        // Create or update subscription entity
        val subscriptionEntity = stripeSubscriptionRepository.findByUserId(userId)
            ?: StripeSubscriptionEntity(
                userId = userId,
                stripeCustomerId = stripeSubscription.customer,
                stripeSubscriptionId = stripeSubscription.id,
                stripePriceId = stripeSubscription.items.data.firstOrNull()?.price?.id ?: "",
                status = stripeSubscription.status,
                currentPeriodStart = if (stripeSubscription.currentPeriodStart != null) Instant.ofEpochSecond(stripeSubscription.currentPeriodStart) else Instant.now(),
                currentPeriodEnd = if (stripeSubscription.currentPeriodEnd != null) Instant.ofEpochSecond(stripeSubscription.currentPeriodEnd) else Instant.now().plusSeconds(30L * 24 * 60 * 60), // 30 days default
                cancelAtPeriodEnd = stripeSubscription.cancelAtPeriodEnd
            )

        subscriptionEntity.status = stripeSubscription.status
        subscriptionEntity.currentPeriodStart = if (stripeSubscription.currentPeriodStart != null) Instant.ofEpochSecond(stripeSubscription.currentPeriodStart) else Instant.now()
        subscriptionEntity.currentPeriodEnd = if (stripeSubscription.currentPeriodEnd != null) Instant.ofEpochSecond(stripeSubscription.currentPeriodEnd) else Instant.now().plusSeconds(30L * 24 * 60 * 60) // 30 days default
        subscriptionEntity.cancelAtPeriodEnd = stripeSubscription.cancelAtPeriodEnd

        stripeSubscriptionRepository.save(subscriptionEntity)
        logger.info("Saved subscription entity for user $userId")

        // Update user subscription plan
        val newPlan = SubscriptionPlan.SUBSCRIPTION_10
        logger.info("Setting user $userId subscription plan to $newPlan (was ${user.subscriptionPlan})")
        
        if (user.subscriptionPlan != newPlan) {
            logger.info("User $userId plan changed from ${user.subscriptionPlan} to $newPlan, saving update")
            user.subscriptionPlan = newPlan
            userRepository.save(user)

            // Reset rate limits to apply new plan limits
            rateLimitingService.resetRateLimit(userId)
            logger.info("Successfully updated user $userId subscription plan to $newPlan and reset rate limits")
        } else {
            logger.info("User $userId already has plan $newPlan, no update needed")
        }
    }

    @Transactional
    fun deactivateSubscription(userId: UUID) {
        logger.info("Deactivating subscription for user $userId")

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Delete the subscription entity to avoid stale references
        stripeSubscriptionRepository.findByUserId(userId)?.let { subscription ->
            stripeSubscriptionRepository.delete(subscription)
            logger.info("Deleted subscription record for user $userId")
        }

        // Downgrade user to FREE plan
        val newPlan = SubscriptionPlan.FREE
        if (user.subscriptionPlan != newPlan) {
            user.subscriptionPlan = newPlan
            userRepository.save(user)

            // Reset rate limits to apply new plan limits
            rateLimitingService.resetRateLimit(userId)
            logger.info("Downgraded user $userId subscription plan to $newPlan")
        }
    }

    fun getSubscriptionStatus(userId: UUID): SubscriptionStatusResponse {
        val subscription = stripeSubscriptionRepository.findByUserId(userId)

        return if (subscription != null && subscription.status == "active") {
            SubscriptionStatusResponse(
                hasActiveSubscription = true,
                stripeSubscriptionId = subscription.stripeSubscriptionId,
                currentPeriodEnd = subscription.currentPeriodEnd,
                cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
                status = subscription.status
            )
        } else {
            SubscriptionStatusResponse(
                hasActiveSubscription = false,
                stripeSubscriptionId = null,
                currentPeriodEnd = null,
                cancelAtPeriodEnd = false,
                status = "none"
            )
        }
    }
}

data class SubscriptionStatusResponse(
    val hasActiveSubscription: Boolean,
    val stripeSubscriptionId: String?,
    val currentPeriodEnd: Instant?,
    val cancelAtPeriodEnd: Boolean,
    val status: String
)
