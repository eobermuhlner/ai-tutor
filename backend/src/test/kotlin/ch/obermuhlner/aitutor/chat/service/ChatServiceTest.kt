package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import ch.obermuhlner.aitutor.tutor.domain.ConversationState
import ch.obermuhlner.aitutor.tutor.service.PhaseDecision
import ch.obermuhlner.aitutor.tutor.service.TopicDecision
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatServiceTest {

    private lateinit var chatService: ChatService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var tutorService: TutorService
    private lateinit var correctionService: ch.obermuhlner.aitutor.tutor.service.CorrectionService
    private lateinit var vocabularyService: VocabularyService
    private lateinit var vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService
    private lateinit var phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService
    private lateinit var topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService
    private lateinit var metadataEvaluationService: ch.obermuhlner.aitutor.tutor.service.MetadataEvaluationService
    private lateinit var catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService
    private lateinit var errorAnalyticsService: ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
    private lateinit var userLanguageService: ch.obermuhlner.aitutor.user.service.UserLanguageService
    private lateinit var lessonProgressionService: ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
    private lateinit var rateLimitingService: ch.obermuhlner.aitutor.user.service.RateLimitingService
    private lateinit var userRepository: ch.obermuhlner.aitutor.user.repository.UserRepository
    private lateinit var imageService: ch.obermuhlner.aitutor.image.service.ImageService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        chatSessionRepository = mockk()
        chatMessageRepository = mockk()
        tutorService = mockk()
        correctionService = mockk(relaxed = true)
        vocabularyService = mockk()
        vocabularyReviewService = mockk()
        phaseDecisionService = mockk()
        topicDecisionService = mockk()
        metadataEvaluationService = mockk(relaxed = true)
        catalogService = mockk()
        errorAnalyticsService = mockk(relaxed = true)
        userLanguageService = mockk(relaxed = true)
        lessonProgressionService = mockk(relaxed = true)
        rateLimitingService = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        imageService = mockk(relaxed = true)
        val userChatModelFactory = mockk<ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory>(relaxed = true)
        val mockChatModel = mockk<org.springframework.ai.chat.model.ChatModel>(relaxed = true)
        every { userChatModelFactory.getChatModelForUser(any()) } returns mockChatModel

        // Mock user for rate limiting
        val testUser = TestDataFactory.createUserEntity()
        every { userRepository.findById(any()) } returns java.util.Optional.of(testUser)

        objectMapper = jacksonObjectMapper()

        chatService = ChatService(
            chatSessionRepository,
            chatMessageRepository,
            tutorService,
            correctionService,
            vocabularyService,
            vocabularyReviewService,
            phaseDecisionService,
            topicDecisionService,
            metadataEvaluationService,
            catalogService,
            errorAnalyticsService,
            userLanguageService,
            lessonProgressionService,
            userChatModelFactory,
            rateLimitingService,
            userRepository,
            imageService,
            objectMapper,
            "An error occurred"
        )
    }

    @Test
    fun `should create session successfully`() {
        val request = TestDataFactory.createSessionRequest()
        val savedSession = TestDataFactory.createSessionEntity()

        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns savedSession

        val result = chatService.createSession(request)

        assertNotNull(result)
        assertEquals(savedSession.id, result.id)
        assertEquals(request.tutorName, result.tutorName)
        assertEquals(request.sourceLanguageCode, result.sourceLanguageCode)
        assertEquals(request.targetLanguageCode, result.targetLanguageCode)

        verify(exactly = 1) { chatSessionRepository.save(any<ChatSessionEntity>()) }
    }

    @Test
    fun `should get session by id`() {
        val session = TestDataFactory.createSessionEntity()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getSession(TestDataFactory.TEST_SESSION_ID)

        assertNotNull(result)
        assertEquals(TestDataFactory.TEST_SESSION_ID, result?.id)
    }

    @Test
    fun `should return null when session not found`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.getSession(UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should get user sessions ordered by updated date`() {
        val sessions = listOf(
            TestDataFactory.createSessionEntity(),
            TestDataFactory.createSessionEntity(id = UUID.randomUUID())
        )

        every { chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) } returns sessions

        val result = chatService.getUserSessions(TestDataFactory.TEST_USER_ID)

        assertEquals(2, result.size)
        verify(exactly = 1) { chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) }
    }

    @Test
    fun `should send message and update session state`() {
        val session = TestDataFactory.createSessionEntity()
        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Test reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Test reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test message", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals("ASSISTANT", result?.role)
        assertEquals("Test reply", result?.content)

        verify { chatSessionRepository.save(any<ChatSessionEntity>()) }
        verify { chatMessageRepository.save(any<ChatMessageEntity>()) }
    }

    @Test
    fun `should return null when sending message to non-existent session`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.sendMessage(UUID.randomUUID(), "Test message", TestDataFactory.TEST_USER_ID)

        assertNull(result)
    }

    @Test
    fun `should delete session`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { chatMessageRepository.deleteAll(any<List<ChatMessageEntity>>()) } just Runs
        every { chatSessionRepository.deleteById(any()) } just Runs

        val result = chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertTrue(result)
        verify(exactly = 1) { chatSessionRepository.deleteById(TestDataFactory.TEST_SESSION_ID) }
    }

    @Test
    fun `should handle topic change and archive old topic`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "old-topic"

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        val allMessages = (1..10).flatMap {
            listOf(
                TestDataFactory.createMessageEntity(session, MessageRole.USER, "msg $it"),
                TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "reply $it")
            )
        }

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns allMessages
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        // Verify that MetadataEvaluationService is called to evaluate metadata periodically
        verify { metadataEvaluationService.evaluateIfNeeded(TestDataFactory.TEST_SESSION_ID, any()) }
    }

    @Test
    fun `should not update user-controlled phase when not in Auto mode`() {
        val session = TestDataFactory.createSessionEntity()
        session.conversationPhase = ConversationPhase.Correction
        session.effectivePhase = ConversationPhase.Correction

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returnsMany listOf(
            TestDataFactory.createMessageEntity(session),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")
        )
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        // User-controlled phase should NOT change from LLM suggestion
        verify { chatSessionRepository.save(match {
            it.conversationPhase == ConversationPhase.Correction &&
            it.effectivePhase == ConversationPhase.Correction
        }) }
    }

    @Test
    fun `should return null when getting session with messages for another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should get session with messages for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session, MessageRole.USER, "Hello"),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Hi there!")
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns messages

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(2, result?.messages?.size)
        assertEquals("Hello", result?.messages?.get(0)?.content)
        assertEquals("Hi there!", result?.messages?.get(1)?.content)
    }

    @Test
    fun `should not delete session owned by another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertFalse(result)
        verify(exactly = 0) { chatSessionRepository.deleteById(any()) }
    }

    @Test
    fun `should update session phase for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.conversationPhase = ConversationPhase.Free

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session.apply { conversationPhase = ConversationPhase.Drill }

        val result = chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(ConversationPhase.Drill, result?.conversationPhase)
        verify { chatSessionRepository.save(match { it.conversationPhase == ConversationPhase.Drill }) }
    }

    @Test
    fun `should not update session phase for another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should update session topic for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "old-topic"

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session.apply { currentTopic = "new-topic" }

        val result = chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, "new-topic", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals("new-topic", result?.currentTopic)
        verify { chatSessionRepository.save(match { it.currentTopic == "new-topic" }) }
    }

    @Test
    fun `should update session teaching style for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.tutorTeachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session.apply { tutorTeachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Directive }

        val result = chatService.updateSessionTeachingStyle(TestDataFactory.TEST_SESSION_ID, ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Directive, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Directive, result?.tutorTeachingStyle)
        verify { chatSessionRepository.save(match { it.tutorTeachingStyle == ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Directive }) }
    }

    @Test
    fun `should not update session teaching style for another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.updateSessionTeachingStyle(TestDataFactory.TEST_SESSION_ID, ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Guided, UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should get topic history for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "cooking"
        session.pastTopicsJson = """["travel", "sports"]"""

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals("cooking", result?.currentTopic)
        assertEquals(2, result?.pastTopics?.size)
        assertTrue(result?.pastTopics?.contains("travel") == true)
    }

    @Test
    fun `should return null when getting topic history for another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should enable vocabulary review mode for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.vocabularyReviewMode = false

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session.apply { vocabularyReviewMode = true }

        val result = chatService.updateVocabularyReviewMode(TestDataFactory.TEST_SESSION_ID, true, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertTrue(result?.vocabularyReviewMode == true)
        verify { chatSessionRepository.save(match { it.vocabularyReviewMode == true }) }
    }

    @Test
    fun `should disable vocabulary review mode for owned session`() {
        val session = TestDataFactory.createSessionEntity()
        session.vocabularyReviewMode = true

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session.apply { vocabularyReviewMode = false }

        val result = chatService.updateVocabularyReviewMode(TestDataFactory.TEST_SESSION_ID, false, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertFalse(result?.vocabularyReviewMode == true)
        verify { chatSessionRepository.save(match { it.vocabularyReviewMode == false }) }
    }

    @Test
    fun `should not update vocabulary review mode for another user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(any()) } returns Optional.of(session)

        val result = chatService.updateVocabularyReviewMode(TestDataFactory.TEST_SESSION_ID, true, UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should pass due vocabulary count to tutor when review mode enabled`() {
        val session = TestDataFactory.createSessionEntity()
        session.vocabularyReviewMode = true

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, ch.obermuhlner.aitutor.chat.domain.MessageRole.ASSISTANT, "Reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ch.obermuhlner.aitutor.tutor.domain.ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns ch.obermuhlner.aitutor.tutor.service.TopicDecision(null, 0, "Free conversation", emptyList())
        every { vocabularyReviewService.getDueCount(any(), any()) } returns 15L
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        verify { vocabularyReviewService.getDueCount(TestDataFactory.TEST_USER_ID, session.targetLanguageCode) }
        verify {
            tutorService.respond(
                any(),
                match { it.vocabularyReviewMode == true && it.dueVocabularyCount == 15L },
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `should not call getDueCount when review mode disabled`() {
        val session = TestDataFactory.createSessionEntity()
        session.vocabularyReviewMode = false

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, ch.obermuhlner.aitutor.chat.domain.MessageRole.ASSISTANT, "Reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ch.obermuhlner.aitutor.tutor.domain.ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns ch.obermuhlner.aitutor.tutor.service.TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        verify(exactly = 0) { vocabularyReviewService.getDueCount(any(), any()) }
        verify {
            tutorService.respond(
                any(),
                match { it.vocabularyReviewMode == false && it.dueVocabularyCount == null },
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `should create session from course successfully`() {
        val courseId = UUID.randomUUID()
        val tutorId = UUID.randomUUID()

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()
        every { course.defaultPhase } returns ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Free
        every { course.startingLevel } returns ch.obermuhlner.aitutor.core.model.CEFRLevel.A1

        val tutor = mockk<ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity>(relaxed = true)
        every { tutor.id } returns tutorId
        every { tutor.name } returns "Maria"
        every { tutor.personaEnglish } returns "friendly"
        every { tutor.domainEnglish } returns "general"
        every { tutor.teachingStyle } returns ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive
        every { tutor.targetLanguageCode } returns "es-ES"
        every { tutor.voiceId } returns ch.obermuhlner.aitutor.core.model.catalog.TutorVoice.Warm
        every { tutor.gender } returns ch.obermuhlner.aitutor.core.model.catalog.TutorGender.Female
        every { tutor.age } returns 30
        every { tutor.location } returns "Spain"
        every { tutor.emoji } returns "👩‍🏫"

        every { catalogService.getCourseById(courseId) } returns course
        every { catalogService.getTutorById(tutorId, TestDataFactory.TEST_USER_ID) } returns tutor
        every { userLanguageService.getLearningLanguages(TestDataFactory.TEST_USER_ID) } returns listOf()
        every { userLanguageService.suggestSourceLanguage(TestDataFactory.TEST_USER_ID, "es-ES") } returns "en"
        every { imageService.getImageUrlByPerson(any(), any(), any(), any()) } returns "http://imagestore.example.com/api/images/999"
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } answers { firstArg() }

        val result = chatService.createSessionFromCourse(
            TestDataFactory.TEST_USER_ID,
            courseId,
            tutorId,
            "en",
            "My Custom Course"
        )

        assertNotNull(result)
        assertEquals("http://imagestore.example.com/api/images/999", result?.tutorImage)
        verify { catalogService.getCourseById(courseId) }
        verify { catalogService.getTutorById(tutorId, TestDataFactory.TEST_USER_ID) }
        verify { chatSessionRepository.save(any<ChatSessionEntity>()) }
    }

    @Test
    fun `should return null when creating session from non-existent course`() {
        val courseId = UUID.randomUUID()
        val tutorId = UUID.randomUUID()

        every { catalogService.getCourseById(courseId) } returns null

        val result = chatService.createSessionFromCourse(
            TestDataFactory.TEST_USER_ID,
            courseId,
            tutorId,
            "en"
        )

        assertNull(result)
        verify { catalogService.getCourseById(courseId) }
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should return null when creating session from non-existent tutor`() {
        val courseId = UUID.randomUUID()
        val tutorId = UUID.randomUUID()

        val course = mockk<ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity>()

        every { catalogService.getCourseById(courseId) } returns course
        every { catalogService.getTutorById(tutorId, TestDataFactory.TEST_USER_ID) } returns null
        every { userLanguageService.getLearningLanguages(TestDataFactory.TEST_USER_ID) } returns listOf()
        every { userLanguageService.suggestSourceLanguage(TestDataFactory.TEST_USER_ID, any()) } returns "en"

        val result = chatService.createSessionFromCourse(
            TestDataFactory.TEST_USER_ID,
            courseId,
            tutorId,
            "en"
        )

        assertNull(result)
        verify { catalogService.getTutorById(tutorId, TestDataFactory.TEST_USER_ID) }
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should get active learning sessions with progress`() {
        val session1 = TestDataFactory.createSessionEntity()
        session1.isActive = true

        val session2 = TestDataFactory.createSessionEntity(id = UUID.randomUUID())
        session2.isActive = true

        every { chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) } returns listOf(session1, session2)
        every { chatSessionRepository.findById(session1.id) } returns Optional.of(session1)
        every { chatSessionRepository.findById(session2.id) } returns Optional.of(session2)
        every { chatMessageRepository.countBySessionId(session1.id) } returns 10L
        every { chatMessageRepository.countBySessionId(session2.id) } returns 5L
        every { vocabularyService.getVocabularyCountForLanguage(any(), any()) } returns 20

        val result = chatService.getActiveLearningSessions(TestDataFactory.TEST_USER_ID)

        assertEquals(2, result.size)
        assertEquals(10, result[0].progress.messageCount)
        assertEquals(5, result[1].progress.messageCount)
        verify { chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) }
    }

    @Test
    fun `should return empty list when no active learning sessions`() {
        every { chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) } returns emptyList()

        val result = chatService.getActiveLearningSessions(TestDataFactory.TEST_USER_ID)

        assertEquals(0, result.size)
    }

    @Test
    fun `should get session progress successfully`() {
        val session = TestDataFactory.createSessionEntity()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.countBySessionId(TestDataFactory.TEST_SESSION_ID) } returns 15L
        every { vocabularyService.getVocabularyCountForLanguage(TestDataFactory.TEST_USER_ID, session.targetLanguageCode) } returns 25

        val result = chatService.getSessionProgress(TestDataFactory.TEST_SESSION_ID)

        assertEquals(15, result.messageCount)
        assertEquals(25, result.vocabularyCount)
        verify { chatMessageRepository.countBySessionId(TestDataFactory.TEST_SESSION_ID) }
        verify { vocabularyService.getVocabularyCountForLanguage(TestDataFactory.TEST_USER_ID, session.targetLanguageCode) }
    }

    @Test
    fun `should return zero progress when session not found`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.getSessionProgress(UUID.randomUUID())

        assertEquals(0, result.messageCount)
        assertEquals(0, result.vocabularyCount)
        assertEquals(0L, result.daysActive)
    }

    @Test
    fun `should return null when getting non-existent session`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.getSession(UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should return null when updating topic for non-existent session`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.updateSessionTopic(UUID.randomUUID(), "new-topic", TestDataFactory.TEST_USER_ID)

        assertNull(result)
    }

    @Test
    fun `should return null when getting session for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should delete session and messages successfully`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session)
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns messages
        every { chatMessageRepository.deleteAll(messages) } returns Unit
        every { chatSessionRepository.deleteById(TestDataFactory.TEST_SESSION_ID) } returns Unit

        val result = chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertTrue(result)
        verify { chatMessageRepository.deleteAll(messages) }
        verify { chatSessionRepository.deleteById(TestDataFactory.TEST_SESSION_ID) }
    }

    @Test
    fun `should not delete session for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertFalse(result)
        verify(exactly = 0) { chatSessionRepository.deleteById(any()) }
    }

    @Test
    fun `should return false when deleting non-existent session`() {
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.empty()

        val result = chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertFalse(result)
        verify(exactly = 0) { chatSessionRepository.deleteById(any()) }
    }

    @Test
    fun `should update session phase successfully`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any()) } returns session

        val result = chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should not update session phase for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should update session teaching style successfully`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any()) } returns session

        val result = chatService.updateSessionTeachingStyle(TestDataFactory.TEST_SESSION_ID, ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Guided, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should not update teaching style for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.updateSessionTeachingStyle(TestDataFactory.TEST_SESSION_ID, ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Guided, UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should get topic history successfully`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "travel"
        session.pastTopicsJson = """["food", "weather"]"""

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals("travel", result!!.currentTopic)
        assertEquals(2, result.pastTopics.size)
        assertTrue(result.pastTopics.contains("food"))
        assertTrue(result.pastTopics.contains("weather"))
    }

    @Test
    fun `should get topic history with empty past topics`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "travel"
        session.pastTopicsJson = null

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals("travel", result!!.currentTopic)
        assertEquals(0, result.pastTopics.size)
    }

    @Test
    fun `should return null for topic history of wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID())

        assertNull(result)
    }

    @Test
    fun `should get session with messages successfully`() {
        val session = TestDataFactory.createSessionEntity()
        val messages = listOf(
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session)
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns messages

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(2, result!!.messages.size)
        verify { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) }
    }

    @Test
    fun `should get user sessions successfully`() {
        val session1 = TestDataFactory.createSessionEntity()
        val session2 = TestDataFactory.createSessionEntity(id = UUID.randomUUID())

        every { chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) } returns listOf(session1, session2)

        val result = chatService.getUserSessions(TestDataFactory.TEST_USER_ID)

        assertEquals(2, result.size)
        verify { chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(TestDataFactory.TEST_USER_ID) }
    }

    @Test
    fun `should update session topic and archive old topic`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentTopic = "old-topic"
        session.pastTopicsJson = null

        val messages = listOf(
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session),
            TestDataFactory.createMessageEntity(session = session)
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns messages
        every { topicDecisionService.countTurnsInRecentMessages(messages) } returns 5
        every { topicDecisionService.shouldArchiveTopic("old-topic", 5) } returns true
        every { chatSessionRepository.save(any()) } returns session

        val result = chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, "new-topic", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should not update topic for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, "new-topic", UUID.randomUUID())

        assertNull(result)
        verify(exactly = 0) { chatSessionRepository.save(any()) }
    }

    @Test
    fun `should get session with messages containing corrections JSON`() {
        val session = TestDataFactory.createSessionEntity()
        val correctionsJson = """[{"span":"hola","errorType":"Typography","severity":"Low","correctedTargetLanguage":"Hola","whySourceLanguage":"Capitalize","whyTargetLanguage":"Capitalizar"}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = "hola amigo",
            correctionsJson = correctionsJson,
            vocabularyJson = null,
            wordCardsJson = null
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.corrections)
        assertEquals(1, result?.messages?.get(0)?.corrections?.size)
        assertEquals("hola", result?.messages?.get(0)?.corrections?.get(0)?.span)
    }

    @Test
    fun `should get session with messages containing vocabulary JSON`() {
        val session = TestDataFactory.createSessionEntity()
        val vocabularyJson = """[{"lemma":"casa","context":"Mi casa es grande","conceptName":"house"}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = "Great! Casa means house.",
            correctionsJson = null,
            vocabularyJson = vocabularyJson,
            wordCardsJson = null
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)
        every { imageService.getImageUrlByConcept("house") } returns "http://imagestore.example.com/api/images/100"

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.newVocabulary)
        assertEquals(1, result?.messages?.get(0)?.newVocabulary?.size)
        assertEquals("casa", result?.messages?.get(0)?.newVocabulary?.get(0)?.lemma)
        assertEquals("http://imagestore.example.com/api/images/100", result?.messages?.get(0)?.newVocabulary?.get(0)?.imageUrl)
    }

    @Test
    fun `should get session with messages containing word cards JSON`() {
        val session = TestDataFactory.createSessionEntity()
        val wordCardsJson = """[{"titleSourceLanguage":"House","titleTargetLanguage":"Casa","descriptionSourceLanguage":"A building","descriptionTargetLanguage":"Un edificio","conceptName":"house"}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = "Here's a word card",
            correctionsJson = null,
            vocabularyJson = null,
            wordCardsJson = wordCardsJson
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)
        every { imageService.getImageUrlByConcept("house") } returns "http://imagestore.example.com/api/images/200"

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.wordCards)
        assertEquals(1, result?.messages?.get(0)?.wordCards?.size)
        assertEquals("House", result?.messages?.get(0)?.wordCards?.get(0)?.titleSourceLanguage)
        assertEquals("http://imagestore.example.com/api/images/200", result?.messages?.get(0)?.wordCards?.get(0)?.imageUrl)
    }

    @Test
    fun `should get session with messages containing all JSON fields`() {
        val session = TestDataFactory.createSessionEntity()
        val correctionsJson = """[{"span":"test","errorType":"Typography","severity":"Low","correctedTargetLanguage":"Test","whySourceLanguage":"Cap","whyTargetLanguage":"Cap"}]"""
        val vocabularyJson = """[{"lemma":"word","context":"context","conceptName":"concept"}]"""
        val wordCardsJson = """[{"titleSourceLanguage":"A","titleTargetLanguage":"B","descriptionSourceLanguage":"C","descriptionTargetLanguage":"D","conceptName":"card"}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = "Full response",
            correctionsJson = correctionsJson,
            vocabularyJson = vocabularyJson,
            wordCardsJson = wordCardsJson
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.corrections)
        assertNotNull(result?.messages?.get(0)?.newVocabulary)
        assertNotNull(result?.messages?.get(0)?.wordCards)
    }

    @Test
    fun `should get session with messages with vocabulary without concept name`() {
        val session = TestDataFactory.createSessionEntity()
        val vocabularyJson = """[{"lemma":"word","context":"context","conceptName":null}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = "Test",
            correctionsJson = null,
            vocabularyJson = vocabularyJson,
            wordCardsJson = null
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertNotNull(result?.messages?.get(0)?.newVocabulary)
        assertNull(result?.messages?.get(0)?.newVocabulary?.get(0)?.imageUrl)
    }

    @Test
    fun `should get session with messages with word card without concept name`() {
        val session = TestDataFactory.createSessionEntity()
        val wordCardsJson = """[{"titleSourceLanguage":"A","titleTargetLanguage":"B","descriptionSourceLanguage":"C","descriptionTargetLanguage":"D","conceptName":null}]"""
        val message = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = "Test",
            correctionsJson = null,
            vocabularyJson = null,
            wordCardsJson = wordCardsJson
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns listOf(message)

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertNotNull(result?.messages?.get(0)?.wordCards)
        assertNull(result?.messages?.get(0)?.wordCards?.get(0)?.imageUrl)
    }

    @Test
    fun `should advance lesson when LLM requests next lesson for course-based session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()  // Course-based session

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Great! Let's move to the next lesson.")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Great! Let's move to the next lesson.",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "I'm ready for the next lesson", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        // Lesson progression is now handled periodically by MetadataEvaluationService
        verify { metadataEvaluationService.evaluateIfNeeded(TestDataFactory.TEST_SESSION_ID, any()) }
    }

    @Test
    fun `should go to previous lesson when LLM requests previous lesson for course-based session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()  // Course-based session

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "OK, let's review the previous lesson.")

        val tutorResponse = TutorService.TutorResponse(
            reply = "OK, let's review the previous lesson.",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Can we go back to the previous lesson?", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        // Lesson progression is now handled periodically by MetadataEvaluationService
        verify { metadataEvaluationService.evaluateIfNeeded(TestDataFactory.TEST_SESSION_ID, any()) }
    }

    @Test
    fun `should not call lesson progression service when LLM requests lesson switch for non-course session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = null  // Not a course-based session

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Next lesson please", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify(exactly = 0) { lessonProgressionService.navigateToNextLesson(any()) }
        verify(exactly = 0) { lessonProgressionService.navigateToPreviousLesson(any()) }
    }

    @Test
    fun `should not call lesson progression service when requestedLessonAction is null`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()  // Course-based session

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Normal conversation", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify(exactly = 0) { lessonProgressionService.navigateToNextLesson(any()) }
        verify(exactly = 0) { lessonProgressionService.navigateToPreviousLesson(any()) }
    }

    @Test
    fun `should handle stay lesson action without calling progression service`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()  // Course-based session

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Let's continue with this lesson.")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Let's continue with this lesson.",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Continue", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify(exactly = 0) { lessonProgressionService.navigateToNextLesson(any()) }
        verify(exactly = 0) { lessonProgressionService.navigateToPreviousLesson(any()) }
    }

    @Test
    fun `getMessage should return message when it belongs to session`() {
        val session = TestDataFactory.createSessionEntity()
        val messageId = UUID.randomUUID()
        val message = TestDataFactory.createMessageEntity(session)

        every { chatMessageRepository.findById(messageId) } returns Optional.of(message)

        val result = chatService.getMessage(TestDataFactory.TEST_SESSION_ID, messageId)

        assertNotNull(result)
        assertEquals(message.id, result?.id)
        verify { chatMessageRepository.findById(messageId) }
    }

    @Test
    fun `getMessage should return null when message not found`() {
        val messageId = UUID.randomUUID()

        every { chatMessageRepository.findById(messageId) } returns Optional.empty()

        val result = chatService.getMessage(TestDataFactory.TEST_SESSION_ID, messageId)

        assertNull(result)
    }

    @Test
    fun `getMessage should return null when message belongs to different session`() {
        val session = TestDataFactory.createSessionEntity()
        val differentSessionId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val message = TestDataFactory.createMessageEntity(session)

        every { chatMessageRepository.findById(messageId) } returns Optional.of(message)

        val result = chatService.getMessage(differentSessionId, messageId)

        assertNull(result)
    }

    @Test
    fun `updateMessageAudioCache should update audio data successfully`() {
        val session = TestDataFactory.createSessionEntity()
        val messageId = UUID.randomUUID()
        val message = TestDataFactory.createMessageEntity(session)
        val audioData = ByteArray(100) { it.toByte() }
        val voiceId = "en-US-1"
        val speed = 1.2

        every { chatMessageRepository.findById(messageId) } returns Optional.of(message)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns message

        val result = chatService.updateMessageAudioCache(TestDataFactory.TEST_SESSION_ID, messageId, audioData, voiceId, speed)

        assertNotNull(result)
        verify { chatMessageRepository.save(match {
            it.audioData.contentEquals(audioData) &&
            it.audioVoiceId == voiceId &&
            it.audioSpeed == speed
        }) }
    }

    @Test
    fun `updateMessageAudioCache should return null when message not found`() {
        val messageId = UUID.randomUUID()
        val audioData = ByteArray(100)

        every { chatMessageRepository.findById(messageId) } returns Optional.empty()

        val result = chatService.updateMessageAudioCache(TestDataFactory.TEST_SESSION_ID, messageId, audioData, null, null)

        assertNull(result)
        verify(exactly = 0) { chatMessageRepository.save(any()) }
    }

    @Test
    fun `updateMessageAudioCache should update audio with null voice and speed`() {
        val session = TestDataFactory.createSessionEntity()
        val messageId = UUID.randomUUID()
        val message = TestDataFactory.createMessageEntity(session)
        val audioData = ByteArray(50)

        every { chatMessageRepository.findById(messageId) } returns Optional.of(message)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns message

        val result = chatService.updateMessageAudioCache(TestDataFactory.TEST_SESSION_ID, messageId, audioData, null, null)

        assertNotNull(result)
        verify { chatMessageRepository.save(match {
            it.audioData.contentEquals(audioData) &&
            it.audioVoiceId == null &&
            it.audioSpeed == null
        }) }
    }

    @Test
    fun `updateSessionLesson should navigate to next lesson for course-based session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()

        val lessonContent = mockk<ch.obermuhlner.aitutor.lesson.domain.LessonContent>()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { lessonProgressionService.navigateToNextLesson(TestDataFactory.TEST_SESSION_ID) } returns lessonContent

        val result = chatService.updateSessionLesson(
            TestDataFactory.TEST_SESSION_ID,
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT,
            TestDataFactory.TEST_USER_ID
        )

        assertNotNull(result)
        verify { lessonProgressionService.navigateToNextLesson(TestDataFactory.TEST_SESSION_ID) }
    }

    @Test
    fun `updateSessionLesson should navigate to previous lesson for course-based session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()

        val lessonContent = mockk<ch.obermuhlner.aitutor.lesson.domain.LessonContent>()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { lessonProgressionService.navigateToPreviousLesson(TestDataFactory.TEST_SESSION_ID) } returns lessonContent

        val result = chatService.updateSessionLesson(
            TestDataFactory.TEST_SESSION_ID,
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.PREVIOUS,
            TestDataFactory.TEST_USER_ID
        )

        assertNotNull(result)
        verify { lessonProgressionService.navigateToPreviousLesson(TestDataFactory.TEST_SESSION_ID) }
    }

    @Test
    fun `updateSessionLesson should return null for non-course session`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = null  // Not a course-based session

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.updateSessionLesson(
            TestDataFactory.TEST_SESSION_ID,
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT,
            TestDataFactory.TEST_USER_ID
        )

        assertNull(result)
        verify(exactly = 0) { lessonProgressionService.navigateToNextLesson(any()) }
    }

    @Test
    fun `updateSessionLesson should return null for wrong user`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.updateSessionLesson(
            TestDataFactory.TEST_SESSION_ID,
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT,
            UUID.randomUUID()
        )

        assertNull(result)
        verify(exactly = 0) { lessonProgressionService.navigateToNextLesson(any()) }
    }

    @Test
    fun `updateSessionLesson should return null when session not found`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.updateSessionLesson(
            UUID.randomUUID(),
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT,
            TestDataFactory.TEST_USER_ID
        )

        assertNull(result)
    }

    @Test
    fun `updateSessionLesson should return null when lesson navigation fails`() {
        val session = TestDataFactory.createSessionEntity()
        session.courseTemplateId = UUID.randomUUID()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { lessonProgressionService.navigateToNextLesson(TestDataFactory.TEST_SESSION_ID) } returns null

        val result = chatService.updateSessionLesson(
            TestDataFactory.TEST_SESSION_ID,
            ch.obermuhlner.aitutor.chat.dto.LessonNavigationDirection.NEXT,
            TestDataFactory.TEST_USER_ID
        )

        assertNull(result)
    }

    @Test
    fun `initiateTutorMessage should create welcome message successfully`() {
        val session = TestDataFactory.createSessionEntity()
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Welcome!")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Welcome!",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns emptyList()
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Free, "Initial", 0.0)
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns assistantMessage

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID, "welcome")

        assertNotNull(result)
        assertEquals("Welcome!", result?.content)
        verify { tutorService.respond(any(), match { it.initiationContext == "welcome" }, any(), any(), any(), any(), any(), any(), any(), any()) }
        verify { chatMessageRepository.save(any<ChatMessageEntity>()) }
    }

    @Test
    fun `initiateTutorMessage should return null when session not found`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.initiateTutorMessage(UUID.randomUUID(), TestDataFactory.TEST_USER_ID, "welcome")

        assertNull(result)
        verify(exactly = 0) { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `initiateTutorMessage should return null when user is not owner`() {
        val session = TestDataFactory.createSessionEntity()

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, UUID.randomUUID(), "welcome")

        assertNull(result)
        verify(exactly = 0) { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `initiateTutorMessage should handle re-engagement context`() {
        val session = TestDataFactory.createSessionEntity()
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Welcome back!")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Welcome back!",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns emptyList()
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Resume", 0.5)
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Resume", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns assistantMessage

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID, "reengage")

        assertNotNull(result)
        verify { tutorService.respond(any(), match { it.initiationContext == "reengage" }, any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `initiateTutorMessage should return error message when tutor service fails`() {
        val session = TestDataFactory.createSessionEntity()
        val errorMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "An error occurred")

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns emptyList()
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Free, "Initial", 0.0)
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("API Error")
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns errorMessage

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID, "welcome")

        assertNotNull(result)
        verify { chatMessageRepository.save(match { it.role == MessageRole.ASSISTANT }) }
    }

    @Test
    fun `initiateTutorMessage should update effective phase in Auto mode`() {
        val session = TestDataFactory.createSessionEntity()
        session.conversationPhase = ConversationPhase.Auto
        session.effectivePhase = ConversationPhase.Correction

        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Let's practice!")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Let's practice!",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns emptyList()
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns assistantMessage

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID, "welcome")

        assertNotNull(result)
        // Phase updates are now handled periodically by MetadataEvaluationService
        verify { metadataEvaluationService.evaluateIfNeeded(TestDataFactory.TEST_SESSION_ID, any()) }
    }

    @Test
    fun `initiateTutorMessage should pass due vocabulary count when review mode enabled`() {
        val session = TestDataFactory.createSessionEntity()
        session.vocabularyReviewMode = true

        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Let's review!")

        val tutorResponse = TutorService.TutorResponse(
            reply = "Let's review!",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(TestDataFactory.TEST_SESSION_ID) } returns emptyList()
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Free, "Initial", 0.0)
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { vocabularyReviewService.getDueCount(TestDataFactory.TEST_USER_ID, session.targetLanguageCode) } returns 10L
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default phase", 0.0)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns assistantMessage

        val result = chatService.initiateTutorMessage(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID, "welcome")

        assertNotNull(result)
        verify { vocabularyReviewService.getDueCount(TestDataFactory.TEST_USER_ID, session.targetLanguageCode) }
        verify { tutorService.respond(any(), match { it.vocabularyReviewMode && it.dueVocabularyCount == 10L }, any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `should increment lesson turn count when lesson is active`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentLessonId = "lesson-01"
        session.lessonProgressTurnCount = 5
        session.lessonProgressGoalsCompleted = false

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } answers { firstArg() }

        // Trigger incrementLessonTurnCount by sending a message
        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")
        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default", 0.0)

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        // Verify turn count was incremented
        verify { chatSessionRepository.save(match { session ->
            session.lessonProgressTurnCount == 6
        }) }
    }

    @Test
    fun `should skip incrementing turn count when no active lesson`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentLessonId = null  // No active lesson
        session.lessonProgressTurnCount = 0
        session.lessonProgressGoalsCompleted = false

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } answers { firstArg() }

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")
        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default", 0.0)

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        // Verify lesson progress was not modified
        verify { chatSessionRepository.save(match { it.lessonProgressTurnCount == 0 && it.lessonProgressGoalsCompleted == false }) }
    }

    @Test
    fun `should initialize turn count with default JSON when not set`() {
        val session = TestDataFactory.createSessionEntity()
        session.currentLessonId = "lesson-01"
        session.lessonProgressTurnCount = 0  // Initialized to 0 by default
        session.lessonProgressGoalsCompleted = false  // Initialized to false by default

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } answers { firstArg() }

        val userMessage = TestDataFactory.createMessageEntity(session)
        val assistantMessage = TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")
        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                newVocabulary = emptyList(),
                wordCards = emptyList(),
                characterCards = emptyList()
            )
        )

        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatMessageRepository.countBySessionId(any()) } returns 0L
        every { phaseDecisionService.decidePhase(any(), any()) } returns PhaseDecision(ConversationPhase.Correction, "Default", 0.0)

        chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        // Verify turn count was set to 1
        verify { chatSessionRepository.save(match { session ->
            session.lessonProgressTurnCount == 1
        }) }
    }
}
