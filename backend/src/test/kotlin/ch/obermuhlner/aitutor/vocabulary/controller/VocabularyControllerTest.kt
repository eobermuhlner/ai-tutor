package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.chat.service.ChatService
import ch.obermuhlner.aitutor.fixtures.TestDataFactory
import ch.obermuhlner.aitutor.tutor.service.TutorService
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
    private lateinit var vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService

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
        every { item1.nextReviewAt } returns null
        every { item1.reviewStage } returns 0

        val item2 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item2.id } returns UUID.randomUUID()
        every { item2.lemma } returns "perro"
        every { item2.lang } returns "Spanish"
        every { item2.exposures } returns 2
        every { item2.lastSeenAt } returns Instant.now()
        every { item2.createdAt } returns Instant.now()
        every { item2.conceptName } returns "dog"
        every { item2.nextReviewAt } returns null
        every { item2.reviewStage } returns 0

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
        every { item.nextReviewAt } returns null
        every { item.reviewStage } returns 0

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

    @Test
    @WithMockUser
    fun `getUserVocabulary should return multiple items with details`() {
        val item1 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item1.id } returns UUID.randomUUID()
        every { item1.lemma } returns "hello"
        every { item1.lang } returns "Spanish"
        every { item1.exposures } returns 3
        every { item1.lastSeenAt } returns Instant.now()
        every { item1.createdAt } returns Instant.now()
        every { item1.conceptName } returns "hello"
        every { item1.nextReviewAt } returns null
        every { item1.reviewStage } returns 0

        val item2 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item2.id } returns UUID.randomUUID()
        every { item2.lemma } returns "goodbye"
        every { item2.lang } returns "Spanish"
        every { item2.exposures } returns 1
        every { item2.lastSeenAt } returns Instant.now()
        every { item2.createdAt } returns Instant.now()
        every { item2.conceptName } returns "goodbye"
        every { item2.nextReviewAt } returns null
        every { item2.reviewStage } returns 0

        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, null) } returns listOf(item1, item2)

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].lemma").value("hello"))
            .andExpect(jsonPath("$[0].exposures").value(3))
            .andExpect(jsonPath("$[1].lemma").value("goodbye"))
    }

    @Test
    @WithMockUser
    fun `getUserVocabulary should filter by language correctly`() {
        val spanishItem = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { spanishItem.id } returns UUID.randomUUID()
        every { spanishItem.lemma } returns "hola"
        every { spanishItem.lang } returns "Spanish"
        every { spanishItem.exposures } returns 1
        every { spanishItem.lastSeenAt } returns Instant.now()
        every { spanishItem.createdAt } returns Instant.now()
        every { spanishItem.conceptName } returns "hello"
        every { spanishItem.nextReviewAt } returns null
        every { spanishItem.reviewStage } returns 0

        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, "Spanish") } returns listOf(spanishItem)

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lang").value("Spanish"))
    }

    @Test
    @WithMockUser
    fun `getVocabularyItem should return item with multiple contexts`() {
        val itemId = UUID.randomUUID()
        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.id } returns itemId
        every { item.lemma } returns "hello"
        every { item.lang } returns "Spanish"
        every { item.exposures } returns 2
        every { item.lastSeenAt } returns Instant.now()
        every { item.createdAt } returns Instant.now()
        every { item.conceptName } returns "hello"
        every { item.nextReviewAt } returns null
        every { item.reviewStage } returns 0

        val context1 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity>()
        every { context1.context } returns "Hello world"
        every { context1.turnId } returns UUID.randomUUID()

        val context2 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity>()
        every { context2.context } returns "Hello friend"
        every { context2.turnId } returns UUID.randomUUID()

        val result = ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService.VocabularyItemWithContexts(
            item = item,
            contexts = listOf(context1, context2)
        )

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemWithContexts(itemId, TestDataFactory.TEST_USER_ID) } returns result

        mockMvc.perform(get("/api/v1/vocabulary/$itemId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lemma").value("hello"))
            .andExpect(jsonPath("$.contexts").isArray)
            .andExpect(jsonPath("$.contexts.length()").value(2))
            .andExpect(jsonPath("$.contexts[0].context").value("Hello world"))
            .andExpect(jsonPath("$.contexts[1].context").value("Hello friend"))
    }

    @Test
    @WithMockUser
    fun `getUserVocabulary should return empty list when no items exist`() {
        every { authorizationService.resolveUserId(TestDataFactory.TEST_USER_ID) } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getUserVocabulary(TestDataFactory.TEST_USER_ID, null) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/vocabulary")
                .param("userId", TestDataFactory.TEST_USER_ID.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @WithMockUser
    fun `getVocabularyItem should return 404 when item does not exist`() {
        val itemId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemWithContexts(itemId, TestDataFactory.TEST_USER_ID) } returns null

        mockMvc.perform(get("/api/v1/vocabulary/$itemId"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `getDueVocabulary should return due items`() {
        val now = Instant.now()
        val item1 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item1.id } returns UUID.randomUUID()
        every { item1.lemma } returns "hablar"
        every { item1.lang } returns "Spanish"
        every { item1.exposures } returns 1
        every { item1.lastSeenAt } returns now
        every { item1.createdAt } returns now
        every { item1.conceptName } returns null
        every { item1.nextReviewAt } returns now.minusSeconds(3600)
        every { item1.reviewStage } returns 1

        val item2 = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item2.id } returns UUID.randomUUID()
        every { item2.lemma } returns "comer"
        every { item2.lang } returns "Spanish"
        every { item2.exposures } returns 2
        every { item2.lastSeenAt } returns now
        every { item2.createdAt } returns now
        every { item2.conceptName } returns null
        every { item2.nextReviewAt } returns now.minusSeconds(7200)
        every { item2.reviewStage } returns 2

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyReviewService.getDueVocabulary(TestDataFactory.TEST_USER_ID, "Spanish", 20) } returns listOf(item1, item2)

        mockMvc.perform(
            get("/api/v1/vocabulary/due")
                .param("lang", "Spanish")
                .param("limit", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].lemma").value("hablar"))
            .andExpect(jsonPath("$[0].isDue").value(true))
            .andExpect(jsonPath("$[0].reviewStage").value(1))
            .andExpect(jsonPath("$[1].lemma").value("comer"))
            .andExpect(jsonPath("$[1].reviewStage").value(2))
    }

    @Test
    @WithMockUser
    fun `getDueVocabulary should return empty list when no items due`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyReviewService.getDueVocabulary(TestDataFactory.TEST_USER_ID, "Spanish", 20) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/vocabulary/due")
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @WithMockUser
    fun `getDueCount should return count of due items`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyReviewService.getDueCount(TestDataFactory.TEST_USER_ID, "Spanish") } returns 15L

        mockMvc.perform(
            get("/api/v1/vocabulary/due/count")
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(15))
    }

    @Test
    @WithMockUser
    fun `getDueCount should return 0 when no items due`() {
        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyReviewService.getDueCount(TestDataFactory.TEST_USER_ID, "Spanish") } returns 0L

        mockMvc.perform(
            get("/api/v1/vocabulary/due/count")
                .param("lang", "Spanish")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    @WithMockUser
    fun `recordReview should record successful review`() {
        val itemId = UUID.randomUUID()
        val now = Instant.now()

        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.id } returns itemId
        every { item.userId } returns TestDataFactory.TEST_USER_ID
        every { item.lemma } returns "hablar"
        every { item.lang } returns "Spanish"
        every { item.exposures } returns 1
        every { item.lastSeenAt } returns now
        every { item.createdAt } returns now
        every { item.conceptName } returns null
        every { item.nextReviewAt } returns now.plusSeconds(86400)
        every { item.reviewStage } returns 2

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemById(itemId) } returns item
        every { vocabularyReviewService.recordReview(itemId, true) } returns item

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vocabulary/$itemId/review")
                .contentType("application/json")
                .content("""{"success": true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lemma").value("hablar"))
            .andExpect(jsonPath("$.reviewStage").value(2))
    }

    @Test
    @WithMockUser
    fun `recordReview should record failed review`() {
        val itemId = UUID.randomUUID()
        val now = Instant.now()

        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.id } returns itemId
        every { item.userId } returns TestDataFactory.TEST_USER_ID
        every { item.lemma } returns "hablar"
        every { item.lang } returns "Spanish"
        every { item.exposures } returns 1
        every { item.lastSeenAt } returns now
        every { item.createdAt } returns now
        every { item.conceptName } returns null
        every { item.nextReviewAt } returns now.plusSeconds(86400)
        every { item.reviewStage } returns 0

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemById(itemId) } returns item
        every { vocabularyReviewService.recordReview(itemId, false) } returns item

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vocabulary/$itemId/review")
                .contentType("application/json")
                .content("""{"success": false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lemma").value("hablar"))
            .andExpect(jsonPath("$.reviewStage").value(0))
    }

    @Test
    @WithMockUser
    fun `recordReview should return 403 when user does not own item`() {
        val itemId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()

        val item = io.mockk.mockk<ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity>()
        every { item.userId } returns otherUserId

        every { authorizationService.getCurrentUserId() } returns TestDataFactory.TEST_USER_ID
        every { vocabularyQueryService.getVocabularyItemById(itemId) } returns item

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/vocabulary/$itemId/review")
                .contentType("application/json")
                .content("""{"success": true}""")
        )
            .andExpect(status().isForbidden)
    }
}
