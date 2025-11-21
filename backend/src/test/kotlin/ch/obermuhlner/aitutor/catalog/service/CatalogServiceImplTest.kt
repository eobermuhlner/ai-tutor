package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.dto.UpdateTutorRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogServiceImplTest {

    private val tutorRepository = mockk<TutorProfileRepository>()
    private val courseRepository = mockk<CourseTemplateRepository>()
    private val languageRepository = mockk<LanguageRepository>()
    private val englishLanguage = LanguageEntity(
        code = "en",
        nameJson = """{"en": "English"}""",
        flagEmoji = "🇬🇧",
        nativeName = "English",
        difficulty = Difficulty.Easy,
        descriptionJson = """{"en": "English"}""",
        isActive = true,
        displayOrder = 0
    )
    private val spanishLanguage = LanguageEntity(
        code = "es",
        nameJson = """{"en": "Spanish", "es": "Español"}""",
        flagEmoji = "🇪🇸",
        nativeName = "Español",
        difficulty = Difficulty.Easy,
        descriptionJson = """{"en": "Spanish"}""",
        isActive = true,
        displayOrder = 1
    )
    private val objectMapper = ObjectMapper()
    private val service = CatalogServiceImpl(tutorRepository, courseRepository, languageRepository, objectMapper)

    @Test
    fun `getAvailableLanguages should return all supported languages`() {
        every { languageRepository.findByIsActiveTrue() } returns listOf(englishLanguage, spanishLanguage)

        val result = service.getAvailableLanguages()

        assertEquals(2, result.size)
        assertTrue(result.any { it.code == "en" })
        assertTrue(result.any { it.code == "es" })
        verify { languageRepository.findByIsActiveTrue() }
    }

    @Test
    fun `getLanguageByCode should return language metadata`() {
        every { languageRepository.findById("es") } returns Optional.of(spanishLanguage)

        val result = service.getLanguageByCode("es")

        assertNotNull(result)
        assertEquals("es", result?.code)
        assertEquals("Español", result?.nativeName)
        verify { languageRepository.findById("es") }
    }

    @Test
    fun `getLanguageByCode should return null for unknown code`() {
        every { languageRepository.findById("fr") } returns Optional.empty()

        val result = service.getLanguageByCode("fr")

        assertNull(result)
        verify { languageRepository.findById("fr") }
    }

    @Test
    fun `getLanguageByCode should return null for inactive language`() {
        val inactiveLanguage = LanguageEntity(
            code = "fr",
            nameJson = """{"en": "French"}""",
            flagEmoji = "🇫🇷",
            nativeName = "Français",
            difficulty = Difficulty.Medium,
            descriptionJson = """{"en": "French"}""",
            isActive = false,
            displayOrder = 2
        )
        every { languageRepository.findById("fr") } returns Optional.of(inactiveLanguage)

        val result = service.getLanguageByCode("fr")

        assertNull(result)
        verify { languageRepository.findById("fr") }
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
    fun `getCourseById should return null for non-existent course`() {
        val id = UUID.randomUUID()
        every { courseRepository.findById(id) } returns Optional.empty()

        val result = service.getCourseById(id)

        assertNull(result)
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
    fun `searchCourses should find courses by name, description, and category`() {
        val course1 = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Travel Spanish"}""",
            shortDescriptionJson = """{"en": "Travel"}""",
            descriptionJson = """{"en": "Spanish for travel"}""",
            category = CourseCategory.Conversational,
            targetAudienceJson = """{"en": "Travelers"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            learningGoalsJson = """{"en": "Learn travel Spanish"}""",
            isActive = true,
            displayOrder = 0
        )
        val course2 = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Business Spanish"}""",
            shortDescriptionJson = """{"en": "Business"}""",
            descriptionJson = """{"en": "Spanish for business"}""",
            category = CourseCategory.Business,
            targetAudienceJson = """{"en": "Businessmen"}""",
            startingLevel = CEFRLevel.B1,
            targetLevel = CEFRLevel.B2,
            learningGoalsJson = """{"en": "Learn business Spanish"}""",
            isActive = true,
            displayOrder = 1
        )
        every { courseRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es") } returns listOf(course1, course2)

        val result = service.searchCourses("business", "es")

        assertEquals(1, result.size)
        assertTrue(result[0].category.name.contains("Business", ignoreCase = true))
    }

    @Test
    fun `searchCourses should find courses without language filter`() {
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
        every { courseRepository.findAll() } returns listOf(course)

        val result = service.searchCourses("travel", null)

        assertEquals(1, result.size)
        assertTrue(result[0].nameJson.contains("Travel", ignoreCase = true))
    }

    @Test
    fun `getTutorsForLanguage should return tutors for global context`() {
        val tutor = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Experienced tutor",
            personaJson = """{"en": "Friendly tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Experienced tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { tutorRepository.findByTargetLanguageCodeAndIsActiveTrueAndIsGlobalTrueOrderByDisplayOrder("es") } returns listOf(tutor)

        val result = service.getTutorsForLanguage("es", userId = null)

        assertEquals(1, result.size)
        assertEquals("Maria", result[0].name)
    }

    @Test
    fun `getTutorsForLanguage should return tutors for user context`() {
        val userId = UUID.randomUUID()
        val tutor = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "Experienced tutor",
            personaJson = """{"en": "Friendly tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "Experienced tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { tutorRepository.findVisibleTutorsForUser("es", userId) } returns listOf(tutor)

        val result = service.getTutorsForLanguage("es", userId = userId)

        assertEquals(1, result.size)
        assertEquals("Maria", result[0].name)
    }

    @Test
    fun `getTutorById should return global tutor`() {
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
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = true
        )
        every { tutorRepository.findById(id) } returns Optional.of(tutor)

        val result = service.getTutorById(id, userId = UUID.randomUUID())

        assertNotNull(result)
        assertEquals(id, result?.id)
    }

    @Test
    fun `getTutorById should return user-specific tutor to owner`() {
        val userId = UUID.randomUUID()
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
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = false,
            createdByUserId = userId
        )
        every { tutorRepository.findById(id) } returns Optional.of(tutor)

        val result = service.getTutorById(id, userId = userId)

        assertNotNull(result)
        assertEquals(id, result?.id)
    }

    @Test
    fun `getTutorById should not return user-specific tutor to non-owner`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
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
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = false,
            createdByUserId = userId
        )
        every { tutorRepository.findById(id) } returns Optional.of(tutor)

        val result = service.getTutorById(id, userId = otherUserId)

        assertNull(result)
    }

    @Test
    fun `getTutorById should return null for non-existent tutor`() {
        val id = UUID.randomUUID()
        every { tutorRepository.findById(id) } returns Optional.empty()

        val result = service.getTutorById(id, userId = UUID.randomUUID())

        assertNull(result)
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
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )
        every { courseRepository.findById(courseId) } returns Optional.of(course)
        every { tutorRepository.findByTargetLanguageCodeAndIsActiveTrueAndIsGlobalTrueOrderByDisplayOrder("es") } returns listOf(tutor)

        val result = service.getTutorsForCourse(courseId, userId = null)

        assertEquals(1, result.size)
        assertEquals("Maria", result[0].name)
    }

    @Test
    fun `getTutorsForCourse should return suggested tutors first then remaining`() {
        val courseId = UUID.randomUUID()
        val firstTutorId = UUID.randomUUID()
        val secondTutorId = UUID.randomUUID()
        val thirdTutorId = UUID.randomUUID()

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
            suggestedTutorIdsJson = """["${firstTutorId}", "${secondTutorId}"]""",
            isActive = true,
            displayOrder = 0
        )

        val firstTutor = TutorProfileEntity(
            id = firstTutorId,
            name = "First Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "First tutor",
            domainEnglish = "General",
            descriptionEnglish = "First tutor",
            personaJson = """{"en": "First tutor"}""",
            domainJson = """{"en": "General"}""",
            descriptionJson = """{"en": "First tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0
        )

        val secondTutor = TutorProfileEntity(
            id = secondTutorId,
            name = "Second Tutor",
            emoji = "👨‍🏫",
            personaEnglish = "Second tutor",
            domainEnglish = "General",
            descriptionEnglish = "Second tutor",
            personaJson = """{"en": "Second tutor"}""",
            domainJson = """{"en": "General"}""",
            descriptionJson = """{"en": "Second tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 1
        )

        val thirdTutor = TutorProfileEntity(
            id = thirdTutorId,
            name = "Third Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "Third tutor",
            domainEnglish = "General",
            descriptionEnglish = "Third tutor",
            personaJson = """{"en": "Third tutor"}""",
            domainJson = """{"en": "General"}""",
            descriptionJson = """{"en": "Third tutor"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 2
        )

        every { courseRepository.findById(courseId) } returns Optional.of(course)
        every { tutorRepository.findByTargetLanguageCodeAndIsActiveTrueAndIsGlobalTrueOrderByDisplayOrder("es") } returns
            listOf(firstTutor, secondTutor, thirdTutor)

        val result = service.getTutorsForCourse(courseId, userId = null)

        assertEquals(3, result.size)
        assertEquals("First Tutor", result[0].name)
        assertEquals("Second Tutor", result[1].name)
        assertEquals("Third Tutor", result[2].name)
    }

    @Test
    fun `createTutor should create global tutor as admin`() {
        val userId = UUID.randomUUID()
        val request = CreateTutorRequest(
            name = "New Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "Friendly tutor persona",
            domainEnglish = "General domain",
            descriptionEnglish = "Friendly tutor description",
            culturalBackground = "Spanish culture",
            location = "Spain",
            age = 30,
            gender = TutorGender.Female,
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = true
        )

        val mockResult1 = mockk<TutorProfileEntity>()
        every { mockResult1.id } returns UUID.randomUUID()
        every { mockResult1.isGlobal } returns true
        every { mockResult1.name } returns "New Tutor"
        every { mockResult1.createdByUserId } returns userId
        every { tutorRepository.save(any()) } returns mockResult1

        val result = service.createTutor(request, userId, isAdminRequest = true)

        assertTrue(result.isGlobal)
        assertEquals("New Tutor", result.name)
        verify { tutorRepository.save(any()) }
    }

    @Test
    fun `createTutor should create user-specific tutor as regular user`() {
        val userId = UUID.randomUUID()
        val request = CreateTutorRequest(
            name = "User Tutor",
            emoji = "👨‍🏫",
            personaEnglish = "User tutor persona",
            domainEnglish = "General domain",
            descriptionEnglish = "User tutor description",
            culturalBackground = "American culture",
            location = "USA",
            age = 25,
            gender = TutorGender.Male,
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Guided,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 1,
            isGlobal = null
        )

        val mockResult2 = mockk<TutorProfileEntity>()
        every { mockResult2.id } returns UUID.randomUUID()
        every { mockResult2.isGlobal } returns false
        every { mockResult2.name } returns "User Tutor"
        every { mockResult2.createdByUserId } returns userId
        every { tutorRepository.save(any()) } returns mockResult2

        val result = service.createTutor(request, userId, isAdminRequest = false)

        assertEquals(false, result.isGlobal)
        assertEquals("User Tutor", result.name)
        assertEquals(userId, result.createdByUserId)
        verify { tutorRepository.save(any()) }
    }

    @Test
    fun `updateTutor should update tutor by admin`() {
        val tutorId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val existingTutor = TutorProfileEntity(
            id = tutorId,
            name = "Old Name",
            emoji = "👩‍🏫",
            personaEnglish = "Old persona",
            domainEnglish = "Old domain",
            descriptionEnglish = "Old description",
            personaJson = """{"en": "Old persona"}""",
            domainJson = """{"en": "Old domain"}""",
            descriptionJson = """{"en": "Old description"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = true
        )
        val request = UpdateTutorRequest(
            name = "New Name",
            personaEnglish = "New persona",
            descriptionEnglish = "New description"
        )

        every { tutorRepository.findById(tutorId) } returns Optional.of(existingTutor)
        every { tutorRepository.save(any()) } returnsArgument 0

        val result = service.updateTutor(tutorId, request, userId, isAdmin = true)

        assertNotNull(result)
        assertEquals("New Name", result?.name)
        assertEquals("New persona", result?.personaEnglish)
        verify { tutorRepository.save(any()) }
    }

    @Test
    fun `updateTutor should update tutor by owner`() {
        val tutorId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val existingTutor = TutorProfileEntity(
            id = tutorId,
            name = "Old Name",
            emoji = "👩‍🏫",
            personaEnglish = "Old persona",
            domainEnglish = "Old domain",
            descriptionEnglish = "Old description",
            personaJson = """{"en": "Old persona"}""",
            domainJson = """{"en": "Old domain"}""",
            descriptionJson = """{"en": "Old description"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = false,
            createdByUserId = userId
        )
        val request = UpdateTutorRequest(
            name = "New Name"
        )

        every { tutorRepository.findById(tutorId) } returns Optional.of(existingTutor)
        every { tutorRepository.save(any()) } returnsArgument 0

        val result = service.updateTutor(tutorId, request, userId, isAdmin = false)

        assertNotNull(result)
        assertEquals("New Name", result?.name)
        verify { tutorRepository.save(any()) }
    }

    @Test
    fun `updateTutor should return null when user is not admin or owner`() {
        val tutorId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val existingTutor = TutorProfileEntity(
            id = tutorId,
            name = "Old Name",
            emoji = "👩‍🏫",
            personaEnglish = "Old persona",
            domainEnglish = "Old domain",
            descriptionEnglish = "Old description",
            personaJson = """{"en": "Old persona"}""",
            domainJson = """{"en": "Old domain"}""",
            descriptionJson = """{"en": "Old description"}""",
            location = null,
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            isActive = true,
            displayOrder = 0,
            isGlobal = false,
            createdByUserId = userId
        )
        val request = UpdateTutorRequest(
            name = "New Name"
        )

        every { tutorRepository.findById(tutorId) } returns Optional.of(existingTutor)

        val result = service.updateTutor(tutorId, request, otherUserId, isAdmin = false)

        assertNull(result)
    }

    @Test
    fun `updateTutor should return null when tutor not found`() {
        val tutorId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val request = UpdateTutorRequest(
            name = "New Name"
        )

        every { tutorRepository.findById(tutorId) } returns Optional.empty()

        val result = service.updateTutor(tutorId, request, userId, isAdmin = false)

        assertNull(result)
    }
}
