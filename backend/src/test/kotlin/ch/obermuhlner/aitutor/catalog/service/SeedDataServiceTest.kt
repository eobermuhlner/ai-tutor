package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.config.TestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.Mockito
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class SeedDataServiceTest {

    @Autowired
    private lateinit var tutorProfileRepository: TutorProfileRepository

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    private lateinit var languageRepository: LanguageRepository

    @Autowired
    private lateinit var lessonContentRepository: LessonContentRepository

    @Autowired
    private lateinit var curriculumRuleRepository: CurriculumRuleRepository

    @Autowired
    private lateinit var catalogProperties: CatalogProperties

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var unifiedCatalogImportService: UnifiedCatalogImportService

    private lateinit var fileImportService: FileImportService

    @BeforeEach
    fun setup() {
        // Clear existing data
        curriculumRuleRepository.deleteAll()
        lessonContentRepository.deleteAll()
        courseTemplateRepository.deleteAll()
        tutorProfileRepository.deleteAll()
        languageRepository.deleteAll()

        // Initialize mock
        fileImportService = Mockito.mock(FileImportService::class.java)

        // Manually seed data for testing
        val seedDataService = SeedDataService(
            tutorProfileRepository,
            courseTemplateRepository,
            languageRepository,
            lessonContentRepository,
            curriculumRuleRepository,
            catalogProperties,
            objectMapper,
            unifiedCatalogImportService,
            fileImportService
        )
        seedDataService.seedData()
    }

    @Test
    fun `should seed tutors for all languages`() {
        // Given seed data has been loaded

        // When
        val allTutors = tutorProfileRepository.findAll()

        // Then - should have tutors for Spanish, French, German, and Japanese (and more languages)
        assertTrue(allTutors.size >= 40, "Should have at least 40 tutors from all languages")

        val spanishTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es-ES")
        assertEquals(4, spanishTutors.size, "Should have 4 Spanish tutors")
        assertTrue(spanishTutors.any { it.name == "María" })
        assertTrue(spanishTutors.any { it.name == "Carlos Rodríguez" })
        assertTrue(spanishTutors.any { it.name == "Carlos" })
        assertTrue(spanishTutors.any { it.name == "Laura García" })

        val frenchTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("fr-FR")
        assertEquals(5, frenchTutors.size, "Should have 5 French tutors")
        assertTrue(frenchTutors.any { it.name == "François Dubois" })
        assertTrue(frenchTutors.any { it.name == "Céline" })
        assertTrue(frenchTutors.any { it.name == "Marie Dubois" })
        assertTrue(frenchTutors.any { it.name == "Henri Moreau" })
        assertTrue(frenchTutors.any { it.name == "Josephine Martin" })

        val germanTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("de-DE")
        assertEquals(5, germanTutors.size, "Should have 5 German tutors")
        assertTrue(germanTutors.any { it.name == "Johann Schmidt" })
        assertTrue(germanTutors.any { it.name == "Lisa Weber" })
        assertTrue(germanTutors.any { it.name == "Anna" })
        assertTrue(germanTutors.any { it.name == "Klaus Weber" })
        assertTrue(germanTutors.any { it.name == "Sarah Müller" })

        val japaneseTutors = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("ja-JP")
        assertEquals(4, japaneseTutors.size, "Should have 4 Japanese tutors")
        assertTrue(japaneseTutors.any { it.name == "Yuki (ゆき)" })
        assertTrue(japaneseTutors.any { it.name == "Tanaka-sensei (田中先生)" })
        assertTrue(japaneseTutors.any { it.name == "Kenji (けんじ)" })
        assertTrue(japaneseTutors.any { it.name == "Sakura (さくら)" })
    }

    @Test
    fun `should seed courses for all languages`() {
        // Given seed data has been loaded

        // When
        val allCourses = courseTemplateRepository.findAll()

        // Then - should have courses for all languages
        assertTrue(allCourses.size >= 5, "Should have at least 5 courses")

        val spanishCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es-ES")
        assertEquals(6, spanishCourses.size, "Should have 6 Spanish courses")
        assertTrue(spanishCourses.any { it.nameJson.contains("Conversational Spanish") })
        assertTrue(spanishCourses.any { it.nameJson.contains("Spanish for Travelers") })
        assertTrue(spanishCourses.any { it.nameJson.contains("Spanish Travel Scenarios") })
        assertTrue(spanishCourses.any { it.nameJson.contains("Free Conversation") })
        assertTrue(spanishCourses.any { it.nameJson.contains("TEST short lessons") })
        assertTrue(spanishCourses.any { it.nameJson.contains("Etymology") })

        val frenchCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("fr-FR")
        assertEquals(5, frenchCourses.size, "Should have 5 French courses")
        assertTrue(frenchCourses.any { it.nameJson.contains("Conversational French") })
        assertTrue(frenchCourses.any { it.nameJson.contains("Etymology") })

        val germanCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("de-DE")
        assertEquals(5, germanCourses.size, "Should have 5 German courses")
        assertTrue(germanCourses.any { it.nameJson.contains("Conversational German") })
        assertTrue(germanCourses.any { it.nameJson.contains("Etymology") })

        val japaneseCourses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("ja-JP")
        assertEquals(8, japaneseCourses.size, "Should have 8 Japanese courses")
        assertTrue(japaneseCourses.any { it.nameJson.contains("Japanese for Beginners") })
    }

    @Test
    fun `tutor profiles should have dual storage format`() {
        // Given
        val maria = tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es-ES")
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
    fun `courses can optionally have suggested tutor IDs`() {
        // Given - catalog-seed.yml courses don't specify suggestedTutors, so they're optional
        val conversationalSpanish = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es-ES")
            .find { it.nameJson.contains("Conversational Spanish") }

        // Then - course exists and suggestedTutorIdsJson is optional (may be null)
        assertNotNull(conversationalSpanish)
        conversationalSpanish?.let {
            // SuggestedTutorIds is optional in catalog-seed.yml format
            // If not specified, it will be null
            assertTrue(it.suggestedTutorIdsJson == null || it.suggestedTutorIdsJson!!.isNotEmpty())
        }
    }

    @Test
    fun `courses should have learning goals in JSON format`() {
        // Given
        val conversationalSpanish = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es-ES")
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
