package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.*
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class CatalogServiceImplTest {

    private val tutorRepository = mockk<TutorProfileRepository>()
    private val courseRepository = mockk<CourseTemplateRepository>()
    private val supportedLanguages = mapOf(
        "en" to LanguageMetadata(
            code = "en",
            nameJson = """{"en": "English"}""",
            flagEmoji = "🇬🇧",
            nativeName = "English",
            difficulty = Difficulty.Easy,
            descriptionJson = """{"en": "English"}"""
        ),
        "es" to LanguageMetadata(
            code = "es",
            nameJson = """{"en": "Spanish", "es": "Español"}""",
            flagEmoji = "🇪🇸",
            nativeName = "Español",
            difficulty = Difficulty.Easy,
            descriptionJson = """{"en": "Spanish"}"""
        )
    )
    private val objectMapper = ObjectMapper()
    private val service = CatalogServiceImpl(tutorRepository, courseRepository, supportedLanguages, objectMapper)

    @Test
    fun `getAvailableLanguages should return all supported languages`() {
        val result = service.getAvailableLanguages()

        assertEquals(2, result.size)
        assertTrue(result.any { it.code == "en" })
        assertTrue(result.any { it.code == "es" })
    }

    @Test
    fun `getLanguageByCode should return language metadata`() {
        val result = service.getLanguageByCode("es")

        assertNotNull(result)
        assertEquals("es", result?.code)
        assertEquals("Español", result?.nativeName)
    }

    @Test
    fun `getLanguageByCode should return null for unknown code`() {
        val result = service.getLanguageByCode("fr")

        assertNull(result)
    }

    @Test
    fun `getCoursesForLanguage should return courses for language`() {
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Spanish Basics"}""",
            shortDescriptionJson = """{"en": "Basic Spanish"}""",
            descriptionJson = """{"en": "Learn Spanish basics"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn basics"}""",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(course)

        val result = service.getCoursesForLanguage("es")

        assertEquals(1, result.size)
        assertEquals("es", result[0].languageCode)
        verify { courseRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") }
    }

    @Test
    fun `getCoursesForLanguage should filter by user level`() {
        val courseA1 = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Spanish A1"}""",
            shortDescriptionJson = """{"en": "A1"}""",
            descriptionJson = """{"en": "A1"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn A1"}""",
            isActive = true,
            displayOrder = 0
        )
        val courseB2 = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Spanish B2"}""",
            shortDescriptionJson = """{"en": "B2"}""",
            descriptionJson = """{"en": "B2"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Advanced"}""",
            startingLevel = CEFRLevel.B2,
            targetLevel = CEFRLevel.C1,
            learningGoalsJson = """{"en": "Learn B2"}""",
            isActive = true,
            displayOrder = 1
        )
        every { courseRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(courseA1, courseB2)

        val result = service.getCoursesForLanguage("es", CEFRLevel.A1)

        assertEquals(1, result.size)
        assertEquals(CEFRLevel.A1, result[0].startingLevel)
    }

    @Test
    fun `getCourseById should return course`() {
        val id = UUID.randomUUID()
        val course = CourseTemplateEntity(
            id = id,
            languageCode = "es",
            nameJson = """{"en": "Spanish Basics"}""",
            shortDescriptionJson = """{"en": "Basic Spanish"}""",
            descriptionJson = """{"en": "Learn Spanish basics"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn basics"}""",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findById(id) } returns Optional.of(course)

        val result = service.getCourseById(id)

        assertNotNull(result)
        assertEquals(id, result?.id)
    }

    @Test
    fun `getCoursesByCategory should return courses by category`() {
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Travel Spanish"}""",
            shortDescriptionJson = """{"en": "Travel"}""",
            descriptionJson = """{"en": "Spanish for travel"}""",
            category = CourseCategory.Travel,
            targetAudienceJson = """{"en": "Travelers"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn travel Spanish"}""",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findByCategoryAndIsActiveTrue(CourseCategory.Travel) } returns listOf(course)

        val result = service.getCoursesByCategory(CourseCategory.Travel)

        assertEquals(1, result.size)
        assertEquals(CourseCategory.Travel, result[0].category)
    }

    @Test
    fun `searchCourses should find courses by query`() {
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Travel Spanish"}""",
            shortDescriptionJson = """{"en": "Travel"}""",
            descriptionJson = """{"en": "Spanish for travel"}""",
            category = CourseCategory.Travel,
            targetAudienceJson = """{"en": "Travelers"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn travel Spanish"}""",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(course)

        val result = service.searchCourses("travel", "es")

        assertEquals(1, result.size)
        assertTrue(result[0].nameJson.contains("Travel", ignoreCase = true))
    }

    @Test
    fun `getTutorsForLanguage should return tutors`() {
        val tutor = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Experienced tutor",
            personaJson = """{"en": "Friendly tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Experienced tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { tutorRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor)

        val result = service.getTutorsForLanguage("es")

        assertEquals(1, result.size)
        assertEquals("Maria", result[0].name)
    }

    @Test
    fun `getTutorById should return tutor`() {
        val id = UUID.randomUUID()
        val tutor = TutorProfileEntity(
            id = id,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Experienced tutor",
            personaJson = """{"en": "Friendly tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Experienced tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { tutorRepository.findById(id) } returns Optional.of(tutor)

        val result = service.getTutorById(id)

        assertNotNull(result)
        assertEquals(id, result?.id)
    }

    @Test
    fun `getTutorsForCourse should return all tutors when no suggested tutors`() {
        val courseId = UUID.randomUUID()
        val course = CourseTemplateEntity(
            id = courseId,
            languageCode = "es",
            nameJson = """{"en": "Spanish Basics"}""",
            shortDescriptionJson = """{"en": "Basic Spanish"}""",
            descriptionJson = """{"en": "Learn Spanish basics"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn basics"}""",
            suggestedTutorIdsJson = null,
            isActive = true,
            displayOrder = 0
        )
        val tutor = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Experienced tutor",
            personaJson = """{"en": "Friendly tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Experienced tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findById(courseId) } returns Optional.of(course)
        every { tutorRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor)

        val result = service.getTutorsForCourse(courseId)

        assertEquals(1, result.size)
        assertEquals("Maria", result[0].name)
    }
}
