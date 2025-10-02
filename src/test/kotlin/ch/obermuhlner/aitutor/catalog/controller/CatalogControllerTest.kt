package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.domain.*
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.language.service.LocalizationService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

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
}
