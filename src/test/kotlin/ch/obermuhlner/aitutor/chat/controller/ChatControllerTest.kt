package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
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
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

@WebMvcTest(controllers = [ChatController::class])
@AutoConfigureJsonTesters
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class ChatControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var chatService: ChatService

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var chatSessionRepository: ChatSessionRepository

    @MockkBean(relaxed = true)
    private lateinit var chatMessageRepository: ChatMessageRepository

    @MockkBean(relaxed = true)
    private lateinit var tutorService: TutorService

    @MockkBean(relaxed = true)
    private lateinit var vocabularyService: VocabularyService

    @MockkBean(relaxed = true)
    private lateinit var catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
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

        every { authorizationService.requireAccessToUser(request.userId) } returns Unit
        every { chatService.createSession(any()) } returns response

        mockMvc.perform(
            post("/api/v1/chat/sessions")
                .with(csrf())
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
    @WithMockUser
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

        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
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
    @WithMockUser
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

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getSessionWithMessages(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID) } returns response

        mockMvc.perform(
            get("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.session.id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$.messages[0].content").value("Hola"))
    }

    @Test
    @WithMockUser
    fun `should return 404 when session not found`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getSessionWithMessages(any(), any()) } returns null

        mockMvc.perform(
            get("/api/v1/chat/sessions/${UUID.randomUUID()}")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
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

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.sendMessage(any(), any(), any(), any()) } returns messageResponse

        mockMvc.perform(
            post("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content": "Hola"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ASSISTANT"))
            .andExpect(jsonPath("$.content").value("Hola! Como estas?"))
    }

    @Test
    @WithMockUser
    fun `should delete session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.deleteSession(any(), any()) } returns true

        mockMvc.perform(
            delete("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}")
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { chatService.deleteSession(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID) }
    }

    @Test
    @WithMockUser
    fun `should update session topic`() {
        val sessionResponse = SessionResponse(
            id = TestDataFactory.TEST_SESSION_ID,
            userId = TestDataFactory.TEST_USER_ID,
            tutorName = "TestTutor",
            tutorPersona = "patient coach",
            tutorDomain = "general conversation",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            currentTopic = "cooking",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, "cooking", TestDataFactory.TEST_USER_ID) } returns sessionResponse

        mockMvc.perform(
            patch("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/topic")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentTopic": "cooking"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentTopic").value("cooking"))

        verify(exactly = 1) { chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, "cooking", TestDataFactory.TEST_USER_ID) }
    }

    @Test
    @WithMockUser
    fun `should update session topic to null`() {
        val sessionResponse = SessionResponse(
            id = TestDataFactory.TEST_SESSION_ID,
            userId = TestDataFactory.TEST_USER_ID,
            tutorName = "TestTutor",
            tutorPersona = "patient coach",
            tutorDomain = "general conversation",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1,
            currentTopic = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, null, TestDataFactory.TEST_USER_ID) } returns sessionResponse

        mockMvc.perform(
            patch("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/topic")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentTopic": null}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentTopic").doesNotExist())

        verify(exactly = 1) { chatService.updateSessionTopic(TestDataFactory.TEST_SESSION_ID, null, TestDataFactory.TEST_USER_ID) }
    }

    @Test
    @WithMockUser
    fun `should return 404 when updating topic for non-existent session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.updateSessionTopic(any(), any(), any()) } returns null

        mockMvc.perform(
            patch("/api/v1/chat/sessions/${UUID.randomUUID()}/topic")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentTopic": "cooking"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `should get topic history`() {
        val topicHistory = TopicHistoryResponse(
            currentTopic = "cooking",
            pastTopics = listOf("travel", "sports", "music")
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID) } returns topicHistory

        mockMvc.perform(
            get("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/topics/history")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentTopic").value("cooking"))
            .andExpect(jsonPath("$.pastTopics").isArray)
            .andExpect(jsonPath("$.pastTopics.length()").value(3))
            .andExpect(jsonPath("$.pastTopics[0]").value("travel"))

        verify(exactly = 1) { chatService.getTopicHistory(TestDataFactory.TEST_SESSION_ID, TestDataFactory.TEST_USER_ID) }
    }

    @Test
    @WithMockUser
    fun `should return 404 when getting topic history for non-existent session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getTopicHistory(any(), any()) } returns null

        mockMvc.perform(
            get("/api/v1/chat/sessions/${UUID.randomUUID()}/topics/history")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `should update session phase`() {
        val sessionResponse = SessionResponse(
            id = TestDataFactory.TEST_SESSION_ID,
            userId = TestDataFactory.TEST_USER_ID,
            tutorName = "TestTutor",
            tutorPersona = "patient coach",
            tutorDomain = "general conversation",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Drill,
            estimatedCEFRLevel = CEFRLevel.A1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, TestDataFactory.TEST_USER_ID) } returns sessionResponse

        mockMvc.perform(
            patch("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/phase")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phase": "Drill"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.conversationPhase").value("Drill"))

        verify(exactly = 1) { chatService.updateSessionPhase(TestDataFactory.TEST_SESSION_ID, ConversationPhase.Drill, TestDataFactory.TEST_USER_ID) }
    }

    @Test
    @WithMockUser
    fun `should return 404 when updating phase for non-existent session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.updateSessionPhase(any(), any(), any()) } returns null

        mockMvc.perform(
            patch("/api/v1/chat/sessions/${UUID.randomUUID()}/phase")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"phase": "Drill"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `should get active learning sessions`() {
        val progressResponse = SessionProgressResponse(
            messageCount = 10,
            vocabularyCount = 5,
            daysActive = 3
        )
        val sessionWithProgress = SessionWithProgressResponse(
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
            progress = progressResponse
        )

        every { authorizationService.resolveUserId(null) } returns TestDataFactory.TEST_USER_ID
        every { chatService.getActiveLearningSessions(TestDataFactory.TEST_USER_ID) } returns listOf(sessionWithProgress)

        mockMvc.perform(
            get("/api/v1/chat/sessions/active")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].session.id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$[0].progress.messageCount").value(10))
            .andExpect(jsonPath("$[0].progress.vocabularyCount").value(5))
    }

    @Test
    @WithMockUser
    fun `should get session progress`() {
        val session = SessionResponse(
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
        val progress = SessionProgressResponse(
            messageCount = 15,
            vocabularyCount = 8,
            daysActive = 4
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getSession(TestDataFactory.TEST_SESSION_ID) } returns session
        every { chatService.getSessionProgress(TestDataFactory.TEST_SESSION_ID) } returns progress

        mockMvc.perform(
            get("/api/v1/chat/sessions/${TestDataFactory.TEST_SESSION_ID}/progress")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.session.id").value(TestDataFactory.TEST_SESSION_ID.toString()))
            .andExpect(jsonPath("$.progress.messageCount").value(15))
            .andExpect(jsonPath("$.progress.vocabularyCount").value(8))
    }

    @Test
    @WithMockUser
    fun `should return 404 when getting progress for non-existent session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.getSession(any()) } returns null

        mockMvc.perform(
            get("/api/v1/chat/sessions/${UUID.randomUUID()}/progress")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `should return 404 when sending message to non-existent session`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { chatService.sendMessage(any(), any(), any(), any()) } returns null

        mockMvc.perform(
            post("/api/v1/chat/sessions/${UUID.randomUUID()}/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content": "Hola"}""")
        )
            .andExpect(status().isNotFound)
    }
}
