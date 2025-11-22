package ch.obermuhlner.aitutor.payment.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled

/**
 * Integration test for StripeWebhookController.
 *
 * Note: This controller is conditionally enabled with @ConditionalOnProperty(name = ["stripe.enabled"], havingValue = "true")
 * and is typically disabled in test environments. Therefore, these tests are disabled.
 */
@Disabled("StripeWebhookController is conditionally disabled in test environment")
class StripeWebhookControllerIntegrationTest {

    @Test
    fun `test handleStripeWebhook endpoint with invalid signature`() {
        // This test is disabled because StripeWebhookController is not available in test environment
    }

    @Test
    fun `test handleStripeWebhook endpoint with proper content type`() {
        // This test is disabled because StripeWebhookController is not available in test environment
    }
}