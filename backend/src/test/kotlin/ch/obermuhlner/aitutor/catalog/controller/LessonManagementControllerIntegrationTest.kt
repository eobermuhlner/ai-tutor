package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CreateCourseRequest
import ch.obermuhlner.aitutor.catalog.dto.LessonRequest
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

class LessonManagementControllerIntegrationTest : BaseControllerIntegrationTest() {


    @Test
    fun `test createLesson and getLessons endpoints`() {
        // First create a course to add lessons to
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

        // Now create a lesson for this course
        val createLessonRequest = LessonRequest(
            lessonId = "lesson-1",
            title = "Greetings",
            content = "# Greetings\n\nHello - Hola\nGoodbye - Adiós",
            displayOrder = 1
        )

        val lessonHeaders = HttpHeaders()
        lessonHeaders.contentType = MediaType.APPLICATION_JSON
        val lessonEntity = HttpEntity(createLessonRequest, lessonHeaders)

        val createLessonResponse = restTemplate.exchange(
            baseUrl("/courses/$courseId/lessons"),
            HttpMethod.POST,
            lessonEntity,
            ch.obermuhlner.aitutor.catalog.dto.LessonResponse::class.java
        )

        Assertions.assertThat(createLessonResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(createLessonResponse.body).isNotNull
        Assertions.assertThat(createLessonResponse.body!!.lessonId).isEqualTo("lesson-1")
        Assertions.assertThat(createLessonResponse.body!!.title).isEqualTo("Greetings")

        // Test getting lessons
        val getLessonsResponse = restTemplate.getForEntity(
            baseUrl("/courses/$courseId/lessons"),
            Array<ch.obermuhlner.aitutor.catalog.dto.LessonResponse>::class.java
        )

        Assertions.assertThat(getLessonsResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getLessonsResponse.body).isNotNull
        Assertions.assertThat(getLessonsResponse.body).hasSize(1)
        Assertions.assertThat(getLessonsResponse.body!![0].lessonId).isEqualTo("lesson-1")
    }
}