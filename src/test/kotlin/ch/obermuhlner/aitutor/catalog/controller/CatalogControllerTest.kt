package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.language.service.LocalizationService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CatalogController::class])
@AutoConfigureJsonTesters
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class CatalogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var catalogService: CatalogService

    @MockkBean(relaxed = true)
    private lateinit var localizationService: LocalizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    private val testCourseId = UUID.randomUUID()
    private val testTutorId = UUID.randomUUID()

    @Test
    @WithMockUser
    fun `should list available languages`() {
        val language = LanguageMetadata(
            code = "es",
            nameJson = """{"en": "Spanish"}""",
            flagEmoji = "🇪🇸",
            nativeName = "Español",
            difficulty = Difficulty.Medium,
            descriptionJson = """{"en": "Spanish description"}"""
        )
        every { catalogService.getAvailableLanguages() } returns listOf(language)
        every { catalogService.getCoursesForLanguage("es") } returns emptyList()

        mockMvc.perform(
            get("/api/v1/catalog/languages?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].code").value("es"))
            .andExpect(jsonPath("$[0].name").value("Spanish"))

        verify { catalogService.getAvailableLanguages() }
    }

    @Test
    @WithMockUser
    fun `should list courses for language`() {
        val course = CourseTemplateEntity(
            id = testCourseId,
            languageCode = "es",
            nameJson = """{"en": "Beginner Spanish"}""",
            shortDescriptionJson = """{"en": "Learn Spanish basics"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"]}""",
            displayOrder = 1
        )
        every { catalogService.getCoursesForLanguage("es") } returns listOf(course)
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } returns "Beginner Spanish"

        mockMvc.perform(
            get("/api/v1/catalog/languages/es/courses?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testCourseId.toString()))
            .andExpect(jsonPath("$[0].languageCode").value("es"))

        verify { catalogService.getCoursesForLanguage("es") }
    }

    @Test
    @WithMockUser
    fun `should get course detail`() {
        val course = CourseTemplateEntity(
            id = testCourseId,
            languageCode = "es",
            nameJson = """{"en": "Beginner Spanish"}""",
            shortDescriptionJson = """{"en": "Learn Spanish basics"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"]}""",
            displayOrder = 1
        )
        val tutor = TutorProfileEntity(
            id = testTutorId,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor",
            personaJson = """{"en": "Patient teacher"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "A patient tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            displayOrder = 1
        )
        every { catalogService.getCourseById(testCourseId) } returns course
        every { catalogService.getTutorsForCourse(testCourseId) } returns listOf(tutor)
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } returns "Beginner Spanish"

        mockMvc.perform(
            get("/api/v1/catalog/courses/$testCourseId?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testCourseId.toString()))

        verify { catalogService.getCourseById(testCourseId) }
        verify { catalogService.getTutorsForCourse(testCourseId) }
    }

    @Test
    @WithMockUser
    fun `should list tutors for language`() {
        val tutor = TutorProfileEntity(
            id = testTutorId,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor",
            personaJson = """{"en": "Patient teacher"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "A patient tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            displayOrder = 1
        )
        every { catalogService.getTutorsForLanguage("es") } returns listOf(tutor)
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } returns "Patient teacher"

        mockMvc.perform(
            get("/api/v1/catalog/languages/es/tutors?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testTutorId.toString()))
            .andExpect(jsonPath("$[0].name").value("Maria"))

        verify { catalogService.getTutorsForLanguage("es") }
    }

    @Test
    @WithMockUser
    fun `should get tutor detail`() {
        val tutor = TutorProfileEntity(
            id = testTutorId,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor",
            personaJson = """{"en": "Patient teacher"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "A patient tutor"}""",
            personality = TutorPersonality.Encouraging,
            targetLanguageCode = "es",
            displayOrder = 1
        )
        every { catalogService.getTutorById(testTutorId) } returns tutor
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } returns "Patient teacher"

        mockMvc.perform(
            get("/api/v1/catalog/tutors/$testTutorId?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testTutorId.toString()))
            .andExpect(jsonPath("$.name").value("Maria"))

        verify { catalogService.getTutorById(testTutorId) }
    }

    @Test
    @WithMockUser
    fun `should return empty list when no courses for language`() {
        every { catalogService.getCoursesForLanguage("fr") } returns emptyList()

        mockMvc.perform(
            get("/api/v1/catalog/languages/fr/courses?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)

        verify { catalogService.getCoursesForLanguage("fr") }
    }

    @Test
    @WithMockUser
    fun `should return empty list when no tutors for language`() {
        every { catalogService.getTutorsForLanguage("it") } returns emptyList()

        mockMvc.perform(
            get("/api/v1/catalog/languages/it/tutors?locale=en")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)

        verify { catalogService.getTutorsForLanguage("it") }
    }

    @Test
    @WithMockUser
    fun `should handle user level filter for courses`() {
        val course = CourseTemplateEntity(
            id = testCourseId,
            languageCode = "es",
            nameJson = """{"en": "Advanced Spanish"}""",
            shortDescriptionJson = """{"en": "For B2+ learners"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Advanced"}""",
            startingLevel = CEFRLevel.B2,
            targetLevel = CEFRLevel.C1,
            estimatedWeeks = 12,
            learningGoalsJson = """{"en": ["Goal 1"]}""",
            displayOrder = 1
        )
        every { catalogService.getCoursesForLanguage("es") } returns listOf(course)
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } returns "Advanced Spanish"

        mockMvc.perform(
            get("/api/v1/catalog/languages/es/courses?locale=en&userLevel=B2")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].startingLevel").value("B2"))

        verify { catalogService.getCoursesForLanguage("es") }
    }

    @Test
    @WithMockUser
    fun `should create tutor successfully`() {
        val tutorId = UUID.randomUUID()
        val createdTutor = TutorProfileEntity(
            id = tutorId,
            name = "NewTutor",
            emoji = "👨‍🏫",
            personaEnglish = "friendly teacher",
            domainEnglish = "general topics",
            descriptionEnglish = "A great tutor",
            personality = TutorPersonality.Encouraging,
            teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive,
            targetLanguageCode = "es",
            personaJson = """{"en": "friendly teacher"}""",
            domainJson = """{"en": "general topics"}""",
            descriptionJson = """{"en": "A great tutor"}""",
            culturalBackgroundJson = null,
            isActive = true,
            displayOrder = 0
        )

        every { catalogService.createTutor(any()) } returns createdTutor
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } answers { thirdArg() }

        mockMvc.perform(
            post("/api/v1/catalog/tutors?locale=en")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "NewTutor",
                        "emoji": "👨‍🏫",
                        "personaEnglish": "friendly teacher",
                        "domainEnglish": "general topics",
                        "descriptionEnglish": "A great tutor",
                        "personality": "Encouraging",
                        "teachingStyle": "Reactive",
                        "targetLanguageCode": "es"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(tutorId.toString()))
            .andExpect(jsonPath("$.name").value("NewTutor"))
            .andExpect(jsonPath("$.personality").value("Encouraging"))

        verify { catalogService.createTutor(any()) }
    }

    @Test
    @WithMockUser
    fun `should create tutor with cultural background`() {
        val tutorId = UUID.randomUUID()
        val createdTutor = TutorProfileEntity(
            id = tutorId,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "friendly teacher",
            domainEnglish = "general topics",
            descriptionEnglish = "A great tutor",
            personality = TutorPersonality.Encouraging,
            teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive,
            targetLanguageCode = "es",
            personaJson = """{"en": "friendly teacher"}""",
            domainJson = """{"en": "general topics"}""",
            descriptionJson = """{"en": "A great tutor"}""",
            culturalBackgroundJson = """{"en": "From Spain"}""",
            isActive = true,
            displayOrder = 0
        )

        every { catalogService.createTutor(any()) } returns createdTutor
        every { localizationService.getLocalizedText(any(), "en", any(), any()) } answers {
            if (secondArg<String>() == "en") "From Spain" else thirdArg()
        }

        mockMvc.perform(
            post("/api/v1/catalog/tutors?locale=en")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Maria",
                        "emoji": "👩‍🏫",
                        "personaEnglish": "friendly teacher",
                        "domainEnglish": "general topics",
                        "descriptionEnglish": "A great tutor",
                        "culturalBackground": "From Spain",
                        "personality": "Encouraging",
                        "teachingStyle": "Reactive",
                        "targetLanguageCode": "es"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.culturalBackground").value("From Spain"))

        verify { catalogService.createTutor(any()) }
    }

    @Test
    @WithMockUser
    fun `should get course detail with all fields`() {
        val course = CourseTemplateEntity(
            id = testCourseId,
            languageCode = "es",
            nameJson = """{"en": "Spanish Course", "de": "Spanischkurs"}""",
            shortDescriptionJson = """{"en": "Short desc", "de": "Kurze Beschreibung"}""",
            descriptionJson = """{"en": "Full description", "de": "Volle Beschreibung"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners", "de": "Anfänger"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"], "de": ["Ziel 1", "Ziel 2"]}""",
            displayOrder = 1,
            suggestedTutorIdsJson = """["${UUID.randomUUID()}"]"""
        )

        every { catalogService.getCourseById(testCourseId) } returns course
        every { localizationService.getLocalizedText(any(), "de", any(), any()) } answers {
            when (firstArg<String>()) {
                """{"en": "Spanish Course", "de": "Spanischkurs"}""" -> "Spanischkurs"
                """{"en": "Short desc", "de": "Kurze Beschreibung"}""" -> "Kurze Beschreibung"
                """{"en": "Full description", "de": "Volle Beschreibung"}""" -> "Volle Beschreibung"
                """{"en": "Beginners", "de": "Anfänger"}""" -> "Anfänger"
                else -> thirdArg()
            }
        }

        mockMvc.perform(
            get("/api/v1/catalog/courses/$testCourseId?locale=de")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Spanischkurs"))
            .andExpect(jsonPath("$.targetAudience").value("Anfänger"))
            .andExpect(jsonPath("$.learningGoals").isArray)
            .andExpect(jsonPath("$.learningGoals[0]").value("Ziel 1"))
            .andExpect(jsonPath("$.learningGoals[1]").value("Ziel 2"))

        verify { catalogService.getCourseById(testCourseId) }
    }

    @Test
    @WithMockUser
    fun `should get tutor detail with localization`() {
        val tutor = TutorProfileEntity(
            id = testTutorId,
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "friendly teacher",
            domainEnglish = "general topics",
            descriptionEnglish = "A great tutor",
            personality = TutorPersonality.Encouraging,
            teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Reactive,
            targetLanguageCode = "es",
            personaJson = """{"en": "friendly teacher", "es": "profesora amable"}""",
            domainJson = """{"en": "general topics", "es": "temas generales"}""",
            descriptionJson = """{"en": "A great tutor", "es": "Una gran profesora"}""",
            culturalBackgroundJson = """{"en": "From Spain", "es": "De España"}""",
            isActive = true,
            displayOrder = 0
        )

        every { catalogService.getTutorById(testTutorId) } returns tutor
        every { localizationService.getLocalizedText(any(), "es", any(), any()) } answers {
            when (firstArg<String>()) {
                """{"en": "friendly teacher", "es": "profesora amable"}""" -> "profesora amable"
                """{"en": "general topics", "es": "temas generales"}""" -> "temas generales"
                """{"en": "A great tutor", "es": "Una gran profesora"}""" -> "Una gran profesora"
                """{"en": "From Spain", "es": "De España"}""" -> "De España"
                else -> thirdArg()
            }
        }

        mockMvc.perform(
            get("/api/v1/catalog/tutors/$testTutorId?locale=es")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.persona").value("profesora amable"))
            .andExpect(jsonPath("$.domain").value("temas generales"))
            .andExpect(jsonPath("$.description").value("Una gran profesora"))
            .andExpect(jsonPath("$.culturalBackground").value("De España"))

        verify { catalogService.getTutorById(testTutorId) }
    }

    @Test
    @WithMockUser
    fun `should get languages with different locales`() {
        val spanish = LanguageMetadata(
            code = "es",
            nameJson = """{"en": "Spanish", "es": "Español"}""",
            flagEmoji = "🇪🇸",
            nativeName = "Español",
            difficulty = ch.obermuhlner.aitutor.core.model.catalog.Difficulty.Medium,
            descriptionJson = """{"en": "Romance language", "es": "Lengua romance"}"""
        )
        val french = LanguageMetadata(
            code = "fr",
            nameJson = """{"en": "French", "fr": "Français"}""",
            flagEmoji = "🇫🇷",
            nativeName = "Français",
            difficulty = ch.obermuhlner.aitutor.core.model.catalog.Difficulty.Medium,
            descriptionJson = """{"en": "Romance language", "fr": "Langue romane"}"""
        )

        every { catalogService.getAvailableLanguages() } returns listOf(spanish, french)
        every { catalogService.getCoursesForLanguage("es") } returns emptyList()
        every { catalogService.getCoursesForLanguage("fr") } returns emptyList()

        mockMvc.perform(
            get("/api/v1/catalog/languages?locale=fr")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].code").value("es"))
            .andExpect(jsonPath("$[0].nativeName").value("Español"))
            .andExpect(jsonPath("$[1].code").value("fr"))

        verify { catalogService.getAvailableLanguages() }
    }
}
