package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

class VocabularyReviewServiceTest {

    private lateinit var vocabularyItemRepository: VocabularyItemRepository
    private lateinit var vocabularyReviewService: VocabularyReviewService

    @BeforeEach
    fun setup() {
        vocabularyItemRepository = mockk()
        vocabularyReviewService = VocabularyReviewService(vocabularyItemRepository)
    }

    @Test
    fun `getDueVocabulary returns due items ordered by nextReviewAt`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val now = Instant.now()

        val item1 = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = userId,
            lang = lang,
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = now,
            nextReviewAt = now.minus(2, ChronoUnit.DAYS),
            reviewStage = 1
        )

        val item2 = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = userId,
            lang = lang,
            lemma = "comer",
            exposures = 1,
            lastSeenAt = now,
            nextReviewAt = now.minus(1, ChronoUnit.DAYS),
            reviewStage = 0
        )

        every {
            vocabularyItemRepository.findDueForReview(userId, lang, any(), PageRequest.of(0, 20))
        } returns listOf(item1, item2)

        val result = vocabularyReviewService.getDueVocabulary(userId, lang, 20)

        assertEquals(2, result.size)
        assertEquals("hablar", result[0].lemma)
        assertEquals("comer", result[1].lemma)
        verify { vocabularyItemRepository.findDueForReview(userId, lang, any(), PageRequest.of(0, 20)) }
    }

    @Test
    fun `getDueVocabulary returns empty list when no items due`() {
        val userId = UUID.randomUUID()
        val lang = "es"

        every {
            vocabularyItemRepository.findDueForReview(userId, lang, any(), PageRequest.of(0, 20))
        } returns emptyList()

        val result = vocabularyReviewService.getDueVocabulary(userId, lang)

        assertEquals(0, result.size)
    }

    @Test
    fun `getDueVocabulary respects limit parameter`() {
        val userId = UUID.randomUUID()
        val lang = "es"

        every {
            vocabularyItemRepository.findDueForReview(userId, lang, any(), PageRequest.of(0, 5))
        } returns emptyList()

        vocabularyReviewService.getDueVocabulary(userId, lang, 5)

        verify { vocabularyItemRepository.findDueForReview(userId, lang, any(), PageRequest.of(0, 5)) }
    }

    @Test
    fun `getDueCount returns correct count`() {
        val userId = UUID.randomUUID()
        val lang = "es"

        every { vocabularyItemRepository.countDueForReview(userId, lang, any()) } returns 15L

        val result = vocabularyReviewService.getDueCount(userId, lang)

        assertEquals(15L, result)
        verify { vocabularyItemRepository.countDueForReview(userId, lang, any()) }
    }

    @Test
    fun `getDueCount returns 0 when no items due`() {
        val userId = UUID.randomUUID()
        val lang = "es"

        every { vocabularyItemRepository.countDueForReview(userId, lang, any()) } returns 0L

        val result = vocabularyReviewService.getDueCount(userId, lang)

        assertEquals(0L, result)
    }

    @Test
    fun `recordReview advances stage on success`() {
        val itemId = UUID.randomUUID()
        val item = VocabularyItemEntity(
            id = itemId,
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = Instant.now(),
            reviewStage = 2,
            nextReviewAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )

        every { vocabularyItemRepository.findById(itemId) } returns Optional.of(item)
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val result = vocabularyReviewService.recordReview(itemId, success = true)

        assertEquals(3, result.reviewStage)
        assertNotNull(result.nextReviewAt)
        assertTrue(result.nextReviewAt!!.isAfter(Instant.now()))
        verify { vocabularyItemRepository.save(item) }
    }

    @Test
    fun `recordReview resets stage to 0 on failure`() {
        val itemId = UUID.randomUUID()
        val item = VocabularyItemEntity(
            id = itemId,
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = Instant.now(),
            reviewStage = 3,
            nextReviewAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )

        every { vocabularyItemRepository.findById(itemId) } returns Optional.of(item)
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val result = vocabularyReviewService.recordReview(itemId, success = false)

        assertEquals(0, result.reviewStage)
        assertNotNull(result.nextReviewAt)
        verify { vocabularyItemRepository.save(item) }
    }

    @Test
    fun `recordReview does not exceed maximum stage`() {
        val itemId = UUID.randomUUID()
        val item = VocabularyItemEntity(
            id = itemId,
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = Instant.now(),
            reviewStage = 5, // Already at max stage (intervals has 6 elements: 0-5)
            nextReviewAt = Instant.now().minus(1, ChronoUnit.DAYS)
        )

        every { vocabularyItemRepository.findById(itemId) } returns Optional.of(item)
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val result = vocabularyReviewService.recordReview(itemId, success = true)

        assertEquals(5, result.reviewStage) // Should stay at 5, not go to 6
        verify { vocabularyItemRepository.save(item) }
    }

    @Test
    fun `recordReview schedules correct intervals for each stage`() {
        val itemId = UUID.randomUUID()
        val now = Instant.now()

        // Test stage 0 -> 1 (1 day interval)
        val item = VocabularyItemEntity(
            id = itemId,
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = now,
            reviewStage = 0,
            nextReviewAt = now
        )

        every { vocabularyItemRepository.findById(itemId) } returns Optional.of(item)
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val result = vocabularyReviewService.recordReview(itemId, success = true)

        assertEquals(1, result.reviewStage)
        // Next review should be ~3 days from now (interval for stage 1)
        val expectedNextReview = now.plus(3, ChronoUnit.DAYS)
        val diff = ChronoUnit.SECONDS.between(expectedNextReview, result.nextReviewAt)
        assertTrue(Math.abs(diff) < 2, "Next review should be ~3 days from now, diff was $diff seconds")
    }

    @Test
    fun `recordReview throws exception when item not found`() {
        val itemId = UUID.randomUUID()

        every { vocabularyItemRepository.findById(itemId) } returns Optional.empty()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            vocabularyReviewService.recordReview(itemId, success = true)
        }

        assertEquals("Vocabulary item not found: $itemId", exception.message)
    }

    @Test
    fun `scheduleInitialReview sets nextReviewAt for new item`() {
        val item = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = Instant.now(),
            nextReviewAt = null,
            reviewStage = 0
        )

        vocabularyReviewService.scheduleInitialReview(item)

        assertNotNull(item.nextReviewAt)
        assertEquals(0, item.reviewStage)
        assertTrue(item.nextReviewAt!!.isAfter(Instant.now()))

        // Should be scheduled for ~1 day from now
        val expectedNextReview = Instant.now().plus(1, ChronoUnit.DAYS)
        val diff = ChronoUnit.SECONDS.between(expectedNextReview, item.nextReviewAt)
        assertTrue(Math.abs(diff) < 2, "Initial review should be ~1 day from now")
    }

    @Test
    fun `scheduleInitialReview does not modify item with existing nextReviewAt`() {
        val existingReviewDate = Instant.now().plus(5, ChronoUnit.DAYS)
        val item = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            lang = "es",
            lemma = "hablar",
            exposures = 1,
            lastSeenAt = Instant.now(),
            nextReviewAt = existingReviewDate,
            reviewStage = 2
        )

        vocabularyReviewService.scheduleInitialReview(item)

        assertEquals(existingReviewDate, item.nextReviewAt)
        assertEquals(2, item.reviewStage)
    }
}
