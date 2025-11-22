package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class SummaryControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getSessionSummaryInfo endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            entity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test getting summary info
        val response = restTemplate.getForEntity(
            baseUrl("/summaries/sessions/$sessionId/info"),
            ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test getSessionSummaryDetails endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            entity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test getting summary details
        val response = restTemplate.getForEntity(
            baseUrl("/summaries/sessions/$sessionId/details"),
            Array<ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test getGlobalStats endpoint - requires admin`() {
        // This endpoint requires admin privileges
        val response = restTemplate.getForEntity(
            baseUrl("/summaries/stats"),
            Map::class.java
        )

        // Should return OK since our test user has admin privileges
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test triggerSummarization endpoint - requires admin`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            entity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test triggering summarization - should work since test user has admin privileges
        val response = restTemplate.postForEntity(
            baseUrl("/summaries/sessions/$sessionId/trigger"),
            null,
            Map::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!["status"]).isEqualTo("accepted")
    }
}