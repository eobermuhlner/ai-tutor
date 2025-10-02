package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class SeedDataServiceTest {

    @Autowired
    private lateinit var tutorProfileRepository: TutorProfileRepository

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    private lateinit var catalogProperties: CatalogProperties

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        // Clear existing data
        courseTemplateRepository.deleteAll()
        tutorProfileRepository.deleteAll()

        // Manually seed data for testing
        val seedDataService = SeedDataService(
            tutorProfileRepository,
            courseTemplateRepository,
            catalogProperties,
            objectMapper
        )
        seedDataService.seedData()
    }

    @Test
    fun `should seed tutors for all languages`() {
        // Given seed data has been loaded

        // When
        val allTutors = tutorProfileRepository.findAll()

        // Then - should have tutors for Spanish, French, German, and Japanese
        assertTrue(allTutors.size >= 9, "Should have at least 9 tutors (3 Spanish, 2 French, 2 German, 2 Japanese)")

        val spanishTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")
        assertEquals(3, spanishTutors.size, "Should have 3 Spanish tutors")
        assertTrue(spanishTutors.any { it.name == "María" })
        assertTrue(spanishTutors.any { it.name == "Professor Rodríguez" })
        assertTrue(spanishTutors.any { it.name == "Carlos" })

        val frenchTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("fr")
        assertEquals(2, frenchTutors.size, "Should have 2 French tutors")
        assertTrue(frenchTutors.any { it.name == "François" })
        assertTrue(frenchTutors.any { it.name == "Céline" })

        val germanTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("de")
        assertEquals(2, germanTutors.size, "Should have 2 German tutors")
        assertTrue(germanTutors.any { it.name == "Herr Schmidt" })
        assertTrue(germanTutors.any { it.name == "Anna" })

        val japaneseTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("ja")
        assertEquals(2, japaneseTutors.size, "Should have 2 Japanese tutors")
        assertTrue(japaneseTutors.any { it.name == "Yuki" })
        assertTrue(japaneseTutors.any { it.name == "Tanaka-sensei" })
    }

    @Test
    fun `should seed courses for all languages`() {
        // Given seed data has been loaded

        // When
        val allCourses = courseTemplateRepository.findAll()

        // Then - should have courses for all languages
        assertTrue(allCourses.size >= 5, "Should have at least 5 courses")

        val spanishCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")
        assertEquals(2, spanishCourses.size, "Should have 2 Spanish courses")
        assertTrue(spanishCourses.any { it.nameJson.contains("Conversational Spanish") })
        assertTrue(spanishCourses.any { it.nameJson.contains("Spanish for Travelers") })

        val frenchCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("fr")
        assertEquals(1, frenchCourses.size, "Should have 1 French course")
        assertTrue(frenchCourses.any { it.nameJson.contains("French Conversation") })

        val germanCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("de")
        assertEquals(1, germanCourses.size, "Should have 1 German course")
        assertTrue(germanCourses.any { it.nameJson.contains("German Fundamentals") })

        val japaneseCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("ja")
        assertEquals(1, japaneseCourses.size, "Should have 1 Japanese course")
        assertTrue(japaneseCourses.any { it.nameJson.contains("Japanese for Beginners") })
    }

    @Test
    fun `tutor profiles should have dual storage format`() {
        // Given
        val maria = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")
            .find { it.name == "María" }

        // Then
        assertNotNull(maria)
        maria?.let {
            // English fields for AI
            assertEquals("patient coach", it.personaEnglish)
            assertEquals("general conversation, grammar, typography", it.domainEnglish)
            assertTrue(it.descriptionEnglish.contains("Madrid"))

            // JSON fields for UI
            assertTrue(it.personaJson.contains("\"en\""))
            assertTrue(it.personaJson.contains("patient coach"))
            assertTrue(it.domainJson.contains("\"en\""))
            assertTrue(it.descriptionJson.contains("\"en\""))
        }
    }

    @Test
    fun `courses should have suggested tutor IDs`() {
        // Given
        val conversationalSpanish = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")
            .find { it.nameJson.contains("Conversational Spanish") }

        // Then
        assertNotNull(conversationalSpanish)
        conversationalSpanish?.let {
            assertNotNull(it.suggestedTutorIdsJson)
            val tutorIds = objectMapper.readValue(it.suggestedTutorIdsJson, List::class.java)
            assertEquals(3, tutorIds.size, "Conversational Spanish should suggest all 3 Spanish tutors")
        }
    }

    @Test
    fun `courses should have learning goals in JSON format`() {
        // Given
        val conversationalSpanish = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")
            .find { it.nameJson.contains("Conversational Spanish") }

        // Then
        assertNotNull(conversationalSpanish)
        conversationalSpanish?.let {
            assertTrue(it.learningGoalsJson.contains("\"en\""))
            assertTrue(it.learningGoalsJson.contains("Greet people"))
            assertTrue(it.learningGoalsJson.contains("daily routines"))
        }
    }
}
