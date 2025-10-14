package ch.obermuhlner.aitutor.catalog.config

import ch.obermuhlner.aitutor.config.TestConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class CatalogPropertiesTest {

    @Autowired
    private lateinit var catalogProperties: CatalogProperties

    @Test
    fun `should load tutor archetypes from YAML`() {
        // When
        val archetypes = catalogProperties.tutorArchetypes

        // Then
        assertTrue(archetypes.isNotEmpty(), "Should have tutor archetypes")

        // Verify common archetypes exist
        assertTrue(archetypes.any { it.id == "encouraging-general" })
        assertTrue(archetypes.any { it.id == "strict-academic" })
        assertTrue(archetypes.any { it.id == "casual-friend" })

        // Verify specialized archetypes
        assertTrue(archetypes.any { it.id == "romaji-specialist" })
        assertTrue(archetypes.any { it.id == "hiragana-bridge" })
    }

    @Test
    fun `should load tutor variants for each language`() {
        // When
        val languages = catalogProperties.languages

        // Then
        assertTrue(languages.isNotEmpty(), "Should have language configurations")

        // Verify Spanish tutors
        val spanish = languages.find { it.code == "es-ES" }
        assertNotNull(spanish)
        assertEquals(4, spanish?.tutorVariants?.size, "Should have 4 Spanish tutor variants")
        assertTrue(spanish?.tutorVariants?.any { it.name == "María" } ?: false)
        assertTrue(spanish?.tutorVariants?.any { it.name == "Professor Rodríguez" } ?: false)
        assertTrue(spanish?.tutorVariants?.any { it.name == "Carlos" } ?: false)
        assertTrue(spanish?.tutorVariants?.any { it.name == "Laura" } ?: false)

        // Verify French tutors
        val french = languages.find { it.code == "fr-FR" }
        assertEquals(2, french?.tutorVariants?.size, "Should have 2 French tutor variants")

        // Verify German tutors
        val german = languages.find { it.code == "de-DE" }
        assertEquals(2, german?.tutorVariants?.size, "Should have 2 German tutor variants")

        // Verify Japanese tutors
        val japanese = languages.find { it.code == "ja-JP" }
        assertEquals(4, japanese?.tutorVariants?.size, "Should have 4 Japanese tutor variants")
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
    fun `tutor archetypes should have all required fields`() {
        // Given
        val encouragingArchetype = catalogProperties.tutorArchetypes.find { it.id == "encouraging-general" }

        // Then
        assertNotNull(encouragingArchetype)
        encouragingArchetype?.let {
            assertEquals("encouraging-general", it.id)
            assertEquals("👩‍🏫", it.emoji)
            assertEquals("patient coach", it.personaEnglish)
            assertTrue(it.domainEnglish.isNotBlank())
            assertTrue(it.descriptionTemplateEnglish.contains("{culturalNotes}"), "Should have template placeholder")
            assertNotNull(it.personality)
            assertEquals(0, it.displayOrder)
        }
    }

    @Test
    fun `tutor variants should reference valid archetypes`() {
        // Given
        val spanish = catalogProperties.languages.find { it.code == "es-ES" }
        val mariaVariant = spanish?.tutorVariants?.find { it.name == "María" }

        // Then
        assertNotNull(mariaVariant)
        mariaVariant?.let {
            assertEquals("María", it.name)
            assertEquals("encouraging-general", it.archetypeId)
            assertTrue(it.culturalNotes.isNotBlank())

            // Verify archetype exists
            val archetype = catalogProperties.tutorArchetypes.find { arch -> arch.id == it.archetypeId }
            assertNotNull(archetype, "Referenced archetype should exist")
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
