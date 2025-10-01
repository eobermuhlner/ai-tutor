package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ConversationPhase
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

@WebMvcTest(controllers = [ChatController::class])
@AutoConfigureJsonTesters
class ChatControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var chatService: ChatService

    @MockkBean(relaxed = true)
    private lateinit var chatSessionRepository: ChatSessionRepository

    @MockkBean(relaxed = true)
    private lateinit var chatMessageRepository: ChatMessageRepository

    @MockkBean(relaxed = true)
    private lateinit var tutorService: TutorService

    @MockkBean(relaxed = true)
    private lateinit var vocabularyService: VocabularyService

    @Test
    fun `should create chat session with valid request`() {
        val request = TestDataFactory.createSessionRequest()
        val response = SessionResponse(
            id = TestDataFactory.TEST_SESSION_ID,
            userId = request.userId,
            tutorName = request.tutorName,
            tutorPersona = request.tutorPersona,
            tutorDomain = request.tutorDomain,
            sourceLanguageCode = request.sourceLanguageCode,
            targetLanguageCode = request.targetLanguageCode,
            conversationPhase = ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { chatService.createSession(any()) } returns response

        mockMvc.perform(
            post("/api/v1/chat/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": "${request.userId}",
                        "tutorName": "${request.tutorName}",
                        "sourceLanguageCode": "${request.sourceLanguageCode}",
                        "targetLanguageCode": "${request.targetLanguageCode}",
                        "estimatedCEFRLevel": "A1"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$.tutorName").value("TestTutor"))
            .andExpect(jsonPath("$.sourceLanguageCode").value("en"))

        verify(exactly = 1) { chatService.createSession(any()) }
    }

    @Test
    fun `should get user sessions`() {
        val sessions = listOf(
            SessionResponse(
                id = TestDataFactory.TEST_SESSION_ID,
                userId = TestDataFactory.TEST_USER_ID,
                tutorName = "TestTutor",
                tutorPersona = "patient coach",
                tutorDomain = "general",
                sourceLanguageCode = "en",
                targetLanguageCode = "es",
                conversationPhase = ConversationPhase.Free,
                estimatedCEFRLevel = CEFRLevel.A1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        every { chatService.getUserSessions(TestDataFactory.TEST_USER_ID) } returns sessions

        mockMvc.perform(
            get("/api/v1/chat/sessions")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$[0].tutorName").value("TestTutor"))
    }

    @Test
    fun `should get session with messages`() {
        val response = SessionWithMessagesResponse(
            session = SessionResponse(
                id = TestDataFactory.TEST_SESSION_ID,
                userId = TestDataFactory.TEST_USER_ID,
                tutorName = "TestTutor",
                tutorPersona = "patient coach",
                tutorDomain = "general",
                sourceLanguageCode = "en",
                targetLanguageCode = "es",
                conversationPhase = ConversationPhase.Free,
                estimatedCEFRLevel = CEFRLevel.A1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            messages = listOf(
                MessageResponse(
                    id = UUID.randomUUID(),
                    role = "USER",
                    content = "Hola",
                    corrections = null,
                    newVocabulary = null,
                    wordCards = null,
                    createdAt = Instant.now()
                )
            )
        )

        every { chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID) } returns response

        mockMvc.perform(
            get("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.session.id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$.messages[0].content").value("Hola"))
    }

    @Test
    fun `should return 404 when session not found`() {
        every { chatService.getSessionWithMessages(any()) } returns null

        mockMvc.perform(
            get("/api/v1/chat/sessions/${UUID.randomUUID()}")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should send message to session`() {
        val messageResponse = MessageResponse(
            id = UUID.randomUUID(),
            role = "ASSISTANT",
            content = "Hola! Como estas?",
            corrections = null,
            newVocabulary = null,
            wordCards = null,
            createdAt = Instant.now()
        )

        every { chatService.sendMessage(any(), any(), any()) } returns messageResponse

        mockMvc.perform(
            post("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content": "Hola"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ASSISTANT"))
            .andExpect(jsonPath("$.content").value("Hola! Como estas?"))
    }

    @Test
    fun `should delete session`() {
        every { chatService.deleteSession(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}")
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { chatService.deleteSession(TestDataFactory.TEST_SESSION_ID) }
    }
}
