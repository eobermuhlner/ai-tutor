package ch.obermuhlner.aitutor.payment.controller

import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class PaymentControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getSubscriptionStatus endpoint`() {
        // Test getting subscription status for the current user
        val response = restTemplate.exchange(
            baseUrl("/payment/subscription-status"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            ch.obermuhlner.aitutor.payment.service.SubscriptionStatusResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        // Verify that the response has the expected fields
        Assertions.assertThat(response.body!!.hasActiveSubscription).isNotNull()
    }

}