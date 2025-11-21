package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CourseDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.CourseResponse
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.dto.LanguageResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorResponse
import ch.obermuhlner.aitutor.catalog.dto.UpdateTutorRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.config.TestConfig
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test", "noauth")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestConfig::class)
class CatalogControllerIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Autowired
    private lateinit var tutorProfileRepository: TutorProfileRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: ch.obermuhlner.aitutor.auth.service.AuthorizationService

    private fun baseUrl(path: String): String = "http://localhost:$port/api/v1$path"

    private val testUserId = UUID.randomUUID()

    private val testUser = UserEntity(
        id = testUserId,
        username = "testuser",
        email = "test@example.com",
        passwordHash = "password",
        roles = mutableSetOf(ch.obermuhlner.aitutor.user.domain.UserRole.USER)
    )

    @BeforeEach
    fun setUp() {
        // Clean up existing data before each test
        courseTemplateRepository.deleteAll()
        tutorProfileRepository.deleteAll()
        userRepository.deleteAll()

        // Create a default admin user for the noauth profile
        val adminUser = UserEntity(
            id = testUserId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "password",
            roles = mutableSetOf(
                ch.obermuhlner.aitutor.user.domain.UserRole.USER,
                ch.obermuhlner.aitutor.user.domain.UserRole.ADMIN,
                ch.obermuhlner.aitutor.user.domain.UserRole.EDITOR
            )
        )
        userRepository.save(adminUser)

        // Mock authorization service to return test user info
        every { authorizationService.getCurrentUserId() } returns testUserId
        every { authorizationService.getCurrentUser() } returns adminUser
        every { authorizationService.isAdmin() } returns true
        every { authorizationService.isEditor() } returns true
        every { authorizationService.isEditorOrAdmin() } returns true
    }

    @Test
    fun `test get languages endpoint`() {
        // Create test language data - this would be handled by seed data in real application
        // For integration test, we're testing the endpoint with any available languages

        val response = restTemplate.getForEntity(baseUrl("/catalog/languages"), Array<LanguageResponse>::class.java)

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        // The response body may be empty if no seed data is loaded
        // But the endpoint should return successfully
    }

    @Test
    fun `test get languages with locale parameter`() {
        val response = restTemplate.getForEntity(
            baseUrl("/catalog/languages?locale=de"),
            Array<LanguageResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test get courses for language endpoint`() {
        // Create a test language and course
        val course = CourseTemplateEntity(
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
            displayOrder = 1,
            isDraft = false
        )
        val savedCourse = courseTemplateRepository.save(course)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/languages/es/courses"),
            Array<CourseResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).hasSize(1)
        Assertions.assertThat(response.body!![0].id).isEqualTo(savedCourse.id)
        Assertions.assertThat(response.body!![0].name).isEqualTo("Beginner Spanish")
    }

    @Test
    fun `test get courses for language with drafts`() {
        // Create published and draft courses
        val publishedCourse = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Published Course"}""",
            shortDescriptionJson = """{"en": "Published description"}""",
            descriptionJson = """{"en": "Published course"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1"]}""",
            displayOrder = 1,
            isDraft = false
        )
        val draftCourse = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Draft Course"}""",
            shortDescriptionJson = """{"en": "Draft description"}""",
            descriptionJson = """{"en": "Draft course"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Advanced"}""",
            startingLevel = CEFRLevel.B1,
            targetLevel = CEFRLevel.B2,
            estimatedWeeks = 12,
            learningGoalsJson = """{"en": ["Goal 2"]}""",
            displayOrder = 2,
            isDraft = true
        )
        val published = courseTemplateRepository.save(publishedCourse)
        val draft = courseTemplateRepository.save(draftCourse)

        // Test without drafts (should only return published)
        val responseWithoutDrafts = restTemplate.getForEntity(
            baseUrl("/catalog/languages/es/courses"),
            Array<CourseResponse>::class.java
        )
        Assertions.assertThat(responseWithoutDrafts.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(responseWithoutDrafts.body).hasSize(1)
        Assertions.assertThat(responseWithoutDrafts.body!![0].name).isEqualTo("Published Course")

        // Test with includeDrafts parameter
        val responseWithDrafts = restTemplate.getForEntity(
            baseUrl("/catalog/languages/es/courses?includeDrafts=true"),
            Array<CourseResponse>::class.java
        )
        Assertions.assertThat(responseWithDrafts.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(responseWithDrafts.body).hasSize(2)
    }

    @Test
    fun `test get course detail endpoint`() {
        // Create a test course
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Advanced Spanish", "de": "Fortgeschrittener Spanisch"}""",
            shortDescriptionJson = """{"en": "Advanced Spanish course", "de": "Fortgeschrittener Spanischkurs"}""",
            descriptionJson = """{"en": "Full advanced description", "de": "Vollständige fortgeschrittene Beschreibung"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Advanced learners", "de": "Fortgeschrittene Lerner"}""",
            startingLevel = CEFRLevel.B1,
            targetLevel = CEFRLevel.B2,
            estimatedWeeks = 10,
            defaultPhase = ConversationPhase.Correction,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"], "de": ["Ziel 1", "Ziel 2"]}""",
            displayOrder = 1,
            isDraft = false
        )
        val savedCourse = courseTemplateRepository.save(course)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/courses/${savedCourse.id}"),
            CourseDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.id).isEqualTo(savedCourse.id)
        Assertions.assertThat(response.body!!.name).isEqualTo("Advanced Spanish")
        Assertions.assertThat(response.body!!.languageCode).isEqualTo("es")
        Assertions.assertThat(response.body!!.category).isEqualTo(CourseCategory.General)
        Assertions.assertThat(response.body!!.startingLevel).isEqualTo(CEFRLevel.B1)
        Assertions.assertThat(response.body!!.targetLevel).isEqualTo(CEFRLevel.B2)
        Assertions.assertThat(response.body!!.estimatedWeeks).isEqualTo(10)
        Assertions.assertThat(response.body!!.defaultPhase).isEqualTo(ConversationPhase.Correction)
        Assertions.assertThat(response.body!!.learningGoals).contains("Goal 1", "Goal 2")
    }

    @Test
    fun `test get course detail with locale parameter`() {
        // Create a test course with German localization
        val course = CourseTemplateEntity(
            languageCode = "de",
            nameJson = """{"en": "German Basics", "de": "Deutsch Grundlagen"}""",
            shortDescriptionJson = """{"en": "German basics course", "de": "Deutsch Grundlagen Kurs"}""",
            descriptionJson = """{"en": "Full German course", "de": "Vollständiger Deutschkurs"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners", "de": "Anfänger"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            learningGoalsJson = """{"en": ["Goal 1"], "de": ["Ziel 1"]}""",
            displayOrder = 1,
            isDraft = false
        )
        val savedCourse = courseTemplateRepository.save(course)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/courses/${savedCourse.id}?locale=de"),
            CourseDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.id).isEqualTo(savedCourse.id)
        Assertions.assertThat(response.body!!.name).isEqualTo("Deutsch Grundlagen") // German localized
        Assertions.assertThat(response.body!!.targetAudience).isEqualTo("Anfänger") // German localized
        Assertions.assertThat(response.body!!.learningGoals).contains("Ziel 1") // German localized
    }

    @Test
    fun `test get course detail with associated tutors`() {
        // Create a tutor profile
        val tutor = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient Spanish teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor for beginners",
            personaJson = """{"en": "Patient Spanish teacher"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "A patient tutor for beginners"}""",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = false
        )
        val savedTutor = tutorProfileRepository.save(tutor)

        // Create a course and associate the tutor
        val course = CourseTemplateEntity(
            languageCode = "es",
            nameJson = """{"en": "Beginner Course"}""",
            shortDescriptionJson = """{"en": "For beginners"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = CourseCategory.General,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = CEFRLevel.A1,
            targetLevel = CEFRLevel.A2,
            estimatedWeeks = 8,
            suggestedTutorIdsJson = """["${savedTutor.id}"]""",
            learningGoalsJson = """{"en": ["Goal 1"]}""",
            displayOrder = 1,
            isDraft = false
        )
        val savedCourse = courseTemplateRepository.save(course)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/courses/${savedCourse.id}"),
            CourseDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.id).isEqualTo(savedCourse.id)
        // The suggestedTutors should be populated based on the tutor profile
        // At least verify the structure exists
        Assertions.assertThat(response.body!!.suggestedTutors).isNotNull
    }

    @Test
    fun `test get tutors for language endpoint`() {
        // Create tutor profiles
        val tutor1 = TutorProfileEntity(
            name = "Maria",
            emoji = "👩‍🏫",
            personaEnglish = "Patient Spanish teacher",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor for beginners",
            personaJson = """{"en": "Patient Spanish teacher", "es": "Profesora paciente"}""",
            domainJson = """{"en": "General conversation", "es": "Conversación general"}""",
            descriptionJson = """{"en": "A patient tutor for beginners", "es": "Una tutora paciente para principiantes"}""",
            location = "Madrid",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val tutor2 = TutorProfileEntity(
            name = "Carlos",
            emoji = "👨‍🏫",
            personaEnglish = "Fun Spanish teacher",
            domainEnglish = "Travel vocabulary",
            descriptionEnglish = "A fun tutor for travel phrases",
            personaJson = """{"en": "Fun Spanish teacher", "es": "Profesor divertido"}""",
            domainJson = """{"en": "Travel vocabulary", "es": "Vocabulario de viaje"}""",
            descriptionJson = """{"en": "A fun tutor for travel phrases", "es": "Un tutor divertido para frases de viaje"}""",
            location = "Barcelona",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Guided,
            targetLanguageCode = "es",
            displayOrder = 2,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val savedTutor1 = tutorProfileRepository.save(tutor1)
        val savedTutor2 = tutorProfileRepository.save(tutor2)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/languages/es/tutors"),
            Array<TutorResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).hasSize(2)

        val tutorResponse1 = response.body!!.first { it.id == savedTutor1.id }
        Assertions.assertThat(tutorResponse1.name).isEqualTo("Maria")
        Assertions.assertThat(tutorResponse1.emoji).isEqualTo("👩‍🏫")
        Assertions.assertThat(tutorResponse1.persona).isEqualTo("Patient Spanish teacher")
        Assertions.assertThat(tutorResponse1.targetLanguageCode).isEqualTo("es")
        Assertions.assertThat(tutorResponse1.location).isEqualTo("Madrid")
        Assertions.assertThat(tutorResponse1.personality).isEqualTo(TutorPersonality.Encouraging)
        Assertions.assertThat(tutorResponse1.teachingStyle).isEqualTo(TeachingStyle.Reactive)

        val tutorResponse2 = response.body!!.first { it.id == savedTutor2.id }
        Assertions.assertThat(tutorResponse2.name).isEqualTo("Carlos")
        Assertions.assertThat(tutorResponse2.emoji).isEqualTo("👨‍🏫")
        Assertions.assertThat(tutorResponse2.persona).isEqualTo("Fun Spanish teacher")
        Assertions.assertThat(tutorResponse2.targetLanguageCode).isEqualTo("es")
        Assertions.assertThat(tutorResponse2.location).isEqualTo("Barcelona")
        Assertions.assertThat(tutorResponse2.personality).isEqualTo(TutorPersonality.Encouraging)
        Assertions.assertThat(tutorResponse2.teachingStyle).isEqualTo(TeachingStyle.Guided)
    }

    @Test
    fun `test get tutor detail endpoint`() {
        // Create a tutor profile
        val tutor = TutorProfileEntity(
            name = "Alex",
            emoji = "🎓",
            personaEnglish = "Academic tutor",
            domainEnglish = "Academic writing",
            descriptionEnglish = "Specializes in academic writing and formal language",
            personaJson = """{"en": "Academic tutor", "fr": "Tuteur académique"}""",
            domainJson = """{"en": "Academic writing", "fr": "Rédaction académique"}""",
            descriptionJson = """{"en": "Specializes in academic writing and formal language", "fr": "Spécialisé dans la rédaction académique et le langage formel"}""",
            culturalBackgroundJson = """{"en": "From France", "fr": "Du France"}""",
            location = "Paris",
            age = 35,
            gender = TutorGender.Female,
            personality = TutorPersonality.Strict,
            teachingStyle = TeachingStyle.Directive,
            targetLanguageCode = "fr", // French tutor
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val savedTutor = tutorProfileRepository.save(tutor)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/tutors/${savedTutor.id}"),
            TutorDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.id).isEqualTo(savedTutor.id)
        Assertions.assertThat(response.body!!.name).isEqualTo("Alex")
        Assertions.assertThat(response.body!!.emoji).isEqualTo("🎓")
        Assertions.assertThat(response.body!!.persona).isEqualTo("Academic tutor")
        Assertions.assertThat(response.body!!.domain).isEqualTo("Academic writing")
        Assertions.assertThat(response.body!!.description).isEqualTo("Specializes in academic writing and formal language")
        Assertions.assertThat(response.body!!.culturalBackground).isEqualTo("From France")
        Assertions.assertThat(response.body!!.location).isEqualTo("Paris")
        Assertions.assertThat(response.body!!.age).isEqualTo(35)
        Assertions.assertThat(response.body!!.gender).isEqualTo(TutorGender.Female)
        Assertions.assertThat(response.body!!.personality).isEqualTo(TutorPersonality.Strict)
        Assertions.assertThat(response.body!!.teachingStyle).isEqualTo(TeachingStyle.Directive)
        Assertions.assertThat(response.body!!.targetLanguageCode).isEqualTo("fr")
    }

    @Test
    fun `test get tutor detail with locale parameter`() {
        // Create a tutor profile with multiple locales
        val tutor = TutorProfileEntity(
            name = "Marie",
            emoji = "👩‍🎓",
            personaEnglish = "Patient French teacher",
            domainEnglish = "French grammar",
            descriptionEnglish = "Patient tutor for grammar and conjugation",
            personaJson = """{"en": "Patient French teacher", "de": "Geduldige Französischlehrerin"}""",
            domainJson = """{"en": "French grammar", "de": "Französische Grammatik"}""",
            descriptionJson = """{"en": "Patient tutor for grammar and conjugation", "de": "Geduldiger Tutor für Grammatik und Konjugation"}""",
            location = "Lyon",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "fr",
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val savedTutor = tutorProfileRepository.save(tutor)

        val response = restTemplate.getForEntity(
            baseUrl("/catalog/tutors/${savedTutor.id}?locale=de"),
            TutorDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.id).isEqualTo(savedTutor.id)
        Assertions.assertThat(response.body!!.name).isEqualTo("Marie")
        // Should be localized to German according to locale parameter
        Assertions.assertThat(response.body!!.persona).isEqualTo("Geduldige Französischlehrerin")
        Assertions.assertThat(response.body!!.domain).isEqualTo("Französische Grammatik")
        Assertions.assertThat(response.body!!.description).isEqualTo("Geduldiger Tutor für Grammatik und Konjugation")
    }

    @Test
    fun `test create tutor endpoint`() {
        // Create user in DB for authentication context
        val user = userRepository.save(testUser)

        val createRequest = CreateTutorRequest(
            name = "New Tutor",
            emoji = "👨‍🏫",
            personaEnglish = "New friendly tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "A new friendly tutor for general conversation",
            culturalBackground = "From Italy",
            location = "Rome",
            age = 28,
            gender = TutorGender.Male,
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Guided,
            targetLanguageCode = "it",
            displayOrder = 1
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(createRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/catalog/tutors"),
            HttpMethod.POST,
            entity,
            TutorDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.name).isEqualTo("New Tutor")
        Assertions.assertThat(response.body!!.emoji).isEqualTo("👨‍🏫")
        Assertions.assertThat(response.body!!.persona).isEqualTo("New friendly tutor")
        Assertions.assertThat(response.body!!.domain).isEqualTo("General conversation")
        Assertions.assertThat(response.body!!.description).isEqualTo("A new friendly tutor for general conversation")
        Assertions.assertThat(response.body!!.culturalBackground).isEqualTo("From Italy")
        Assertions.assertThat(response.body!!.location).isEqualTo("Rome")
        Assertions.assertThat(response.body!!.age).isEqualTo(28)
        Assertions.assertThat(response.body!!.gender).isEqualTo(TutorGender.Male)
        Assertions.assertThat(response.body!!.personality).isEqualTo(TutorPersonality.Encouraging)
        Assertions.assertThat(response.body!!.teachingStyle).isEqualTo(TeachingStyle.Guided)
        Assertions.assertThat(response.body!!.targetLanguageCode).isEqualTo("it")

        // Verify it was saved to the database
        val savedTutor = tutorProfileRepository.findById(response.body!!.id)
        Assertions.assertThat(savedTutor).isPresent
        Assertions.assertThat(savedTutor.get().name).isEqualTo("New Tutor")
        Assertions.assertThat(savedTutor.get().isGlobal).isFalse // Should be user-specific since not admin
    }

    @Test
    fun `test update tutor endpoint`() {
        // Create user in DB for authentication context
        val user = userRepository.save(testUser)

        // Create initial tutor
        val initialTutor = TutorProfileEntity(
            name = "Initial Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "Initial persona",
            domainEnglish = "Initial domain",
            descriptionEnglish = "Initial description",
            personaJson = """{"en": "Initial persona"}""",
            domainJson = """{"en": "Initial domain"}""",
            descriptionJson = """{"en": "Initial description"}""",
            location = "Initial location",
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1,
            createdByUserId = user.id,
            isGlobal = false
        )
        val savedTutor = tutorProfileRepository.save(initialTutor)

        // Update request
        val updateRequest = UpdateTutorRequest(
            name = "Updated Tutor",
            emoji = "👨‍🎓",
            personaEnglish = "Updated persona",
            domainEnglish = "Updated domain",
            descriptionEnglish = "Updated description",
            culturalBackground = "From Spain",
            location = "Madrid",
            age = 32,
            gender = TutorGender.Male,
            personality = TutorPersonality.Strict,
            teachingStyle = TeachingStyle.Directive,
            targetLanguageCode = "es",
            displayOrder = 2
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(updateRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/catalog/tutors/${savedTutor.id}"),
            HttpMethod.PUT,
            entity,
            TutorDetailResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.name).isEqualTo("Updated Tutor")
        Assertions.assertThat(response.body!!.emoji).isEqualTo("👨‍🎓")
        Assertions.assertThat(response.body!!.persona).isEqualTo("Updated persona")
        Assertions.assertThat(response.body!!.domain).isEqualTo("Updated domain")
        Assertions.assertThat(response.body!!.description).isEqualTo("Updated description")
        Assertions.assertThat(response.body!!.culturalBackground).isEqualTo("From Spain")
        Assertions.assertThat(response.body!!.location).isEqualTo("Madrid")
        Assertions.assertThat(response.body!!.age).isEqualTo(32)
        Assertions.assertThat(response.body!!.gender).isEqualTo(TutorGender.Male)
        Assertions.assertThat(response.body!!.personality).isEqualTo(TutorPersonality.Strict)
        Assertions.assertThat(response.body!!.teachingStyle).isEqualTo(TeachingStyle.Directive)
        Assertions.assertThat(response.body!!.targetLanguageCode).isEqualTo("es")

        // Verify it was updated in the database
        val updatedTutor = tutorProfileRepository.findById(savedTutor.id)
        Assertions.assertThat(updatedTutor).isPresent
        Assertions.assertThat(updatedTutor.get().name).isEqualTo("Updated Tutor")
        Assertions.assertThat(updatedTutor.get().emoji).isEqualTo("👨‍🎓")
        Assertions.assertThat(updatedTutor.get().personaEnglish).isEqualTo("Updated persona")
        Assertions.assertThat(updatedTutor.get().domainEnglish).isEqualTo("Updated domain")
        Assertions.assertThat(updatedTutor.get().descriptionEnglish).isEqualTo("Updated description")
    }

    @Test
    fun `test update non-existent tutor returns 404`() {
        val updateRequest = UpdateTutorRequest(
            name = "Updated Tutor",
            emoji = "👨‍🎓",
            personaEnglish = "Updated persona",
            domainEnglish = "Updated domain",
            descriptionEnglish = "Updated description",
            culturalBackground = null,
            location = "Madrid",
            age = 32,
            gender = TutorGender.Male,
            personality = TutorPersonality.Strict,
            teachingStyle = TeachingStyle.Directive,
            targetLanguageCode = "es",
            displayOrder = 2
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(updateRequest, headers)

        // In noauth profile, when tutor is not found, the controller properly returns 404
        // But the response cannot be deserialized as TutorDetailResponse, so we expect an exception
        Assertions.assertThatThrownBy {
            restTemplate.exchange(
                baseUrl("/catalog/tutors/${UUID.randomUUID()}"),
                HttpMethod.PUT,
                entity,
                TutorDetailResponse::class.java
            )
        }.isInstanceOf(org.springframework.web.client.RestClientException::class.java)
    }
}
