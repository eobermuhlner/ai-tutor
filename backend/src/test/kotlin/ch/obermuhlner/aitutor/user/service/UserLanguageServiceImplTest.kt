package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserLanguageServiceImplTest {
    private lateinit var userLanguageProficiencyRepository: UserLanguageProficiencyRepository
    private lateinit var service: UserLanguageServiceImpl

    @BeforeEach
    fun setup() {
        userLanguageProficiencyRepository = mockk()
        service = UserLanguageServiceImpl(userLanguageProficiencyRepository)
    }

    @Test
    fun `addLanguage should save new language proficiency when not exists`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"
        val type = LanguageProficiencyType.Learning
        val cefrLevel = CEFRLevel.A1
        val isNative = false

        val entity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = type,
            cefrLevel = cefrLevel,
            isNative = isNative,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns null
        every { userLanguageProficiencyRepository.save(any()) } returns entity

        val result = service.addLanguage(userId, languageCode, type, cefrLevel, isNative)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.save(any()) }
        Assertions.assertEquals(userId, result.userId)
        Assertions.assertEquals(languageCode, result.languageCode)
        Assertions.assertEquals(type, result.proficiencyType)
        Assertions.assertEquals(cefrLevel, result.cefrLevel)
    }

    @Test
    fun `addLanguage should return existing entity when already exists`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"
        val type = LanguageProficiencyType.Learning
        val cefrLevel = CEFRLevel.A1
        val isNative = false

        val existingEntity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = LanguageProficiencyType.Learning,  // Different from requested
            cefrLevel = CEFRLevel.B1,  // Different from requested
            isNative = true,  // Different from requested
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns existingEntity

        val result = service.addLanguage(userId, languageCode, type, cefrLevel, isNative)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify(exactly = 0) { userLanguageProficiencyRepository.save(any()) }
        Assertions.assertEquals(existingEntity, result)
    }

    @Test
    fun `updateLanguage should update existing language proficiency`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"
        val newLevel = CEFRLevel.B2

        val existingEntity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        val updatedEntity = existingEntity.apply {
            this.cefrLevel = newLevel
            this.lastAssessedAt = java.time.Instant.now()
        }

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns existingEntity
        every { userLanguageProficiencyRepository.save(existingEntity) } returns updatedEntity

        val result = service.updateLanguage(userId, languageCode, newLevel)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.save(existingEntity) }
        Assertions.assertEquals(newLevel, result.cefrLevel)
    }

    @Test
    fun `updateLanguage should create new record when language doesn't exist`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"
        val newLevel = CEFRLevel.B2
        val newEntity = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = languageCode,
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = newLevel,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns null
        every { userLanguageProficiencyRepository.save(any()) } returns newEntity

        val result = service.updateLanguage(userId, languageCode, newLevel)

        Assertions.assertNotNull(result)
        Assertions.assertEquals(newLevel, result.cefrLevel)
        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.save(any()) }
    }

    @Test
    fun `getUserLanguages should return languages for user`() {
        val userId = UUID.randomUUID()
        val entities = listOf(
            mockk<UserLanguageProficiencyEntity>(),
            mockk<UserLanguageProficiencyEntity>()
        )

        every { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns entities

        val result = service.getUserLanguages(userId)

        verify { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) }
        Assertions.assertEquals(entities, result)
    }

    @Test
    fun `getNativeLanguages should return native languages for user`() {
        val userId = UUID.randomUUID()
        val entities = listOf(mockk<UserLanguageProficiencyEntity>())

        every { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) } returns entities

        val result = service.getNativeLanguages(userId)

        verify { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) }
        Assertions.assertEquals(entities, result)
    }

    @Test
    fun `getLearningLanguages should return learning languages for user`() {
        val userId = UUID.randomUUID()
        val entities = listOf(mockk<UserLanguageProficiencyEntity>())

        every { userLanguageProficiencyRepository.findByUserIdAndProficiencyType(userId, LanguageProficiencyType.Learning) } returns entities

        val result = service.getLearningLanguages(userId)

        verify { userLanguageProficiencyRepository.findByUserIdAndProficiencyType(userId, LanguageProficiencyType.Learning) }
        Assertions.assertEquals(entities, result)
    }

    @Test
    fun `getPrimaryLanguage should return primary language for user`() {
        val userId = UUID.randomUUID()
        val entity = mockk<UserLanguageProficiencyEntity>()

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns entity

        val result = service.getPrimaryLanguage(userId)

        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
        Assertions.assertEquals(entity, result)
    }

    @Test
    fun `setPrimaryLanguage should clear existing primary and set new primary`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"

        val allLanguages = listOf(
            UserLanguageProficiencyEntity(
                id = UUID.randomUUID(),
                userId = userId,
                languageCode = "en",
                proficiencyType = LanguageProficiencyType.Native,
                cefrLevel = CEFRLevel.C2,
                isNative = true,
                isPrimary = true,
                selfAssessed = true,
                lastAssessedAt = Instant.now()
            ),
            UserLanguageProficiencyEntity(
                id = UUID.randomUUID(),
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

        val targetEntity = allLanguages[1].apply {
            this.isPrimary = true
        }

        every { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns allLanguages
        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns allLanguages[1]
        every { userLanguageProficiencyRepository.saveAll(allLanguages) } returns allLanguages
        every { userLanguageProficiencyRepository.save(any()) } returns targetEntity

        service.setPrimaryLanguage(userId, languageCode)

        verify { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) }
        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.saveAll(allLanguages) }
        verify { userLanguageProficiencyRepository.save(any()) }
    }

    @Test
    fun `removeLanguage should delete language when exists`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"

        val entity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns entity
        every { userLanguageProficiencyRepository.delete(entity) } returns Unit

        service.removeLanguage(userId, languageCode)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.delete(entity) }
    }

    @Test
    fun `removeLanguage should do nothing when language doesn't exist`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns null

        service.removeLanguage(userId, languageCode)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify(exactly = 0) { userLanguageProficiencyRepository.delete(any()) }
    }

    @Test
    fun `suggestSourceLanguage should return primary language when not target`() {
        val userId = UUID.randomUUID()
        val targetLanguageCode = "es"
        val primaryLanguageCode = "fr"

        val primary = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = primaryLanguageCode,
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = CEFRLevel.C2,
            isNative = true,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns primary

        val result = service.suggestSourceLanguage(userId, targetLanguageCode)

        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
        Assertions.assertEquals(primaryLanguageCode, result)
    }

    @Test
    fun `suggestSourceLanguage should return native language when no primary or primary is target`() {
        val userId = UUID.randomUUID()
        val targetLanguageCode = "es"

        val primary = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = targetLanguageCode,  // Same as target
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = CEFRLevel.C2,
            isNative = true,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        val native = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = "fr",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = CEFRLevel.C1,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns primary
        every { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) } returns listOf(native)

        val result = service.suggestSourceLanguage(userId, targetLanguageCode)

        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
        verify { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) }
        Assertions.assertEquals("fr", result)
    }

    @Test
    fun `suggestSourceLanguage should return default en when no suitable languages found`() {
        val userId = UUID.randomUUID()
        val targetLanguageCode = "es"

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns null
        every { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) } returns emptyList()

        val result = service.suggestSourceLanguage(userId, targetLanguageCode)

        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
        verify { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) }
        Assertions.assertEquals("en", result)
    }

    @Test
    fun `inferFromSession should add source and target languages from session when not exist`() {
        val userId = UUID.randomUUID()
        val session = ChatSessionEntity(
            id = UUID.randomUUID(),
            userId = userId,
            tutorName = "Test Tutor",
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ch.obermuhlner.aitutor.tutor.domain.ConversationPhase.Free,
            estimatedCEFRLevel = CEFRLevel.A1
        )

        val sourceEntity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        val targetEntity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, "en") } returns null
        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, "es") } returns null
        every { userLanguageProficiencyRepository.save(any()) } returnsMany listOf(sourceEntity, targetEntity)

        service.inferFromSession(userId, session)

        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, "en") }
        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, "es") }
        verify { userLanguageProficiencyRepository.save(any()) }
    }
}