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
        objectMapper = ObjectMapper()

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
}
