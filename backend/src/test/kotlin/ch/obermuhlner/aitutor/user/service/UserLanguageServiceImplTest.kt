package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `addLanguage should create new language proficiency for user`() {
        val userId = UUID.randomUUID()
        val languageCode = "es"
        val type = LanguageProficiencyType.Learning
        val level = CEFRLevel.A1
        val isNative = false

        val expectedEntity = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = type,
            cefrLevel = level,
            isNative = isNative,
            isPrimary = false,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) } returns null
        every { userLanguageProficiencyRepository.save(any()) } returns expectedEntity

        val result = service.addLanguage(userId, languageCode, type, level, isNative)

        assertEquals(expectedEntity, result)
        verify { userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode) }
        verify { userLanguageProficiencyRepository.save(any()) }
    }

    @Test
    fun `getUserLanguages should return all user languages`() {
        val userId = UUID.randomUUID()
        val languages = listOf(
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

        every { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) } returns languages

        val result = service.getUserLanguages(userId)

        assertEquals(1, result.size)
        verify { userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId) }
    }

    @Test
    fun `getNativeLanguages should return only native languages`() {
        val userId = UUID.randomUUID()
        val nativeLanguages = listOf(
            UserLanguageProficiencyEntity(
                id = UUID.randomUUID(),
                userId = userId,
                languageCode = "en",
                proficiencyType = LanguageProficiencyType.Native,
                cefrLevel = null,
                isNative = true,
                isPrimary = true,
                selfAssessed = true,
                lastAssessedAt = Instant.now()
            )
        )

        every { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) } returns nativeLanguages

        val result = service.getNativeLanguages(userId)

        assertEquals(1, result.size)
        assertTrue(result[0].isNative)
        verify { userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId) }
    }

    @Test
    fun `getPrimaryLanguage should return primary language`() {
        val userId = UUID.randomUUID()
        val primaryLanguage = UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = "en",
            proficiencyType = LanguageProficiencyType.Native,
            cefrLevel = null,
            isNative = true,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns primaryLanguage

        val result = service.getPrimaryLanguage(userId)

        assertNotNull(result)
        assertTrue(result!!.isPrimary)
        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
    }

    @Test
    fun `getPrimaryLanguage should return null when no primary language set`() {
        val userId = UUID.randomUUID()

        every { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) } returns null

        val result = service.getPrimaryLanguage(userId)

        assertNull(result)
        verify { userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId) }
    }
}