package ch.obermuhlner.aitutor.assessment.controller

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

class AssessmentControllerIntegrationTest : BaseControllerIntegrationTest() {


    @Test
    fun `test getSkillBreakdown endpoint - with existing session`() {
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

        // Test getting skill breakdown for the created session
        val response = restTemplate.getForEntity(
            baseUrl("/assessment/sessions/$sessionId/skills"),
            ch.obermuhlner.aitutor.assessment.dto.SkillBreakdownResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }


    @Test
    fun `test triggerReassessment endpoint - with existing session`() {
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

        // Test triggering reassessment for the created session
        val response = restTemplate.postForEntity(
            baseUrl("/assessment/sessions/$sessionId/reassess"),
            null,
            ch.obermuhlner.aitutor.assessment.dto.SkillBreakdownResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }
}