package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.service.UserLanguageService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [UserLanguageController::class])
@AutoConfigureJsonTesters
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class UserLanguageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var userLanguageService: UserLanguageService

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    private val testUserId = UUID.randomUUID()
    private val testLanguageId = UUID.randomUUID()

    private fun createTestLanguageProficiency(): UserLanguageProficiencyEntity {
        return UserLanguageProficiencyEntity(
            id = testLanguageId,
            userId = testUserId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
    }

    @Test
    @WithMockUser
    fun `should get user languages`() {
        val language = createTestLanguageProficiency()
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every { userLanguageService.getUserLanguages(testUserId) } returns listOf(language)

        mockMvc.perform(
            get("/api/v1/users/$testUserId/languages")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testLanguageId.toString()))
            .andExpect(jsonPath("$[0].languageCode").value("es"))
            .andExpect(jsonPath("$[0].proficiencyType").value("Learning"))
            .andExpect(jsonPath("$[0].cefrLevel").value("A1"))

        verify { userLanguageService.getUserLanguages(testUserId) }
    }

    @Test
    @WithMockUser
    fun `should get user languages filtered by type`() {
        val language = createTestLanguageProficiency()
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every { userLanguageService.getLearningLanguages(testUserId) } returns listOf(language)

        mockMvc.perform(
            get("/api/v1/users/$testUserId/languages?type=Learning")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].proficiencyType").value("Learning"))

        verify { userLanguageService.getLearningLanguages(testUserId) }
    }

    @Test
    @WithMockUser
    fun `should add language to user profile`() {
        val language = createTestLanguageProficiency()
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every {
            userLanguageService.addLanguage(
                userId = testUserId,
                languageCode = "es",
                type = LanguageProficiencyType.Learning,
                cefrLevel = CEFRLevel.A1,
                isNative = false
            )
        } returns language

        mockMvc.perform(
            post("/api/v1/users/$testUserId/languages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "languageCode": "es",
                        "type": "Learning",
                        "cefrLevel": "A1",
                        "isNative": false
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.languageCode").value("es"))

        verify {
            userLanguageService.addLanguage(
                userId = testUserId,
                languageCode = "es",
                type = LanguageProficiencyType.Learning,
                cefrLevel = CEFRLevel.A1,
                isNative = false
            )
        }
    }

    @Test
    @WithMockUser
    fun `should update language level`() {
        val language = UserLanguageProficiencyEntity(
            id = testLanguageId,
            userId = testUserId,
            languageCode = "es",
            proficiencyType = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A2,
            isNative = false,
            isPrimary = true,
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every { userLanguageService.updateLanguage(testUserId, "es", CEFRLevel.A2) } returns language

        mockMvc.perform(
            patch("/api/v1/users/$testUserId/languages/es")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "cefrLevel": "A2"
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cefrLevel").value("A2"))

        verify { userLanguageService.updateLanguage(testUserId, "es", CEFRLevel.A2) }
    }

    @Test
    @WithMockUser
    fun `should set primary target language`() {
        val language = createTestLanguageProficiency()
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every { userLanguageService.setPrimaryLanguage(testUserId, "es") } returns Unit
        every { userLanguageService.getUserLanguages(testUserId) } returns listOf(language)

        mockMvc.perform(
            post("/api/v1/users/$testUserId/languages/es/primary")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isPrimary").value(true))

        verify { userLanguageService.setPrimaryLanguage(testUserId, "es") }
    }

    @Test
    @WithMockUser
    fun `should remove language from user profile`() {
        every { authorizationService.requireAccessToUser(testUserId) } returns Unit
        every { userLanguageService.removeLanguage(testUserId, "es") } returns Unit

        mockMvc.perform(
            delete("/api/v1/users/$testUserId/languages/es")
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify { userLanguageService.removeLanguage(testUserId, "es") }
    }
}
