package ch.obermuhlner.aitutor.user.repository

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.Instant
import java.util.*

@DataJpaTest
class UserLanguageProficiencyRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: UserLanguageProficiencyRepository

    private val testUserId = UUID.randomUUID()

    @Test
    fun `should find user languages ordered by native status and CEFR level`() {
        // Given
        val native = createProficiency(testUserId, "en", LanguageProficiencyType.Native, null, true)
        val learning1 = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.B2, false)
        val learning2 = createProficiency(testUserId, "fr", LanguageProficiencyType.Learning, CEFRLevel.A1, false)

        entityManager.persist(native)
        entityManager.persist(learning1)
        entityManager.persist(learning2)
        entityManager.flush()

        // When
        val result = repository.findByUserIdOrderByIsNativeDescCefrLevelDesc(testUserId)

        // Then
        assertEquals(3, result.size)
        assertEquals("en", result[0].languageCode)  // Native first
        assertEquals("es", result[1].languageCode)  // B2 before A1
        assertEquals("fr", result[2].languageCode)
    }

    @Test
    fun `should find user language by language code`() {
        // Given
        val proficiency = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A2, false)
        entityManager.persist(proficiency)
        entityManager.flush()

        // When
        val result = repository.findByUserIdAndLanguageCode(testUserId, "es")

        // Then
        assertNotNull(result)
        assertEquals("es", result?.languageCode)
        assertEquals(CEFRLevel.A2, result?.cefrLevel)
    }

    @Test
    fun `should return null when language not found for user`() {
        // Given
        val proficiency = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A2, false)
        entityManager.persist(proficiency)
        entityManager.flush()

        // When
        val result = repository.findByUserIdAndLanguageCode(testUserId, "fr")

        // Then
        assertNull(result)
    }

    @Test
    fun `should find native languages for user`() {
        // Given
        val native1 = createProficiency(testUserId, "en", LanguageProficiencyType.Native, null, true)
        val native2 = createProficiency(testUserId, "de", LanguageProficiencyType.Native, null, true)
        val learning = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A1, false)

        entityManager.persist(native1)
        entityManager.persist(native2)
        entityManager.persist(learning)
        entityManager.flush()

        // When
        val result = repository.findByUserIdAndIsNativeTrue(testUserId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isNative })
    }

    @Test
    fun `should find primary language for user`() {
        // Given
        val primary = createProficiency(testUserId, "en", LanguageProficiencyType.Native, null, true, true)
        val other = createProficiency(testUserId, "de", LanguageProficiencyType.Native, null, true, false)

        entityManager.persist(primary)
        entityManager.persist(other)
        entityManager.flush()

        // When
        val result = repository.findByUserIdAndIsPrimaryTrue(testUserId)

        // Then
        assertNotNull(result)
        assertEquals("en", result?.languageCode)
        assertTrue(result?.isPrimary ?: false)
    }

    @Test
    fun `should find languages by proficiency type`() {
        // Given
        val native = createProficiency(testUserId, "en", LanguageProficiencyType.Native, null, true)
        val learning1 = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A2, false)
        val learning2 = createProficiency(testUserId, "fr", LanguageProficiencyType.Learning, CEFRLevel.B1, false)

        entityManager.persist(native)
        entityManager.persist(learning1)
        entityManager.persist(learning2)
        entityManager.flush()

        // When
        val result = repository.findByUserIdAndProficiencyType(testUserId, LanguageProficiencyType.Learning)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.proficiencyType == LanguageProficiencyType.Learning })
    }

    @Test
    fun `should enforce unique constraint on user and language code`() {
        // Given
        val proficiency1 = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A1, false)
        entityManager.persist(proficiency1)
        entityManager.flush()

        // When/Then - attempting to create duplicate should fail
        val proficiency2 = createProficiency(testUserId, "es", LanguageProficiencyType.Learning, CEFRLevel.A2, false)

        assertThrows(Exception::class.java) {
            entityManager.persist(proficiency2)
            entityManager.flush()
        }
    }

    private fun createProficiency(
        userId: UUID,
        languageCode: String,
        type: LanguageProficiencyType,
        cefrLevel: CEFRLevel?,
        isNative: Boolean,
        isPrimary: Boolean = false
    ): UserLanguageProficiencyEntity {
        return UserLanguageProficiencyEntity(
            id = UUID.randomUUID(),
            userId = userId,
            languageCode = languageCode,
            proficiencyType = type,
            cefrLevel = cefrLevel,
            isNative = isNative,
            isPrimary = isPrimary,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
    }
}
