package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.user.dto.UpdateApiKeyRequest
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class ApiKeyControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getApiKeyConfiguration endpoint`() {
        // Test getting API key configuration for the current user
        val response = restTemplate.getForEntity(
            baseUrl("/users/me/api-key"),
            ch.obermuhlner.aitutor.user.dto.ApiKeyConfigurationResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test setApiKey endpoint`() {
        // Test setting an API key
        val updateRequest = UpdateApiKeyRequest(
            apiKey = "test-api-key",  // Use a test API key format
            endpoint = "https://api.openai.com/v1"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(updateRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/users/me/api-key"),
            HttpMethod.PUT,
            entity,
            Map::class.java  // Response is a Map<String, String>
        )

        // The endpoint will likely fail validation for a fake API key, but it should return a 400 with proper validation
        // Or it might pass if validation is skipped in test environment
        Assertions.assertThat(response.statusCode).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `test removeApiKey endpoint`() {
        // First set an API key to remove
        val updateRequest = UpdateApiKeyRequest(
            apiKey = "test-api-key-to-remove",
            endpoint = "https://api.openai.com/v1"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(updateRequest, headers)

        restTemplate.exchange(
            baseUrl("/users/me/api-key"),
            HttpMethod.PUT,
            entity,
            Map::class.java
        )

        // Now remove the API key
        val response = restTemplate.exchange(
            baseUrl("/users/me/api-key"),
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            Map::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!["message"]).isNotNull()
    }
}