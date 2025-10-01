package ch.obermuhlner.aitutor.chat.service

import ch.obermuhlner.aitutor.chat.domain.ChatMessageEntity
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.domain.MessageRole
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.core.model.*
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ChatServiceTest {

    private lateinit var chatService: ChatService
    private lateinit var chatSessionRepository: ChatSessionRepository
    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var tutorService: TutorService
    private lateinit var vocabularyService: VocabularyService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        chatSessionRepository = mockk()
        chatMessageRepository = mockk()
        tutorService = mockk()
        vocabularyService = mockk()
        objectMapper = ObjectMapper()

        chatService = ChatService(
            chatSessionRepository,
            chatMessageRepository,
            tutorService,
            vocabularyService,
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
        every { tutorService.respond(any(), any(), any(), any(), any()) } returns tutorResponse
        every { chatSessionRepository.save(any<ChatSessionEntity>()) } returns session

        val result = chatService.sendMessage(TestDataFactory.TEST_SESSION_ID, "Test message")

        assertNotNull(result)
        assertEquals("ASSISTANT", result?.role)
        assertEquals("Test reply", result?.content)

        verify { chatSessionRepository.save(any<ChatSessionEntity>()) }
        verify { chatMessageRepository.save(any<ChatMessageEntity>()) }
    }

    @Test
    fun `should return null when sending message to non-existent session`() {
        every { chatSessionRepository.findById(any()) } returns Optional.empty()

        val result = chatService.sendMessage(UUID.randomUUID(), "Test message")

        assertNull(result)
    }

    @Test
    fun `should delete session`() {
        every { chatSessionRepository.deleteById(any()) } just Runs

        chatService.deleteSession(TestDataFactory.TEST_SESSION_ID)

        verify(exactly = 1) { chatSessionRepository.deleteById(TestDataFactory.TEST_SESSION_ID) }
    }
}
