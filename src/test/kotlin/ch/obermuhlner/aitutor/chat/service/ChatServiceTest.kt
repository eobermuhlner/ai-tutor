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
    private lateinit var vocabularyService: VocabularyService
    private lateinit var vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService
    private lateinit var phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService
    private lateinit var topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService
    private lateinit var catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        chatSessionRepository = mockk()
        chatMessageRepository = mockk()
        tutorService = mockk()
        vocabularyService = mockk()
        vocabularyReviewService = mockk()
        phaseDecisionService = mockk()
        topicDecisionService = mockk()
        catalogService = mockk()
        objectMapper = jacksonObjectMapper()

        chatService = ChatService(
            chatSessionRepository,
            chatMessageRepository,
            tutorService,
            vocabularyService,
            vocabularyReviewService,
            phaseDecisionService,
            topicDecisionService,
            catalogService,
            objectMapper
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
                conversationState = ConversationState(
                    phase = ConversationPhase.Drill,
                    estimatedCEFRLevel = CEFRLevel.A2
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any()) } returns tutorResponse
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
                conversationState = ConversationState(
                    phase = ConversationPhase.Correction,
                    estimatedCEFRLevel = CEFRLevel.A1,
                    currentTopic = "new-topic"
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
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
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision("new-topic", 10, "Topic changed", emptyList())
        every { topicDecisionService.countTurnsInRecentMessages(any()) } returns 10
        every { topicDecisionService.shouldArchiveTopic(any(), any()) } returns true
        every { tutorService.respond(any(), any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test", TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        verify { chatSessionRepository.save(match { it.currentTopic == "new-topic" }) }
    }

    @Test
    fun `should not update user-controlled phase when not in Auto mode`() {
        val session = TestDataFactory.createSessionEntity()
        session.conversationPhase = ConversationPhase.Correction
        session.effectivePhase = ConversationPhase.Correction

        val tutorResponse = TutorService.TutorResponse(
            reply = "Reply",
            conversationResponse = ConversationResponse(
                conversationState = ConversationState(
                    phase = ConversationPhase.Drill,  // LLM suggests Drill
                    estimatedCEFRLevel = CEFRLevel.A2,
                    currentTopic = null
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returnsMany listOf(
            TestDataFactory.createMessageEntity(session),
            TestDataFactory.createMessageEntity(session, MessageRole.ASSISTANT, "Reply")
        )
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any()) } returns tutorResponse
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
                conversationState = ch.obermuhlner.aitutor.tutor.domain.ConversationState(
                    phase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Free,
                    estimatedCEFRLevel = ch.obermuhlner.aitutor.core.model.CEFRLevel.A1
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns ch.obermuhlner.aitutor.tutor.service.TopicDecision(null, 0, "Free conversation", emptyList())
        every { vocabularyReviewService.getDueCount(any(), any()) } returns 15L
        every { tutorService.respond(any(), any(), any(), any(), any(), any()) } returns tutorResponse
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
                conversationState = ch.obermuhlner.aitutor.tutor.domain.ConversationState(
                    phase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Free,
                    estimatedCEFRLevel = ch.obermuhlner.aitutor.core.model.CEFRLevel.A1
                ),
                corrections = emptyList(),
                newVocabulary = emptyList()
            )
        )

        every { chatSessionRepository.findById(TestDataFactory.TEST_SESSION_ID) } returns Optional.of(session)
        every { chatMessageRepository.save(any<ChatMessageEntity>()) } returns userMessage andThen assistantMessage
        every { chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(any()) } returns emptyList()
        every { topicDecisionService.decideTopic(any(), any(), any(), any()) } returns ch.obermuhlner.aitutor.tutor.service.TopicDecision(null, 0, "Free conversation", emptyList())
        every { tutorService.respond(any(), any(), any(), any(), any(), any()) } returns tutorResponse
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

        val tutor = mockk<ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity>()
        every { tutor.name } returns "Maria"
        every { tutor.personaEnglish } returns "friendly"
        every { tutor.domainEnglish } returns "general"
        every { tutor.teachingStyle } returns ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive
        every { tutor.targetLanguageCode } returns "es"

        val savedSession = TestDataFactory.createSessionEntity()

        every { catalogService.getCourseById(courseId) } returns course
        every { catalogService.getTutorById(tutorId) } returns tutor
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns savedSession

        val result = chatService.createSessionFromCourse(
            TestDataFactory.TEST_USER_ID,
            courseId,
            tutorId,
            "en",
            "My Custom Course"
        )

        assertNotNull(result)
        verify { catalogService.getCourseById(courseId) }
        verify { catalogService.getTutorById(tutorId) }
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
        every { catalogService.getTutorById(tutorId) } returns null

        val result = chatService.createSessionFromCourse(
            TestDataFactory.TEST_USER_ID,
            courseId,
            tutorId,
            "en"
        )

        assertNull(result)
        verify { catalogService.getTutorById(tutorId) }
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

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.newVocabulary)
        assertEquals(1, result?.messages?.get(0)?.newVocabulary?.size)
        assertEquals("casa", result?.messages?.get(0)?.newVocabulary?.get(0)?.lemma)
        assertEquals("/api/v1/images/concept/house/data", result?.messages?.get(0)?.newVocabulary?.get(0)?.imageUrl)
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

        val result = chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID)

        assertNotNull(result)
        assertEquals(1, result?.messages?.size)
        assertNotNull(result?.messages?.get(0)?.wordCards)
        assertEquals(1, result?.messages?.get(0)?.wordCards?.size)
        assertEquals("House", result?.messages?.get(0)?.wordCards?.get(0)?.titleSourceLanguage)
        assertEquals("/api/v1/images/concept/house/data", result?.messages?.get(0)?.wordCards?.get(0)?.imageUrl)
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
}
