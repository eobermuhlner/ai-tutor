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
    fun `should get vocabulary item with contexts when found`() {
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

    @Test
    @WithMockUser
    fun `should get user vocabulary with items`() {
        every { authorizationService.resolveUserId(null) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, null) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/vocabulary")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
    }

    @Test
    @WithMockUser
    fun `should return vocabulary item with contexts when found`() {
        val itemId = UUID.randomUUID()
        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.id } returns itemId
        every { item.lemma } returns "hola"
        every { item.lang } returns "Spanish"
        every { item.exposures } returns 3
        every { item.lastSeenAt } returns Instant.now()
        every { item.createdAt } returns Instant.now()
        every { item.conceptName } returns "hello"

        val context = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity>()
        every { context.context } returns "Hola, ¿cómo estás?"
        every { context.turnId } returns UUID.randomUUID()

        val result = ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService.VocabularyItemWithContexts(
            item = item,
            contexts = listOf(context)
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemWithContexts(itemId, TestDataFactory.TEST_USER_ID) } returns result

        mockMvc.perform(
            get("/api/v1/vocabulary/$itemId")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.lemma").value("hola"))
            .andExpect(jsonPath("$.lang").value("Spanish"))
            .andExpect(jsonPath("$.exposures").value(3))
            .andExpect(jsonPath("$.contexts").isArray)
            .andExpect(jsonPath("$.contexts[0].context").value("Hola, ¿cómo estás?"))
            .andExpect(jsonPath("$.imageUrl").value("/api/v1/images/concept/hello/data"))
    }

    @Test
    @WithMockUser
    fun `should return vocabulary items with concept images`() {
        val item1 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item1.id } returns UUID.randomUUID()
        every { item1.lemma } returns "manzana"
        every { item1.lang } returns "Spanish"
        every { item1.exposures } returns 5
        every { item1.lastSeenAt } returns Instant.now()
        every { item1.createdAt } returns Instant.now()
        every { item1.conceptName } returns "apple"

        val item2 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item2.id } returns UUID.randomUUID()
        every { item2.lemma } returns "perro"
        every { item2.lang } returns "Spanish"
        every { item2.exposures } returns 2
        every { item2.lastSeenAt } returns Instant.now()
        every { item2.createdAt } returns Instant.now()
        every { item2.conceptName } returns "dog"

        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, "Spanish") } returns listOf(item1, item2)

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].lemma").value("manzana"))
            .andExpect(jsonPath("$[0].imageUrl").value("/api/v1/images/concept/apple/data"))
            .andExpect(jsonPath("$[1].lemma").value("perro"))
            .andExpect(jsonPath("$[1].imageUrl").value("/api/v1/images/concept/dog/data"))
    }

    @Test
    @WithMockUser
    fun `should handle vocabulary items without concept images`() {
        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.id } returns UUID.randomUUID()
        every { item.lemma } returns "word"
        every { item.lang } returns "English"
        every { item.exposures } returns 1
        every { item.lastSeenAt } returns Instant.now()
        every { item.createdAt } returns null // Test null createdAt fallback
        every { item.conceptName } returns null // No concept image

        every { authorizationService.resolveUserId(null) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, null) } returns listOf(item)

        mockMvc.perform(
            get("/api/v1/vocabulary")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$[0].lemma").value("word"))
            .andExpect(jsonPath("$[0].imageUrl").doesNotExist())
    }
}
