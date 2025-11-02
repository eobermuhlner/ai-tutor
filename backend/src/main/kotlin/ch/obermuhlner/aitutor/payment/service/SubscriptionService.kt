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
        logger.info("Activating subscription for user $userId with Stripe subscription ${stripeSubscription.id}")

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Create or update subscription entity
        val subscriptionEntity = stripeSubscriptionRepository.findByUserId(userId)
            ?: StripeSubscriptionEntity(
                userId = userId,
                stripeCustomerId = stripeSubscription.customer,
                stripeSubscriptionId = stripeSubscription.id,
                stripePriceId = stripeSubscription.items.data.firstOrNull()?.price?.id ?: "",
                status = stripeSubscription.status,
                currentPeriodStart = Instant.ofEpochSecond(stripeSubscription.currentPeriodStart),
                currentPeriodEnd = Instant.ofEpochSecond(stripeSubscription.currentPeriodEnd),
                cancelAtPeriodEnd = stripeSubscription.cancelAtPeriodEnd
            )

        subscriptionEntity.status = stripeSubscription.status
        subscriptionEntity.currentPeriodStart = Instant.ofEpochSecond(stripeSubscription.currentPeriodStart)
        subscriptionEntity.currentPeriodEnd = Instant.ofEpochSecond(stripeSubscription.currentPeriodEnd)
        subscriptionEntity.cancelAtPeriodEnd = stripeSubscription.cancelAtPeriodEnd

        stripeSubscriptionRepository.save(subscriptionEntity)

        // Update user subscription plan
        val newPlan = SubscriptionPlan.SUBSCRIPTION_10
        if (user.subscriptionPlan != newPlan) {
            user.subscriptionPlan = newPlan
            userRepository.save(user)

            // Reset rate limits to apply new plan limits
            rateLimitingService.resetRateLimit(userId)
            logger.info("Updated user $userId subscription plan to $newPlan")
        }
    }

    @Transactional
    fun deactivateSubscription(userId: UUID) {
        logger.info("Deactivating subscription for user $userId")

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found: $userId")
        }

        // Update subscription entity status
        stripeSubscriptionRepository.findByUserId(userId)?.let { subscription ->
            subscription.status = "canceled"
            stripeSubscriptionRepository.save(subscription)
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
