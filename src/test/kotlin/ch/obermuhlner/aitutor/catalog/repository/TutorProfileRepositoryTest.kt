package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
class TutorProfileRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var tutorProfileRepository: TutorProfileRepository

    @Test
    fun `should find active tutors ordered by display order`() {
        // Given
        val tutor1 = createTutorProfile("María", "es", 1, true)
        val tutor2 = createTutorProfile("Carlos", "es", 0, true)
        val tutor3 = createTutorProfile("Inactive", "es", 2, false)

        entityManager.persist(tutor1)
        entityManager.persist(tutor2)
        entityManager.persist(tutor3)
        entityManager.flush()

        // When
        val result = tutorProfileRepository.findByIsActiveTrueOrderByDisplayOrder()

        // Then
        assertEquals(2, result.size)
        assertEquals("Carlos", result[0].name)
        assertEquals("María", result[1].name)
    }

    @Test
    fun `should find tutors by language code and active status`() {
        // Given
        val spanishTutor = createTutorProfile("María", "es", 0, true)
        val frenchTutor = createTutorProfile("François", "fr", 0, true)
        val inactiveTutor = createTutorProfile("Inactive", "es", 1, false)

        entityManager.persist(spanishTutor)
        entityManager.persist(frenchTutor)
        entityManager.persist(inactiveTutor)
        entityManager.flush()

        // When
        val result = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")

        // Then
        assertEquals(1, result.size)
        assertEquals("María", result[0].name)
        assertEquals("es", result[0].targetLanguageCode)
    }

    @Test
    fun `should return empty list when no active tutors for language`() {
        // Given
        val frenchTutor = createTutorProfile("François", "fr", 0, true)
        entityManager.persist(frenchTutor)
        entityManager.flush()

        // When
        val result = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")

        // Then
        assertTrue(result.isEmpty())
    }

    private fun createTutorProfile(
        name: String,
        languageCode: String,
        displayOrder: Int,
        isActive: Boolean
    ): TutorProfileEntity {
        return TutorProfileEntity(
            id = UUID.randomUUID(),
            name = name,
            emoji = "👩‍🏫",
            personaEnglish = "patient coach",
            domainEnglish = "general conversation",
            descriptionEnglish = "Patient coach who loves helping beginners",
            personaJson = """{"en": "patient coach"}""",
            domainJson = """{"en": "general conversation"}""",
            descriptionJson = """{"en": "Patient coach who loves helping beginners"}""",
            culturalBackgroundJson = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = languageCode,
            isActive = isActive,
            displayOrder = displayOrder
        )
    }
}
