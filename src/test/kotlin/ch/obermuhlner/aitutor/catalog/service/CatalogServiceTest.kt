package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CatalogServiceTest {

    private lateinit var catalogService: CatalogService
    private lateinit var tutorProfileRepository: TutorProfileRepository
    private lateinit var courseTemplateRepository: CourseTemplateRepository
    private lateinit var supportedLanguages: Map<String, LanguageMetadata>
    private lateinit var objectMapper: ObjectMapper

    private val testCourseId = UUID.randomUUID()
    private val testTutorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        tutorProfileRepository = mockk()
        courseTemplateRepository = mockk()
        objectMapper = ObjectMapper()

        supportedLanguages = mapOf(
            "es" to LanguageMetadata(
                code = "es",
                nameJson = """{"en": "Spanish"}""",
                flagEmoji = "🇪🇸",
                nativeName = "Español",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Spanish description"}"""
            ),
            "fr" to LanguageMetadata(
                code = "fr",
                nameJson = """{"en": "French"}""",
                flagEmoji = "🇫🇷",
                nativeName = "Français",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "French description"}"""
            )
        )

        catalogService = CatalogServiceImpl(
            tutorProfileRepository,
            courseTemplateRepository,
            supportedLanguages,
            objectMapper
        )
    }

    @Test
    fun `should get available languages`() {
        val languages = catalogService.getAvailableLanguages()

        assertEquals(2, languages.size)
        assertTrue(languages.any { it.code == "es" })
        assertTrue(languages.any { it.code == "fr" })
    }

    @Test
    fun `should get language by code`() {
        val language = catalogService.getLanguageByCode("es")

        assertNotNull(language)
        assertEquals("es", language!!.code)
        assertEquals("Español", language.nativeName)
    }

    @Test
    fun `should return null for non-existent language code`() {
        val language = catalogService.getLanguageByCode("invalid")

        assertNull(language)
    }

    @Test
    fun `should get courses for language without user level filter`() {
        val course1 = createCourse(CEFRLevel.A1)
        val course2 = createCourse(CEFRLevel.B1)
        every { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(course1, course2)

        val courses = catalogService.getCoursesForLanguage("es")

        assertEquals(2, courses.size)
        verify { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") }
    }

    @Test
    fun `should filter courses by user level`() {
        val courseA1 = createCourse(CEFRLevel.A1)
        val courseB1 = createCourse(CEFRLevel.B1)
        val courseC1 = createCourse(CEFRLevel.C1)
        every { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(courseA1, courseB1, courseC1)

        val courses = catalogService.getCoursesForLanguage("es", CEFRLevel.B1)

        assertEquals(2, courses.size)
        assertTrue(courses.all { it.startingLevel.ordinal <= CEFRLevel.B1.ordinal })
        verify { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") }
    }

    @Test
    fun `should get course by id`() {
        val course = createCourse(CEFRLevel.A1)
        every { courseTemplateRepository.findById(testCourseId) } returns Optional.of(course)

        val result = catalogService.getCourseById(testCourseId)

        assertNotNull(result)
        assertEquals(testCourseId, result!!.id)
        verify { courseTemplateRepository.findById(testCourseId) }
    }

    @Test
    fun `should return null for non-existent course id`() {
        every { courseTemplateRepository.findById(testCourseId) } returns Optional.empty()

        val result = catalogService.getCourseById(testCourseId)

        assertNull(result)
        verify { courseTemplateRepository.findById(testCourseId) }
    }

    @Test
    fun `should get courses by category`() {
        val course1 = createCourse(CEFRLevel.A1, CourseCategory.Business)
        val course2 = createCourse(CEFRLevel.A2, CourseCategory.Business)
        every { courseTemplateRepository.findByCategoryAndIsActiveTrue(CourseCategory.Business) } returns listOf(course1, course2)

        val courses = catalogService.getCoursesByCategory(CourseCategory.Business)

        assertEquals(2, courses.size)
        assertTrue(courses.all { it.category == CourseCategory.Business })
        verify { courseTemplateRepository.findByCategoryAndIsActiveTrue(CourseCategory.Business) }
    }

    @Test
    fun `should search courses by query without language filter`() {
        val course1 = createCourse(CEFRLevel.A1, name = "Spanish Basics")
        val course2 = createCourse(CEFRLevel.A2, name = "Advanced Spanish")
        every { courseTemplateRepository.findAll() } returns listOf(course1, course2)

        val courses = catalogService.searchCourses("Spanish")

        assertEquals(2, courses.size)
        verify { courseTemplateRepository.findAll() }
    }

    @Test
    fun `should search courses by query with language filter`() {
        val course1 = createCourse(CEFRLevel.A1, name = "Beginner Course")
        val course2 = createCourse(CEFRLevel.A2, name = "Intermediate Course")
        every { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(course1, course2)

        val courses = catalogService.searchCourses("Beginner", "es")

        assertEquals(1, courses.size)
        assertTrue(courses[0].nameJson.contains("Beginner"))
        verify { courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") }
    }

    @Test
    fun `should search courses by category name`() {
        val course = createCourse(CEFRLevel.A1, category = CourseCategory.Travel)
        every { courseTemplateRepository.findAll() } returns listOf(course)

        val courses = catalogService.searchCourses("Travel")

        assertEquals(1, courses.size)
        assertEquals(CourseCategory.Travel, courses[0].category)
    }

    @Test
    fun `should get tutors for language`() {
        val tutor1 = createTutor("Maria")
        val tutor2 = createTutor("Carlos")
        every { tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor1, tutor2)

        val tutors = catalogService.getTutorsForLanguage("es")

        assertEquals(2, tutors.size)
        verify { tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") }
    }

    @Test
    fun `should get tutor by id`() {
        val tutor = createTutor("Maria")
        every { tutorProfileRepository.findById(testTutorId) } returns Optional.of(tutor)

        val result = catalogService.getTutorById(testTutorId)

        assertNotNull(result)
        assertEquals(testTutorId, result!!.id)
        verify { tutorProfileRepository.findById(testTutorId) }
    }

    @Test
    fun `should return null for non-existent tutor id`() {
        every { tutorProfileRepository.findById(testTutorId) } returns Optional.empty()

        val result = catalogService.getTutorById(testTutorId)

        assertNull(result)
        verify { tutorProfileRepository.findById(testTutorId) }
    }

    @Test
    fun `should get tutors for course with suggested tutors`() {
        val tutor1 = createTutor("Maria", UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val tutor2 = createTutor("Carlos", UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val tutor3 = createTutor("Juan", UUID.fromString("00000000-0000-0000-0000-000000000003"))

        val course = createCourse(
            CEFRLevel.A1,
            suggestedTutorIds = """["00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000001"]"""
        )

        every { courseTemplateRepository.findById(testCourseId) } returns Optional.of(course)
        every { tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor1, tutor2, tutor3)

        val tutors = catalogService.getTutorsForCourse(testCourseId)

        assertEquals(3, tutors.size)
        // Suggested tutors should be first, in the suggested order
        assertEquals("Carlos", tutors[0].name)
        assertEquals("Maria", tutors[1].name)
        assertEquals("Juan", tutors[2].name)
    }

    @Test
    fun `should get all tutors for course without suggested tutors`() {
        val tutor1 = createTutor("Maria")
        val tutor2 = createTutor("Carlos")
        val course = createCourse(CEFRLevel.A1, suggestedTutorIds = null)

        every { courseTemplateRepository.findById(testCourseId) } returns Optional.of(course)
        every { tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor1, tutor2)

        val tutors = catalogService.getTutorsForCourse(testCourseId)

        assertEquals(2, tutors.size)
    }

    @Test
    fun `should return empty list for non-existent course in getTutorsForCourse`() {
        every { courseTemplateRepository.findById(testCourseId) } returns Optional.empty()

        val tutors = catalogService.getTutorsForCourse(testCourseId)

        assertEquals(0, tutors.size)
    }

    @Test
    fun `should handle invalid JSON in suggested tutor IDs`() {
        val course = createCourse(CEFRLevel.A1, suggestedTutorIds = "invalid-json")
        val tutor1 = createTutor("Maria")

        every { courseTemplateRepository.findById(testCourseId) } returns Optional.of(course)
        every { tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(tutor1)

        val tutors = catalogService.getTutorsForCourse(testCourseId)

        // Should fall back to returning all tutors
        assertEquals(1, tutors.size)
    }

    @Test
    fun `should create tutor successfully`() {
        val request = CreateTutorRequest(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor",
            culturalBackground = "From Madrid",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 1
        )

        val savedTutor = createTutor("Maria")
        every { tutorProfileRepository.save(any()) } returns savedTutor

        val result = catalogService.createTutor(request)

        assertNotNull(result)
        assertEquals("Maria", result.name)
        verify { tutorProfileRepository.save(any()) }
    }

    @Test
    fun `should create tutor without cultural background`() {
        val request = CreateTutorRequest(
            name = "Carlos",
            emoji = "👨‍🏫",
            personaEnglish = "Friendly teacher",
            domainEnglish = "Business",
            descriptionEnglish = "Business specialist",
            culturalBackground = null,
            personality = TutorPersonality.Professional,
            teachingStyle = TeachingStyle.Guided,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 2
        )

        val savedTutor = createTutor("Carlos")
        every { tutorProfileRepository.save(any()) } returns savedTutor

        val result = catalogService.createTutor(request)

        assertNotNull(result)
        verify { tutorProfileRepository.save(any()) }
    }

    private fun createCourse(
        level: CEFRLevel,
        category: CourseCategory = CourseCategory.General,
        name: String = "Test Course",
        suggestedTutorIds: String? = null
    ): CourseTemplateEntity {
        return CourseTemplateEntity(
            id = testCourseId,
            languageCode = "es",
            nameJson = """{"en": "$name"}""",
            shortDescriptionJson = """{"en": "Short description"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = category,
            targetAudienceJson = """{"en": "All levels"}""",
            startingLevel = level,
            targetLevel = level,
            estimatedWeeks = 8,
            suggestedTutorIdsJson = suggestedTutorIds,
            defaultPhase = ConversationPhase.Auto,
            learningGoalsJson = """{"en": ["Goal 1"]}""",
            displayOrder = 1
        )
    }

    private fun createTutor(name: String, id: UUID = testTutorId): TutorProfileEntity {
        return TutorProfileEntity(
            id = id,
            name = name,
            emoji = "👩‍🏫",
            personaEnglish = "Patient teacher",
            domainEnglish = "General",
            descriptionEnglish = "A patient tutor",
            personaJson = """{"en": "Patient teacher"}""",
            domainJson = """{"en": "General"}""",
            descriptionJson = """{"en": "A patient tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1
        )
    }
}
