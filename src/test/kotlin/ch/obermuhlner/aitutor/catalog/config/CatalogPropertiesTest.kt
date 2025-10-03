package ch.obermuhlner.aitutor.catalog.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CatalogPropertiesTest {

    @Autowired
    private lateinit var catalogProperties: CatalogProperties

    @Test
    fun `should load tutor configurations from YAML`() {
        // When
        val tutors = catalogProperties.tutors

        // Then
        assertTrue(tutors.isNotEmpty(), "Should have tutor configurations")

        // Verify Spanish tutors
        val spanishTutors = tutors.filter { it.targetLanguageCode == "es-ES" }
        assertEquals(3, spanishTutors.size, "Should have 3 Spanish tutors")
        assertTrue(spanishTutors.any { it.name == "María" })
        assertTrue(spanishTutors.any { it.name == "Professor Rodríguez" })
        assertTrue(spanishTutors.any { it.name == "Carlos" })

        // Verify French tutors
        val frenchTutors = tutors.filter { it.targetLanguageCode == "fr-FR" }
        assertEquals(2, frenchTutors.size, "Should have 2 French tutors")

        // Verify German tutors
        val germanTutors = tutors.filter { it.targetLanguageCode == "de-DE" }
        assertEquals(2, germanTutors.size, "Should have 2 German tutors")

        // Verify Japanese tutors
        val japaneseTutors = tutors.filter { it.targetLanguageCode == "ja-JP" }
        assertEquals(4, japaneseTutors.size, "Should have 4 Japanese tutors")
    }

    @Test
    fun `should load course configurations from YAML`() {
        // When
        val courses = catalogProperties.courses

        // Then
        assertTrue(courses.isNotEmpty(), "Should have course configurations")

        // Verify Spanish courses
        val spanishCourses = courses.filter { it.languageCode == "es-ES" }
        assertEquals(2, spanishCourses.size, "Should have 2 Spanish courses")
        assertTrue(spanishCourses.any { it.nameEnglish.contains("Conversational") })
        assertTrue(spanishCourses.any { it.nameEnglish.contains("Travelers") })

        // Verify French courses
        val frenchCourses = courses.filter { it.languageCode == "fr-FR" }
        assertEquals(1, frenchCourses.size, "Should have 1 French course")

        // Verify German courses
        val germanCourses = courses.filter { it.languageCode == "de-DE" }
        assertEquals(1, germanCourses.size, "Should have 1 German course")

        // Verify Japanese courses
        val japaneseCourses = courses.filter { it.languageCode == "ja-JP" }
        assertEquals(1, japaneseCourses.size, "Should have 1 Japanese course")
    }

    @Test
    fun `tutor configurations should have all required fields`() {
        // Given
        val maria = catalogProperties.tutors.find { it.name == "María" }

        // Then
        assertNotNull(maria)
        maria?.let {
            assertEquals("María", it.name)
            assertEquals("👩‍🏫", it.emoji)
            assertEquals("patient coach", it.personaEnglish)
            assertTrue(it.domainEnglish.isNotBlank())
            assertTrue(it.descriptionEnglish.isNotBlank())
            assertNotNull(it.personality)
            assertEquals("es-ES", it.targetLanguageCode)
            assertEquals(0, it.displayOrder)
        }
    }

    @Test
    fun `course configurations should have all required fields`() {
        // Given
        val conversationalSpanish = catalogProperties.courses
            .find { it.nameEnglish == "Conversational Spanish" }

        // Then
        assertNotNull(conversationalSpanish)
        conversationalSpanish?.let {
            assertEquals("Conversational Spanish", it.nameEnglish)
            assertEquals("es-ES", it.languageCode)
            assertTrue(it.shortDescriptionEnglish.isNotBlank())
            assertTrue(it.descriptionEnglish.isNotBlank())
            assertNotNull(it.category)
            assertTrue(it.targetAudienceEnglish.isNotBlank())
            assertNotNull(it.startingLevel)
            assertNotNull(it.targetLevel)
            assertTrue(it.learningGoalsEnglish.isNotEmpty())
            assertEquals(0, it.displayOrder)
        }
    }

    @Test
    fun `course configurations should have learning goals`() {
        // Given
        val conversationalSpanish = catalogProperties.courses
            .find { it.nameEnglish == "Conversational Spanish" }

        // Then
        assertNotNull(conversationalSpanish)
        conversationalSpanish?.let {
            assertTrue(it.learningGoalsEnglish.size >= 3, "Should have multiple learning goals")
            assertTrue(it.learningGoalsEnglish.any { goal -> goal.contains("Greet") })
            assertTrue(it.learningGoalsEnglish.any { goal -> goal.contains("daily routines") })
        }
    }
}
