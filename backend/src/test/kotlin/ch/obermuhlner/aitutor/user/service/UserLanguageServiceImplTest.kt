package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserLanguageServiceImplTest {

    private val repository = mockk<UserLanguageProficiencyRepository>()
    private val service = UserLanguageServiceImpl(repository)

    @Test
    fun `addLanguage should create new proficiency when not exists`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.addLanguage(userId, "es", LanguageProficiencyType.Learning, CEFRLevel.A1, false)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("es", result.languageCode)
        assertEquals(LanguageProficiencyType.Learning, result.proficiencyType)
        assertEquals(CEFRLevel.A1, result.cefrLevel)
        assertFalse(result.isNative)
        verify { repository.save(any()) }
    }

    @Test
    fun `addLanguage should return existing proficiency when already exists`() {
        val userId = UUID.randomUUID()
        val existing = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A2,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns existing

        val result = service.addLanguage(userId, "es", LanguageProficiencyType.Learning, CEFRLevel.A1, false)

        assertEquals(existing, result)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `updateLanguage should update CEFR level`() {
        val userId = UUID.randomUUID()
        val entity = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns entity
        every { repository.save(any()) } answers { firstArg() }

        val result = service.updateLanguage(userId, "es", CEFRLevel.A2)

        assertEquals(CEFRLevel.A2, result.cefrLevel)
        verify { repository.save(entity) }
    }

    @Test
    fun `getUserLanguages should return all user languages`() {
        val userId = UUID.randomUUID()
        val languages = listOf(
            UserLanguageProficiencyEntity(
                userId = userId,
                languageCode = "en",
                proficiencyType = LanguageProficiencyType.Native,
                cefrLevel = null,
                isNative = true,
                isPrimary = true,
                selfAssessed = true,
                lastAssessedAt = Instant.now()
            ),
            UserLanguageProficiencyEntity(
                userId = userId,
                languageCode = "es",
                proficiencyType = LanguageProficiencyType.Learning,
                cefrLevel = CEFRLevel.A1,
                isNative = false,
                isPrimary = false,
                selfAssessed = true,
                lastAssessedAt = Instant.now()
            )
        )
        every { repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns languages

        val result = service.getUserLanguages(userId)

        assertEquals(2, result.size)
        verify { repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) }
    }

    @Test
    fun `setPrimaryLanguage should update primary flag`() {
        val userId = UUID.randomUUID()
        val language1 = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        val language2 = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns listOf(language1, language2)
        every { repository.saveAll(any<List<UserLanguageProficiencyEntity>>()) } returns listOf(language1, language2)
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns language2
        every { repository.save(any()) } answers { firstArg() }

        service.setPrimaryLanguage(userId, "es")

        assertFalse(language1.isPrimary)
        assertTrue(language2.isPrimary)
        verify { repository.saveAll(listOf(language1, language2)) }
        verify { repository.save(language2) }
    }

    @Test
    fun `suggestSourceLanguage should return primary language`() {
        val userId = UUID.randomUUID()
        val primary = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndIsPrimaryTrue(userId) } returns primary

        val result = service.suggestSourceLanguage(userId, "es")

        assertEquals("en", result)
    }

    @Test
    fun `suggestSourceLanguage should return first native non-target when no primary`() {
        val userId = UUID.randomUUID()
        val native = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndIsPrimaryTrue(userId) } returns null
        every { repository.findByUserIdAndIsNativeTrue(userId) } returns listOf(native)

        val result = service.suggestSourceLanguage(userId, "es")

        assertEquals("en", result)
    }

    @Test
    fun `suggestSourceLanguage should return en when no native languages`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdAndIsPrimaryTrue(userId) } returns null
        every { repository.findByUserIdAndIsNativeTrue(userId) } returns emptyList()

        val result = service.suggestSourceLanguage(userId, "es")

        assertEquals("en", result)
    }

    @Test
    fun `inferFromSession should add both languages when not exist`() {
        val userId = UUID.randomUUID()
        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "Friendly",
            tutorDomain = "General",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A1
        )
        every { repository.findByUserIdAndLanguageCode(userId, "en") } returns null
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns null
        every { repository.save(any()) } answers { firstArg() }

        service.inferFromSession(userId, session)

        verify(exactly = 2) { repository.save(any()) }
    }

    @Test
    fun `updateLanguage should throw exception when language not found`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns null

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.updateLanguage(userId, "es", CEFRLevel.B1)
        }

        assertEquals("Language proficiency not found for user $userId and language es", exception.message)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `removeLanguage should delete language proficiency`() {
        val userId = UUID.randomUUID()
        val entity = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns entity
        every { repository.delete(entity) } returns Unit

        service.removeLanguage(userId, "es")

        verify { repository.delete(entity) }
    }

    @Test
    fun `removeLanguage should not delete when language not found`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns null

        service.removeLanguage(userId, "es")

        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `addLanguage should create native language proficiency`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdAndLanguageCode(userId, "en") } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.addLanguage(userId, "en", LanguageProficiencyType.Native, null, true)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("en", result.languageCode)
        assertEquals(LanguageProficiencyType.Native, result.proficiencyType)
        assertEquals(null, result.cefrLevel)
        assertEquals(true, result.isNative)
        verify { repository.save(any()) }
    }

    @Test
    fun `getUserLanguages should return empty list when no languages`() {
        val userId = UUID.randomUUID()
        every { repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns emptyList()

        val result = service.getUserLanguages(userId)

        assertEquals(0, result.size)
    }

    @Test
    fun `setPrimaryLanguage should handle single language case`() {
        val userId = UUID.randomUUID()
        val language = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns listOf(language)
        every { repository.saveAll(any<List<UserLanguageProficiencyEntity>>()) } returns listOf(language)
        every { repository.findByUserIdAndLanguageCode(userId, "en") } returns language
        every { repository.save(any()) } answers { firstArg() }

        service.setPrimaryLanguage(userId, "en")

        assertEquals(true, language.isPrimary)
        verify { repository.save(language) }
    }

    @Test
    fun `inferFromSession should only add source language when target exists`() {
        val userId = UUID.randomUUID()
        val existingTarget = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        val session = ChatSessionEntity(
            userId = userId,
            tutorName = "Maria",
            tutorPersona = "Friendly",
            tutorDomain = "General",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Correction,
            estimatedCEFRLevel = CEFRLevel.A2
        )
        every { repository.findByUserIdAndLanguageCode(userId, "en") } returns null
        every { repository.findByUserIdAndLanguageCode(userId, "es") } returns existingTarget
        every { repository.save(any()) } answers { firstArg() }

        service.inferFromSession(userId, session)

        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `suggestSourceLanguage should skip target language when finding native`() {
        val userId = UUID.randomUUID()
        val targetNative = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        val otherNative = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { repository.findByUserIdAndIsPrimaryTrue(userId) } returns null
        every { repository.findByUserIdAndIsNativeTrue(userId) } returns listOf(targetNative, otherNative)

        val result = service.suggestSourceLanguage(userId, "es")

        assertEquals("en", result)
    }
}
