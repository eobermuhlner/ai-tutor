package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CreateCourseRequest
import ch.obermuhlner.aitutor.catalog.dto.CurriculumRequest
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class CurriculumControllerIntegrationTest : BaseControllerIntegrationTest() {


    @Test
    fun `test updateCurriculum and getCurriculum endpoints`() {
        // First create a course to configure curriculum for
        val createCourseRequest = CreateCourseRequest(
            languageCode = "es",
            nameJson = """{"en": "Test Course"}""",
            shortDescriptionJson = """{"en": "A test course"}""",
            descriptionJson = """{"en": "A comprehensive test course"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            defaultPhase = ConversationPhase.Correction,
            learningGoalsJson = """{"en": ["Learn basic greetings", "Form simple sentences"]}"""
        )

        val courseHeaders = HttpHeaders()
        courseHeaders.contentType = MediaType.APPLICATION_JSON
        val courseEntity = HttpEntity(createCourseRequest, courseHeaders)

        val courseResponse = restTemplate.exchange(
            baseUrl("/courses"),
            HttpMethod.POST,
            courseEntity,
            ch.obermuhlner.aitutor.catalog.dto.CourseManagementResponse::class.java
        )

        Assertions.assertThat(courseResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(courseResponse.body).isNotNull
        val courseId = courseResponse.body!!.id

        // Create curriculum settings for the course
        val createCurriculumRequest = CurriculumRequest(
            progressionMode = "LINEAR",
            allowSkipping = false,
            requireCompletion = true
        )

        val curriculumHeaders = HttpHeaders()
        curriculumHeaders.contentType = MediaType.APPLICATION_JSON
        val curriculumEntity = HttpEntity(createCurriculumRequest, curriculumHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/courses/$courseId/curriculum"),
            HttpMethod.PUT,
            curriculumEntity,
            ch.obermuhlner.aitutor.catalog.dto.CurriculumResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(createResponse.body).isNotNull
        Assertions.assertThat(createResponse.body!!.courseId).isEqualTo(courseId)
        Assertions.assertThat(createResponse.body!!.progressionMode).isEqualTo("LINEAR")

        // Test getting curriculum
        val getResponse = restTemplate.getForEntity(
            baseUrl("/courses/$courseId/curriculum"),
            ch.obermuhlner.aitutor.catalog.dto.CurriculumResponse::class.java
        )

        Assertions.assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getResponse.body).isNotNull
        Assertions.assertThat(getResponse.body!!.courseId).isEqualTo(courseId)
        Assertions.assertThat(getResponse.body!!.progressionMode).isEqualTo("LINEAR")
    }
}