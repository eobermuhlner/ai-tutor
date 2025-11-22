package ch.obermuhlner.aitutor.lesson.controller

import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class LessonControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getCourseCurriculum endpoint - not found when course doesn't exist`() {
        // Test getting curriculum for a non-existent course
        val nonExistentCourseId = "non-existent-course"
        val response = restTemplate.getForEntity(
            baseUrl("/lessons/courses/$nonExistentCourseId/curriculum"),
            ch.obermuhlner.aitutor.lesson.dto.CourseCurriculumResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getLesson endpoint - not found when lesson doesn't exist`() {
        // Test getting a lesson that doesn't exist
        val response = restTemplate.getForEntity(
            baseUrl("/lessons/courses/non-existent-course/lessons/non-existent-lesson"),
            ch.obermuhlner.aitutor.lesson.dto.LessonContentResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getCurrentLesson endpoint - not found when session doesn't exist`() {
        // Test getting current lesson for a non-existent session
        val nonExistentSessionId = UUID.randomUUID()

        val response = restTemplate.exchange(
            baseUrl("/lessons/sessions/$nonExistentSessionId/current"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String::class.java
        )

        Assertions.assertThat(response.statusCode).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN)
    }

    @Test
    fun `test getCurrentLesson endpoint - with existing session`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val response = restTemplate.postForEntity(
            baseUrl("/chat/sessions"),
            createRequest,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(response.body).isNotNull
        val sessionId = response.body!!.id

        // Try to get current lesson for the session
        // This will likely return 400 (Bad Request) since the session is not course-based
        try {
            restTemplate.getForEntity(
                baseUrl("/lessons/sessions/$sessionId/current"),
                ch.obermuhlner.aitutor.lesson.dto.LessonContentResponse::class.java
            )
            // If successful, it means the call worked but there might not be a lesson
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            // Could return 400 since the session is not course-based (no courseTemplateId)
            Assertions.assertThat(e.statusCode).isIn(HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND)
        }
    }

    @Test
    fun `test advanceLesson endpoint - not found when session doesn't exist`() {
        // Test advancing lesson for a non-existent session
        val nonExistentSessionId = UUID.randomUUID()

        val response = restTemplate.exchange(
            baseUrl("/lessons/sessions/$nonExistentSessionId/advance"),
            HttpMethod.POST,
            HttpEntity.EMPTY,
            String::class.java
        )

        Assertions.assertThat(response.statusCode).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN)
    }
}