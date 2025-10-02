package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.dto.NewVocabularyDTO
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class VocabularyServiceTest {

    private lateinit var vocabularyItemRepository: VocabularyItemRepository
    private lateinit var vocabularyContextRepository: VocabularyContextRepository
    private lateinit var vocabularyService: VocabularyService

    @BeforeEach
    fun setup() {
        vocabularyItemRepository = mockk()
        vocabularyContextRepository = mockk()
        vocabularyService = VocabularyService(vocabularyItemRepository, vocabularyContextRepository)
    }

    @Test
    fun `addNewVocabulary creates new vocabulary item when not exists`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val lemma = "hablar"
        val context = "Yo quiero hablar español"
        val turnId = UUID.randomUUID()

        val items = listOf(NewVocabularyDTO(lemma, context))

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemma) } returns null

        val itemSlot = slot<VocabularyItemEntity>()
        val savedItem = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = userId,
            lang = lang,
            lemma = lemma,
            exposures = 1,
            lastSeenAt = Instant.now()
        )

        every { vocabularyItemRepository.save(capture(itemSlot)) } returns savedItem

        val contextSlot = slot<VocabularyContextEntity>()
        every { vocabularyContextRepository.save(capture(contextSlot)) } answers { firstArg() }

        val result = vocabularyService.addNewVocabulary(userId, lang, items, turnId)

        assertEquals(1, result.size)
        verify { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemma) }
        verify { vocabularyItemRepository.save(any()) }
        verify { vocabularyContextRepository.save(any()) }

        // Verify item properties
        assertEquals(userId, itemSlot.captured.userId)
        assertEquals(lang, itemSlot.captured.lang)
        assertEquals(lemma, itemSlot.captured.lemma)
        assertEquals(1, itemSlot.captured.exposures)

        // Verify context properties
        assertEquals(context, contextSlot.captured.context)
        assertEquals(turnId, contextSlot.captured.turnId)
    }

    @Test
    fun `addNewVocabulary increments exposures for existing vocabulary item`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val lemma = "hablar"
        val context = "Me gusta hablar"
        val turnId = UUID.randomUUID()

        val items = listOf(NewVocabularyDTO(lemma, context))

        val existingItem = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = userId,
            lang = lang,
            lemma = lemma,
            exposures = 3,
            lastSeenAt = Instant.now().minusSeconds(86400)
        )

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemma) } returns existingItem
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }
        every { vocabularyContextRepository.save(any()) } answers { firstArg() }

        val result = vocabularyService.addNewVocabulary(userId, lang, items, turnId)

        assertEquals(1, result.size)
        assertEquals(4, existingItem.exposures)
        verify { vocabularyItemRepository.save(existingItem) }
    }

    @Test
    fun `addNewVocabulary handles multiple items`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val items = listOf(
            NewVocabularyDTO("hablar", "context 1"),
            NewVocabularyDTO("comer", "context 2"),
            NewVocabularyDTO("dormir", "context 3")
        )

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) } returns null
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }
        every { vocabularyContextRepository.save(any()) } answers { firstArg() }

        val result = vocabularyService.addNewVocabulary(userId, lang, items)

        assertEquals(3, result.size)
        verify(exactly = 3) { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) }
        verify(exactly = 3) { vocabularyItemRepository.save(any()) }
        verify(exactly = 3) { vocabularyContextRepository.save(any()) }
    }

    @Test
    fun `addNewVocabulary normalizes lemma with trim`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val lemma = "  hablar  "
        val normalizedLemma = "hablar"
        val context = "context"

        val items = listOf(NewVocabularyDTO(lemma, context))

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, normalizedLemma) } returns null
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }
        every { vocabularyContextRepository.save(any()) } answers { firstArg() }

        vocabularyService.addNewVocabulary(userId, lang, items)

        verify { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, normalizedLemma) }
    }

    @Test
    fun `addNewVocabulary skips blank lemmas`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val items = listOf(
            NewVocabularyDTO("", "context 1"),
            NewVocabularyDTO("   ", "context 2"),
            NewVocabularyDTO("hablar", "context 3")
        )

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) } returns null
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }
        every { vocabularyContextRepository.save(any()) } answers { firstArg() }

        val result = vocabularyService.addNewVocabulary(userId, lang, items)

        assertEquals(1, result.size)
        verify(exactly = 1) { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) }
    }

    @Test
    fun `addNewVocabulary truncates long context to 512 characters`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val longContext = "a".repeat(1000)
        val items = listOf(NewVocabularyDTO("hablar", longContext))

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) } returns null
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val contextSlot = slot<VocabularyContextEntity>()
        every { vocabularyContextRepository.save(capture(contextSlot)) } answers { firstArg() }

        vocabularyService.addNewVocabulary(userId, lang, items)

        assertEquals(512, contextSlot.captured.context.length)
        verify { vocabularyContextRepository.save(any()) }
    }

    @Test
    fun `addNewVocabulary handles null turnId`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val items = listOf(NewVocabularyDTO("hablar", "context"))

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(any(), any(), any()) } returns null
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }

        val contextSlot = slot<VocabularyContextEntity>()
        every { vocabularyContextRepository.save(capture(contextSlot)) } answers { firstArg() }

        vocabularyService.addNewVocabulary(userId, lang, items, turnId = null)

        assertNull(contextSlot.captured.turnId)
        verify { vocabularyContextRepository.save(any()) }
    }

    @Test
    fun `addNewVocabulary updates lastSeenAt for existing items`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val lemma = "hablar"
        val oldTimestamp = Instant.now().minusSeconds(86400)

        val existingItem = VocabularyItemEntity(
            id = UUID.randomUUID(),
            userId = userId,
            lang = lang,
            lemma = lemma,
            exposures = 1,
            lastSeenAt = oldTimestamp
        )

        val items = listOf(NewVocabularyDTO(lemma, "context"))

        every { vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, lemma) } returns existingItem
        every { vocabularyItemRepository.save(any()) } answers { firstArg() }
        every { vocabularyContextRepository.save(any()) } answers { firstArg() }

        vocabularyService.addNewVocabulary(userId, lang, items)

        assertTrue(existingItem.lastSeenAt.isAfter(oldTimestamp))
        verify { vocabularyItemRepository.save(existingItem) }
    }

    @Test
    fun `addNewVocabulary returns empty list when all items are blank`() {
        val userId = UUID.randomUUID()
        val lang = "es"
        val items = listOf(
            NewVocabularyDTO("", "context 1"),
            NewVocabularyDTO("   ", "context 2")
        )

        val result = vocabularyService.addNewVocabulary(userId, lang, items)

        assertEquals(0, result.size)
        verify(exactly = 0) { vocabularyItemRepository.save(any()) }
        verify(exactly = 0) { vocabularyContextRepository.save(any()) }
    }
}
