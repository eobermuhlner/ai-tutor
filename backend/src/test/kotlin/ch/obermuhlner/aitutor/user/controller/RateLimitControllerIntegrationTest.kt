package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.LoginResponse
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.user.dto.UpdateUserSubscriptionPlanRequest
import ch.obermuhlner.aitutor.user.dto.RateLimitStatusResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class RateLimitControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getRateLimitStatus endpoint`() {
        // Test getting rate limit status for the current user
        val response = restTemplate.getForEntity(
            baseUrl("/rate-limits/status"),
            RateLimitStatusResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.availableTokens).isGreaterThanOrEqualTo(0)
        Assertions.assertThat(response.body!!.dailyLimit).isGreaterThan(0)
        Assertions.assertThat(response.body!!.hourlyLimit).isGreaterThan(0)
        Assertions.assertThat(response.body!!.subscriptionPlan).isNotNull()
    }

    @Test
    fun `test updateUserSubscriptionPlan endpoint`() {
        // Test updating user subscription plan
        val updateRequest = UpdateUserSubscriptionPlanRequest(
            subscriptionPlan = "FREE_BYOK"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(updateRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/rate-limits/subscription-plan"),
            HttpMethod.PATCH,
            entity,
            RateLimitStatusResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.subscriptionPlan).isEqualTo("FREE_BYOK")
    }
}