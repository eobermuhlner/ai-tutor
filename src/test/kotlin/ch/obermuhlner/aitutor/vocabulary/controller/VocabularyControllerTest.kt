package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyContextResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemWithContextsResponse
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

@WebMvcTest(controllers = [VocabularyController::class])
@AutoConfigureJsonTesters
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class VocabularyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var vocabularyQueryService: VocabularyQueryService

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var vocabularyItemRepository: VocabularyItemRepository

    @MockkBean(relaxed = true)
    private lateinit var vocabularyContextRepository: VocabularyContextRepository

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

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `should get user vocabulary without language filter`() {
        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, null) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
    }

    @Test
    @WithMockUser
    fun `should get user vocabulary with language filter`() {
        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every {
            vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, "Spanish")
        } returns emptyList()

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
    }

    @Test
    @WithMockUser
    fun `should get vocabulary item with contexts`() {
        val itemId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemWithContexts(itemId, TestDataFactory.TEST_USER_ID) } returns null

        mockMvc.perform(
            get("/api/v1/vocabulary/$itemId")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `should return 404 when vocabulary item not found`() {
        val nonExistentId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemWithContexts(nonExistentId, TestDataFactory.TEST_USER_ID) } returns null

        mockMvc.perform(
            get("/api/v1/vocabulary/$nonExistentId")
        )
            .andExpect(status().isNotFound)
    }
}
