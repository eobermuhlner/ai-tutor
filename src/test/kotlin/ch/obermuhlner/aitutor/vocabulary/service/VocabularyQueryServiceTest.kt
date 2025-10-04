package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VocabularyQueryServiceTest {

    private lateinit var vocabularyItemRepository: VocabularyItemRepository
    private lateinit var vocabularyContextRepository: VocabularyContextRepository
    private lateinit var vocabularyQueryService: VocabularyQueryService

    private val testUserId = UUID.randomUUID()
    private val testLang = "Spanish"

    @BeforeEach
    fun setup() {
        vocabularyItemRepository = mockk()
        vocabularyContextRepository = mockk()
        vocabularyQueryService = VocabularyQueryService(vocabularyItemRepository, vocabularyContextRepository)
    }

    @Test
    fun `should get user vocabulary without language filter`() {
        val item1 = VocabularyItemEntity(
            userId = testUserId,
            lang = testLang,
            lemma = "hola",
            exposures = 1
        )
        val item2 = VocabularyItemEntity(
            userId = testUserId,
            lang = testLang,
            lemma = "gracias",
            exposures = 2
        )

        every { vocabularyItemRepository.findByUserIdOrderByLastSeenAtDesc(testUserId) } returns listOf(item1, item2)

        val result = vocabularyQueryService.getUserVocabulary(testUserId)

        assertEquals(2, result.size)
        assertEquals("hola", result[0].lemma)
        assertEquals("gracias", result[1].lemma)
        verify(exactly = 1) { vocabularyItemRepository.findByUserIdOrderByLastSeenAtDesc(testUserId) }
    }

    @Test
    fun `should get user vocabulary with language filter`() {
        val item1 = VocabularyItemEntity(
            userId = testUserId,
            lang = testLang,
            lemma = "hola",
            exposures = 1
        )

        every { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) } returns listOf(item1)

        val result = vocabularyQueryService.getUserVocabulary(testUserId, testLang)

        assertEquals(1, result.size)
        assertEquals("hola", result[0].lemma)
        verify(exactly = 1) { vocabularyItemRepository.findByUserIdAndLangOrderByLastSeenAtDesc(testUserId, testLang) }
    }

    @Test
    fun `should get vocabulary item with contexts`() {
        val itemId = UUID.randomUUID()
        val item = VocabularyItemEntity(
            id = itemId,
            userId = testUserId,
            lang = testLang,
            lemma = "hola",
            exposures = 3
        )
        val context1 = VocabularyContextEntity(
            vocabItem = item,
            context = "Hola, ¿cómo estás?",
            turnId = UUID.randomUUID()
        )
        val context2 = VocabularyContextEntity(
            vocabItem = item,
            context = "Hola amigo",
            turnId = UUID.randomUUID()
        )

        every { vocabularyItemRepository.findById(itemId) } returns Optional.of(item)
        every { vocabularyContextRepository.findByVocabItemId(itemId) } returns listOf(context1, context2)

        val result = vocabularyQueryService.getVocabularyItemWithContexts(itemId, testUserId)

        assertNotNull(result)
        assertEquals("hola", result!!.item.lemma)
        assertEquals(2, result.contexts.size)
        assertEquals("Hola, ¿cómo estás?", result.contexts[0].context)
        assertEquals("Hola amigo", result.contexts[1].context)
        verify(exactly = 1) { vocabularyItemRepository.findById(itemId) }
        verify(exactly = 1) { vocabularyContextRepository.findByVocabItemId(itemId) }
    }

    @Test
    fun `should return null when vocabulary item not found`() {
        val itemId = UUID.randomUUID()

        every { vocabularyItemRepository.findById(itemId) } returns Optional.empty()

        val result = vocabularyQueryService.getVocabularyItemWithContexts(itemId, testUserId)

        assertNull(result)
        verify(exactly = 1) { vocabularyItemRepository.findById(itemId) }
        verify(exactly = 0) { vocabularyContextRepository.findByVocabItemId(any()) }
    }
}
