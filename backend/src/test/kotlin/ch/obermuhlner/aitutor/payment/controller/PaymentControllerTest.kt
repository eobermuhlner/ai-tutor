package ch.obermuhlner.aitutor.payment.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.payment.service.StripeServiceInterface
import ch.obermuhlner.aitutor.payment.service.SubscriptionService
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.domain.UserEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import com.stripe.model.checkout.Session as StripeSession
import com.stripe.model.billingportal.Session as PortalSession
import java.util.UUID

class PaymentControllerTest {
    private lateinit var authorizationService: AuthorizationService
    private lateinit var stripeService: StripeServiceInterface
    private lateinit var subscriptionService: SubscriptionService
    private lateinit var userRepository: UserRepository
    private lateinit var controller: PaymentController

    @BeforeEach
    fun setup() {
        authorizationService = mockk()
        stripeService = mockk()
        subscriptionService = mockk()
        userRepository = mockk()
        controller = PaymentController(
            authorizationService = authorizationService,
            stripeService = stripeService,
            subscriptionService = subscriptionService,
            userRepository = userRepository
        )
    }

    @Test
    fun `createCheckoutSession should return checkout session response`() {
        val userId = UUID.randomUUID()
        val user = mockk<UserEntity>()
        val stripeSession = mockk<StripeSession>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.of(user)
        every { user.email } returns "test@example.com"
        every { stripeService.getOrCreateCustomer(user) } returns "cus_123"
        every { stripeService.createCheckoutSession(userId, "test@example.com", "cus_123") } returns stripeSession
        every { stripeSession.id } returns "cs_123"
        every { stripeSession.url } returns "https://checkout.stripe.com/cs_123"

        val result = controller.createCheckoutSession()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("cs_123", result.body?.sessionId)
        assertEquals("https://checkout.stripe.com/cs_123", result.body?.url)
        verify { stripeService.getOrCreateCustomer(user) }
        verify { stripeService.createCheckoutSession(userId, "test@example.com", "cus_123") }
    }

    @Test
    fun `createCheckoutSession should throw exception when user not found`() {
        val userId = UUID.randomUUID()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            controller.createCheckoutSession()
        }
    }

    @Test
    fun `createBillingPortalSession should return billing portal session response`() {
        val userId = UUID.randomUUID()
        val user = mockk<UserEntity>()
        val stripeSession = mockk<PortalSession>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.of(user)
        every { user.stripeCustomerId } returns "cus_123"
        every { stripeService.createBillingPortalSession("cus_123") } returns stripeSession
        every { stripeSession.url } returns "https://billing.stripe.com/session_123"

        val result = controller.createBillingPortalSession()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("https://billing.stripe.com/session_123", result.body?.url)
        verify { stripeService.createBillingPortalSession("cus_123") }
    }

    @Test
    fun `createBillingPortalSession should throw exception when no stripe customer ID exists`() {
        val userId = UUID.randomUUID()
        val user = mockk<UserEntity>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.of(user)
        every { user.stripeCustomerId } returns null

        assertThrows(IllegalStateException::class.java) {
            controller.createBillingPortalSession()
        }
    }

    @Test
    fun `createBillingPortalSession should handle configuration error`() {
        val userId = UUID.randomUUID()
        val user = mockk<UserEntity>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.of(user)
        every { user.stripeCustomerId } returns "cus_123"
        every { stripeService.createBillingPortalSession("cus_123") } throws RuntimeException("configuration error")

        assertThrows(IllegalStateException::class.java) {
            controller.createBillingPortalSession()
        }
    }

    @Test
    fun `createBillingPortalSession should handle general error`() {
        val userId = UUID.randomUUID()
        val user = mockk<UserEntity>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { userRepository.findById(userId) } returns java.util.Optional.of(user)
        every { user.stripeCustomerId } returns "cus_123"
        every { stripeService.createBillingPortalSession("cus_123") } throws RuntimeException("general error")

        assertThrows(IllegalStateException::class.java) {
            controller.createBillingPortalSession()
        }
    }

    @Test
    fun `cancelSubscription should cancel active subscription`() {
        val userId = UUID.randomUUID()
        val subscriptionStatus = mockk<ch.obermuhlner.aitutor.payment.service.SubscriptionStatusResponse>()
        val canceledSubscription = mockk<com.stripe.model.Subscription>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { subscriptionService.getSubscriptionStatus(userId) } returns subscriptionStatus
        every { subscriptionStatus.hasActiveSubscription } returns true
        every { subscriptionStatus.stripeSubscriptionId } returns "sub_123"
        every { stripeService.cancelSubscription("sub_123") } returns canceledSubscription
        every { canceledSubscription.id } returns "sub_123"
        every { canceledSubscription.status } returns "canceled"
        every { canceledSubscription.canceledAt } returns null
        every { canceledSubscription.cancelAtPeriodEnd } returns false
        every { canceledSubscription.currentPeriodEnd } returns null

        val result = controller.cancelSubscription()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("sub_123", result.body?.subscriptionId)
        assertEquals("canceled", result.body?.status)
        verify { stripeService.cancelSubscription("sub_123") }
    }

    @Test
    fun `cancelSubscription should throw exception when no active subscription exists`() {
        val userId = UUID.randomUUID()
        val subscriptionStatus = mockk<ch.obermuhlner.aitutor.payment.service.SubscriptionStatusResponse>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { subscriptionService.getSubscriptionStatus(userId) } returns subscriptionStatus
        every { subscriptionStatus.hasActiveSubscription } returns false
        every { subscriptionStatus.stripeSubscriptionId } returns null

        assertThrows(IllegalStateException::class.java) {
            controller.cancelSubscription()
        }
    }

    @Test
    fun `getSubscriptionStatus should return subscription status`() {
        val userId = UUID.randomUUID()
        val subscriptionStatus = mockk<ch.obermuhlner.aitutor.payment.service.SubscriptionStatusResponse>()
        
        every { authorizationService.getCurrentUserId() } returns userId
        every { subscriptionService.getSubscriptionStatus(userId) } returns subscriptionStatus

        val result = controller.getSubscriptionStatus()

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(subscriptionStatus, result.body)
        verify { subscriptionService.getSubscriptionStatus(userId) }
    }
}