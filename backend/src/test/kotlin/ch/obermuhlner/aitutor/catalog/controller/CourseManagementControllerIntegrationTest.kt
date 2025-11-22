package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CreateCourseRequest
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

class CourseManagementControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getAllCourses endpoint`() {
        // Test getting all courses
        val response = restTemplate.getForEntity(
            baseUrl("/courses"),
            Array<ch.obermuhlner.aitutor.catalog.dto.CourseManagementResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test createCourse endpoint`() {
        // Test creating a new course
        val createRequest = CreateCourseRequest(
            languageCode = "es",
            nameJson = """{"en": "Spanish Basics"}""",
            shortDescriptionJson = """{"en": "Basic Spanish course"}""",
            descriptionJson = """{"en": "A comprehensive basic Spanish course"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            defaultPhase = ConversationPhase.Correction,
            learningGoalsJson = """{"en": ["Learn basic greetings", "Form simple sentences"]}"""
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/courses"),
            HttpMethod.POST,
            entity,
            ch.obermuhlner.aitutor.catalog.dto.CourseManagementResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.languageCode).isEqualTo("es")
    }


    @Test
    fun `test getAllCourses with includeDrafts parameter`() {
        // Test getting all courses with includeDrafts parameter
        val response = restTemplate.getForEntity(
            baseUrl("/courses?includeDrafts=true"),
            Array<ch.obermuhlner.aitutor.catalog.dto.CourseManagementResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }
}