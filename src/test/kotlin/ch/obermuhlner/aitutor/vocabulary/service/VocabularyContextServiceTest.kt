package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class VocabularyContextServiceTest {

    private lateinit var vocabularyContextService: VocabularyContextService
    private lateinit var vocabularyItemRepository: VocabularyItemRepository

    private val testUserId = UUID.randomUUID()
    private val testLang = "es"

    @BeforeEach
    fun setup() {
        vocabularyItemRepository = mockk()
        vocabularyContextService = VocabularyContextService(vocabularyItemRepository)
    }

    @Test
    fun `should return empty context when user has no vocabulary`() {
        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns emptyList()

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(0, result.totalWordCount)
        assertTrue(result.wordsForReinforcement.isEmpty())
        assertTrue(result.masteredWords.isEmpty())
        assertTrue(result.recentNewWords.isEmpty())
    }

    @Test
    fun `should identify words for reinforcement`() {
        val now = Instant.now()
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)

        val vocab = listOf(
            createVocabItem("palabra", exposures = 2, lastSeenAt = threeDaysAgo),
            createVocabItem("casa", exposures = 3, lastSeenAt = threeDaysAgo),
            createVocabItem("perro", exposures = 4, lastSeenAt = threeDaysAgo)
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(3, result.totalWordCount)
        assertEquals(3, result.wordsForReinforcement.size)
        assertTrue(result.wordsForReinforcement.contains("palabra"))
        assertTrue(result.wordsForReinforcement.contains("casa"))
        assertTrue(result.wordsForReinforcement.contains("perro"))
    }

    @Test
    fun `should identify mastered words`() {
        val now = Instant.now()

        val vocab = listOf(
            createVocabItem("hola", exposures = 5, lastSeenAt = now),
            createVocabItem("gracias", exposures = 7, lastSeenAt = now),
            createVocabItem("adiós", exposures = 10, lastSeenAt = now)
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(3, result.totalWordCount)
        assertEquals(3, result.masteredWords.size)
        assertTrue(result.masteredWords.contains("hola"))
        assertTrue(result.masteredWords.contains("gracias"))
        assertTrue(result.masteredWords.contains("adiós"))
    }

    @Test
    fun `should identify recent new words`() {
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)

        val vocab = listOf(
            createVocabItem("nuevo", exposures = 1, lastSeenAt = oneHourAgo),
            createVocabItem("reciente", exposures = 1, lastSeenAt = oneDayAgo),
            createVocabItem("fresco", exposures = 2, lastSeenAt = oneHourAgo)
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(3, result.totalWordCount)
        assertEquals(3, result.recentNewWords.size)
        assertTrue(result.recentNewWords.contains("nuevo"))
        assertTrue(result.recentNewWords.contains("reciente"))
        assertTrue(result.recentNewWords.contains("fresco"))
    }

    @Test
    fun `should not include recent words in reinforcement list`() {
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)

        val vocab = listOf(
            createVocabItem("reciente", exposures = 3, lastSeenAt = oneHourAgo)
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(1, result.totalWordCount)
        assertTrue(result.wordsForReinforcement.isEmpty())
        assertTrue(result.recentNewWords.contains("reciente"))
    }

    @Test
    fun `should limit reinforcement words to 10`() {
        val now = Instant.now()
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)

        val vocab = (1..15).map {
            createVocabItem("word$it", exposures = 3, lastSeenAt = threeDaysAgo)
        }

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(15, result.totalWordCount)
        assertEquals(10, result.wordsForReinforcement.size)
    }

    @Test
    fun `should limit mastered words to 30`() {
        val now = Instant.now()

        val vocab = (1..40).map {
            createVocabItem("word$it", exposures = 6, lastSeenAt = now)
        }

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(40, result.totalWordCount)
        assertEquals(30, result.masteredWords.size)
    }

    @Test
    fun `should categorize mixed vocabulary correctly`() {
        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)

        val vocab = listOf(
            createVocabItem("mastered1", exposures = 5, lastSeenAt = threeDaysAgo),
            createVocabItem("mastered2", exposures = 8, lastSeenAt = now),
            createVocabItem("reinforce1", exposures = 2, lastSeenAt = threeDaysAgo),
            createVocabItem("reinforce2", exposures = 4, lastSeenAt = threeDaysAgo),
            createVocabItem("recent1", exposures = 1, lastSeenAt = oneHourAgo),
            createVocabItem("recent2", exposures = 2, lastSeenAt = oneHourAgo)
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns vocab

        val result = vocabularyContextService.getVocabularyContext(testUserId, testLang)

        assertEquals(6, result.totalWordCount)
        assertEquals(2, result.wordsForReinforcement.size)
        assertEquals(2, result.masteredWords.size)
        // mastered2 is recent (seen now) and has 8 exposures, so it appears in both mastered and recent
        // recent1 and recent2 also qualify as recent
        assertEquals(3, result.recentNewWords.size)
    }

    private fun createVocabItem(
        lemma: String,
        exposures: Int = 1,
        lastSeenAt: Instant = Instant.now()
    ): VocabularyItemEntity {
        return VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = testUserId,
            lang = testLang,
            lemma = lemma,
            exposures = exposures,
            lastSeenAt = lastSeenAt
        )
    }
}
