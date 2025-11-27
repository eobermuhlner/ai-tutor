package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.dto.CreateSessionFromCourseRequest
import ch.obermuhlner.aitutor.chat.dto.CreateSessionRequest
import ch.obermuhlner.aitutor.chat.dto.SendMessageRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateCorrectionsRequest
import ch.obermuhlner.aitutor.chat.dto.UpdatePhaseRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateTeachingStyleRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateTopicRequest
import ch.obermuhlner.aitutor.chat.dto.UpdateVocabularyReviewModeRequest
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.conversation.config.AudioProperties
import ch.obermuhlner.aitutor.conversation.service.AiAudioService
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class ChatControllerTest {
    private lateinit var chatService: ChatService
    private lateinit var authorizationService: AuthorizationService
    private lateinit var catalogService: CatalogService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var audioService: AiAudioService
    private lateinit var audioProperties: AudioProperties
    private lateinit var rateLimitingService: RateLimitingService
    private lateinit var userRepository: UserRepository
    private lateinit var controller: ChatController

    @BeforeEach
    fun setup() {
        chatService = mockk()
        authorizationService = mockk()
        catalogService = mockk()
        chatSessionRepository = mockk()
        audioService = mockk()
        audioProperties = mockk()
        rateLimitingService = mockk()
        userRepository = mockk()
        controller = ChatController(
            chatService = chatService,
            authorizationService = authorizationService,
            catalogService = catalogService,
            chatSessionRepository = chatSessionRepository,
            audioService = audioService,
            audioProperties = audioProperties,
            rateLimitingService = rateLimitingService,
            userRepository = userRepository
        )
    }

    @Test
    fun `createSession should call chatService and return created session`() {
        val userId = UUID.randomUUID()
        val request = CreateSessionRequest(
            userId = userId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.requireAccessToUser(userId) } returns Unit
        every { chatService.createSession(request) } returns sessionResponse

        val result = controller.createSession(request)

        verify { authorizationService.requireAccessToUser(userId) }
        verify { chatService.createSession(request) }
        assert(result.statusCode == HttpStatus.CREATED)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `createSessionFromCourse should create session from course and return it`() {
        val currentUserId = UUID.randomUUID()
        val courseTemplateId = UUID.randomUUID()
        val tutorId = UUID.randomUUID()
        val request = CreateSessionFromCourseRequest(
            courseTemplateId = courseTemplateId,
            tutorProfileId = tutorId,
            customName = "Test Session"
        )
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { 
            chatService.createSessionFromCourse(
                userId = currentUserId,
                courseTemplateId = courseTemplateId,
                tutorProfileId = tutorId,
                sourceLanguageCode = "en",
                customName = "Test Session"
            ) 
        } returns sessionResponse

        val result = controller.createSessionFromCourse(request)

        verify { authorizationService.getCurrentUserId() }
        verify { 
            chatService.createSessionFromCourse(
                userId = currentUserId,
                courseTemplateId = courseTemplateId,
                tutorProfileId = tutorId,
                sourceLanguageCode = "en",
                customName = "Test Session"
            ) 
        }
        assert(result.statusCode == HttpStatus.CREATED)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `createSessionFromCourse should return bad request when no tutor available`() {
        val currentUserId = UUID.randomUUID()
        val courseTemplateId = UUID.randomUUID()
        val request = CreateSessionFromCourseRequest(
            courseTemplateId = courseTemplateId,
            tutorProfileId = null,
            customName = null
        )

        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { catalogService.getTutorsForCourse(courseTemplateId) } returns emptyList()

        val exception = assertThrows<ch.obermuhlner.aitutor.core.exception.BadRequestException> {
            controller.createSessionFromCourse(request)
        }

        verify { authorizationService.getCurrentUserId() }
        verify { catalogService.getTutorsForCourse(courseTemplateId) }
        assert(exception.message?.contains("No tutors available") == true)
    }

    @Test
    fun `getUserSessions should return user sessions`() {
        val userId = UUID.randomUUID()
        val resolvedUserId = UUID.randomUUID()
        val sessionList = listOf<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.resolveUserId(userId) } returns resolvedUserId
        every { chatService.getUserSessions(resolvedUserId) } returns sessionList

        val result = controller.getUserSessions(userId)

        verify { authorizationService.resolveUserId(userId) }
        verify { chatService.getUserSessions(resolvedUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionList)
    }

    @Test
    fun `updateSessionPhase should update session phase and return updated session`() {
        val sessionId = UUID.randomUUID()
        val phase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Correction
        val request = UpdatePhaseRequest(phase)
        val currentUserId = UUID.randomUUID()
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateSessionPhase(sessionId, phase, currentUserId) } returns sessionResponse

        val result = controller.updateSessionPhase(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateSessionPhase(sessionId, phase, currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `updateSessionPhase should return not found when session not found`() {
        val sessionId = UUID.randomUUID()
        val phase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Correction
        val request = UpdatePhaseRequest(phase)
        val currentUserId = UUID.randomUUID()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateSessionPhase(sessionId, phase, currentUserId) } returns null

        val result = controller.updateSessionPhase(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateSessionPhase(sessionId, phase, currentUserId) }
        assert(result.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun `updateSessionTopic should update session topic and return updated session`() {
        val sessionId = UUID.randomUUID()
        val topic = "travel"
        val request = UpdateTopicRequest(topic)
        val currentUserId = UUID.randomUUID()
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateSessionTopic(sessionId, topic, currentUserId) } returns sessionResponse

        val result = controller.updateSessionTopic(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateSessionTopic(sessionId, topic, currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `updateSessionTeachingStyle should update teaching style and return updated session`() {
        val sessionId = UUID.randomUUID()
        val teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive
        val request = UpdateTeachingStyleRequest(teachingStyle)
        val currentUserId = UUID.randomUUID()
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateSessionTeachingStyle(sessionId, teachingStyle, currentUserId) } returns sessionResponse

        val result = controller.updateSessionTeachingStyle(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateSessionTeachingStyle(sessionId, teachingStyle, currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `updateVocabularyReviewMode should update vocabulary review mode and return updated session`() {
        val sessionId = UUID.randomUUID()
        val enabled = true
        val request = UpdateVocabularyReviewModeRequest(enabled)
        val currentUserId = UUID.randomUUID()
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateVocabularyReviewMode(sessionId, enabled, currentUserId) } returns sessionResponse

        val result = controller.updateVocabularyReviewMode(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateVocabularyReviewMode(sessionId, enabled, currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `updateSessionLesson should update session lesson and return updated session`() {
        val sessionId = UUID.randomUUID()
        val direction = ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT
        val request = ch.obermuhlner.aitutor.chat.dto.UpdateLessonRequest(direction)
        val currentUserId = UUID.randomUUID()
        val sessionResponse = mockk<ch.obermuhlner.aitutor.chat.dto.SessionResponse>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateSessionLesson(sessionId, direction, currentUserId) } returns sessionResponse

        val result = controller.updateSessionLesson(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateSessionLesson(sessionId, direction, currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == sessionResponse)
    }

    @Test
    fun `sendMessage should send message and return response`() {
        val sessionId = UUID.randomUUID()
        val userContent = "Hello"
        val request = SendMessageRequest(userContent)
        val currentUserId = UUID.randomUUID()
        val messageResponse = mockk<ch.obermuhlner.aitutor.chat.dto.MessageResponse>()
        val userEntity = mockk<ch.obermuhlner.aitutor.user.domain.UserEntity>()
        val rateLimitStatus = mockk<ch.obermuhlner.aitutor.user.service.RateLimitingService.RateLimitStatus>()
        
        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns java.util.Optional.of(userEntity)
        every { userEntity.subscriptionPlan } returns ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE
        every { rateLimitingService.getRateLimitStatus(currentUserId, ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE) } returns rateLimitStatus
        every { rateLimitStatus.dailyLimit } returns 100
        every { rateLimitStatus.availableTokens } returns 99
        every { rateLimitStatus.hourlyLimit } returns 20
        every { rateLimitStatus.hourlyRemaining } returns 19
        every { rateLimitStatus.dailyRemaining } returns 98
        every { rateLimitStatus.hourlyResetSeconds } returns 3600
        every { rateLimitStatus.dailyResetSeconds } returns 86400
        every { chatService.sendMessage(sessionId, userContent, currentUserId) } returns messageResponse

        val result = controller.sendMessage(sessionId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { userRepository.findById(currentUserId) }
        verify { rateLimitingService.getRateLimitStatus(currentUserId, ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE) }
        verify { chatService.sendMessage(sessionId = sessionId, userContent = userContent, currentUserId = currentUserId) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == messageResponse)
    }

    @Test
    fun `updateMessageCorrections should update corrections and return updated message`() {
        val sessionId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val currentUserId = UUID.randomUUID()
        val corrections = listOf(
            Correction(
                span = "gehen",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Medium,
                correctedTargetLanguage = "gehe",
                whySourceLanguage = "Use 'gehe' with 'ich'",
                whyTargetLanguage = "Verwende 'gehe' mit 'ich'"
            )
        )
        val request = UpdateCorrectionsRequest(corrections)
        val messageResponse = mockk<ch.obermuhlner.aitutor.chat.dto.MessageResponse>()

        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateMessageCorrections(sessionId, messageId, currentUserId, corrections) } returns messageResponse

        val result = controller.updateMessageCorrections(sessionId, messageId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateMessageCorrections(sessionId, messageId, currentUserId, corrections) }
        assert(result.statusCode == HttpStatus.OK)
        assert(result.body == messageResponse)
    }

    @Test
    fun `updateMessageCorrections should return 404 when message not found`() {
        val sessionId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val currentUserId = UUID.randomUUID()
        val corrections = listOf(
            Correction(
                span = "test",
                errorType = ErrorType.Typography,
                severity = ErrorSeverity.Low,
                correctedTargetLanguage = "test",
                whySourceLanguage = "Test",
                whyTargetLanguage = "Test"
            )
        )
        val request = UpdateCorrectionsRequest(corrections)

        every { authorizationService.getCurrentUserId() } returns currentUserId
        every { chatService.updateMessageCorrections(sessionId, messageId, currentUserId, corrections) } returns null

        val result = controller.updateMessageCorrections(sessionId, messageId, request)

        verify { authorizationService.getCurrentUserId() }
        verify { chatService.updateMessageCorrections(sessionId, messageId, currentUserId, corrections) }
        assert(result.statusCode == HttpStatus.NOT_FOUND)
    }
}