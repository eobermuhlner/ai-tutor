package ch.obermuhlner.aitutor.admin.controller

import ch.obermuhlner.aitutor.admin.dto.UpdateSubscriptionPlanRequest
import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import ch.obermuhlner.aitutor.user.domain.SubscriptionPlan
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class AdminControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getAllUsers endpoint`() {
        // Test getting all users - requires admin privileges
        val response = restTemplate.getForEntity(
            baseUrl("/admin/users"),
            ch.obermuhlner.aitutor.admin.dto.UsersPageResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test updateUserSubscriptionPlan endpoint`() {
        // First create a user to update
        val registerRequest = RegisterRequest(
            username = "testuserforadmin",
            email = "testadmin@example.com",
            password = "TestPassword123!",
            firstName = "Test",
            lastName = "User"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val registerEntity = HttpEntity(registerRequest, headers)

        val registerResponse = restTemplate.exchange(
            baseUrl("/auth/register"),
            HttpMethod.POST,
            registerEntity,
            UserResponse::class.java
        )

        Assertions.assertThat(registerResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(registerResponse.body).isNotNull
        val userId = registerResponse.body!!.id

        // Test updating the subscription plan
        val updateRequest = UpdateSubscriptionPlanRequest(
            subscriptionPlan = SubscriptionPlan.SUBSCRIPTION_10
        )

        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/admin/users/$userId/subscription-plan"),
            HttpMethod.PATCH,
            updateEntity,
            UserResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.subscriptionPlan).isEqualTo(SubscriptionPlan.SUBSCRIPTION_10)
    }

}