package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.CreateSessionFromCourseRequest
import ch.obermuhlner.aitutor.chat.dto.SendMessageRequest
import ch.obermuhlner.aitutor.chat.dto.UpdatePhaseRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateTopicRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateTeachingStyleRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateVocabularyReviewModeRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateLessonRequest
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class ChatControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test createSession endpoint`() {
        // Create test data
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Spanish Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            entity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.userId).isEqualTo(testUserId)
        Assertions.assertThat(response.body!!.tutorName).isEqualTo("Spanish Tutor")
        Assertions.assertThat(response.body!!.sourceLanguageCode).isEqualTo("en")
        Assertions.assertThat(response.body!!.targetLanguageCode).isEqualTo("es")
    }

    @Test
    fun `test createSessionFromCourse endpoint`() {
        // Create a course and tutor for testing
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Spanish Basics"}""",
            shortDescriptionJson = """{"en": "Basic Spanish course"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"]}""",
            displayOrder = 1,
            isDraft = false
        )
        val savedCourse = courseTemplateRepository.save(course)

        val tutor = TutorProfileEntity(
            name = "Spanish Tutor",
            emoji = "🇪🇸",
            personaEnglish = "Friendly Spanish tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Helps with Spanish basics",
            personaJson = """{"en": "Friendly Spanish tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Helps with Spanish basics"}""",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val savedTutor = tutorProfileRepository.save(tutor)

        val createRequest = CreateSessionFromCourseRequest(
            courseTemplateId = savedCourse.id,
            tutorProfileId = savedTutor.id,
            customName = "Test Session"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/from-course"),
            HttpMethod.POST,
            entity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.userId).isEqualTo(testUserId)
        Assertions.assertThat(response.body!!.courseTemplateId).isEqualTo(savedCourse.id)
        Assertions.assertThat(response.body!!.tutorProfileId).isEqualTo(savedTutor.id)
    }

    @Test
    fun `test getUserSessions endpoint`() {
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

        // Test getting user sessions
        val response = restTemplate.getForEntity(
            baseUrl("/chat/sessions?userId=${testUserId}"),
            Array<SessionResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).hasSize(1)
        Assertions.assertThat(response.body!![0].id).isEqualTo(sessionId)
    }

    @Test
    fun `test getSession endpoint`() {
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

        // Test getting the specific session
        val response = restTemplate.getForEntity(
            baseUrl("/chat/sessions/$sessionId"),
            ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.session.id).isEqualTo(sessionId)
    }

    @Test
    fun `test updateSessionPhase endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test updating session phase
        val updateRequest = UpdatePhaseRequest(ConversationPhase.Drill)
        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId/phase"),
            HttpMethod.PATCH,
            updateEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.conversationPhase).isEqualTo(ConversationPhase.Drill)
    }

    @Test
    fun `test updateSessionTopic endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test updating session topic
        val updateRequest = UpdateTopicRequest("travel")
        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId/topic"),
            HttpMethod.PATCH,
            updateEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.currentTopic).isEqualTo("travel")
    }

    @Test
    fun `test updateSessionTeachingStyle endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test updating teaching style
        val updateRequest = UpdateTeachingStyleRequest(TeachingStyle.Guided)
        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId/teaching-style"),
            HttpMethod.PATCH,
            updateEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.tutorTeachingStyle).isEqualTo(TeachingStyle.Guided)
    }

    @Test
    fun `test updateVocabularyReviewMode endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test updating vocabulary review mode
        val updateRequest = UpdateVocabularyReviewModeRequest(true)
        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId/vocabulary-review-mode"),
            HttpMethod.PATCH,
            updateEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.vocabularyReviewMode).isTrue()
    }

    @Test
    fun `test deleteSession endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Verify session exists
        val getResponse = restTemplate.getForEntity(
            baseUrl("/chat/sessions/$sessionId"),
            ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse::class.java
        )
        Assertions.assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)

        // Test deleting the session
        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId"),
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            Void::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // Verify session is deleted
        val getResponseAfterDelete = restTemplate.getForEntity(
            baseUrl("/chat/sessions/$sessionId"),
            ch.obermuhlner.aitutor.chat.dto.SessionWithMessagesResponse::class.java
        )
        Assertions.assertThat(getResponseAfterDelete.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test sendMessage endpoint`() {
        // Create a session first
        val createRequest = CreateSessionRequest(
            userId = testUserId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val createHeaders = HttpHeaders()
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(createRequest, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl("/chat/sessions"),
            HttpMethod.POST,
            createEntity,
            SessionResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull
        val sessionId = createResponse.body!!.id

        // Test sending a message
        val messageRequest = SendMessageRequest("Hello, how are you?")
        val messageHeaders = HttpHeaders()
        messageHeaders.contentType = MediaType.APPLICATION_JSON
        val messageEntity = HttpEntity(messageRequest, messageHeaders)

        val response = restTemplate.exchange(
            baseUrl("/chat/sessions/$sessionId/messages"),
            HttpMethod.POST,
            messageEntity,
            ch.obermuhlner.aitutor.chat.dto.MessageResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.content).isNotEmpty()
    }
}