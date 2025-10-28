package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.image.service.ImageData
import ch.obermuhlner.aitutor.image.service.ImageService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional
import java.util.UUID

@WebMvcTest(controllers = [ImageController::class])
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class ImageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var imageService: ImageService

    @MockkBean
    private lateinit var tutorProfileRepository: ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `getImageData should return image when found`() {
        val concept = "apple"
        val imageBytes = ByteArray(100) { it.toByte() }
        val imageData = ImageData(
            data = imageBytes,
            format = "png",
            contentType = "image/png"
        )

        every { imageService.getImageByConcept(concept) } returns imageData

        mockMvc.perform(get("/api/v1/images/concept/$concept/data"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000"))
            .andExpect(content().bytes(imageBytes))

        verify(exactly = 1) { imageService.getImageByConcept(concept) }
    }

    @Test
    @WithMockUser
    fun `getImageData should return 404 when not found`() {
        val concept = "nonexistent"

        every { imageService.getImageByConcept(concept) } returns null

        mockMvc.perform(get("/api/v1/images/concept/$concept/data"))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { imageService.getImageByConcept(concept) }
    }

    @Test
    @WithMockUser
    fun `getTutorImageData should return image when tutor found`() {
        val tutorId = UUID.randomUUID()
        val imageBytes = ByteArray(100) { it.toByte() }
        val imageData = ImageData(
            data = imageBytes,
            format = "jpeg",
            contentType = "image/jpeg"
        )

        val tutor = TutorProfileEntity(
            id = tutorId,
            name = "Test Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "friendly teacher",
            domainEnglish = "grammar, conversation",
            descriptionEnglish = "A friendly teacher from Madrid",
            personaJson = """{"en":"friendly teacher"}""",
            domainJson = """{"en":"grammar, conversation"}""",
            descriptionJson = """{"en":"A friendly teacher from Madrid"}""",
            location = "Madrid, Spain",
            personality = ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality.Encouraging,
            teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Guided,
            voiceId = ch.obermuhlner.aitutor.core.model.catalog.TutorVoice.Warm,
            gender = TutorGender.Female,
            age = 30,
            targetLanguageCode = "es-ES",
            displayOrder = 0,
            isGlobal = true,
            createdByUserId = null
        )

        every { tutorProfileRepository.findById(tutorId) } returns Optional.of(tutor)
        every { imageService.getImageByPerson("ES", TutorGender.Female, 30, "Madrid, Spain friendly teacher}") } returns imageData

        mockMvc.perform(get("/api/v1/images/tutor/$tutorId/data"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000"))
            .andExpect(content().bytes(imageBytes))

        verify(exactly = 1) { tutorProfileRepository.findById(tutorId) }
        verify(exactly = 1) { imageService.getImageByPerson("ES", TutorGender.Female, 30, "Madrid, Spain friendly teacher}") }
    }

    @Test
    @WithMockUser
    fun `getTutorImageData should return 404 when tutor not found`() {
        val tutorId = UUID.randomUUID()

        every { tutorProfileRepository.findById(tutorId) } returns Optional.empty()

        mockMvc.perform(get("/api/v1/images/tutor/$tutorId/data"))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { tutorProfileRepository.findById(tutorId) }
        verify(exactly = 0) { imageService.getImageByPerson(any(), any(), any(), any()) }
    }

    @Test
    @WithMockUser
    fun `getTutorImageData should return 404 when image not found`() {
        val tutorId = UUID.randomUUID()

        val tutor = TutorProfileEntity(
            id = tutorId,
            name = "Test Tutor",
            emoji = "👨‍🏫",
            personaEnglish = "strict professor",
            domainEnglish = "grammar",
            descriptionEnglish = "A strict professor",
            personaJson = """{"en":"strict professor"}""",
            domainJson = """{"en":"grammar"}""",
            descriptionJson = """{"en":"A strict professor"}""",
            location = "Paris, France",
            personality = ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality.Strict,
            teachingStyle = ch.obermuhlner.aitutor.tutor.domain.TeachingStyle.Directive,
            voiceId = ch.obermuhlner.aitutor.core.model.catalog.TutorVoice.Professional,
            gender = TutorGender.Male,
            age = 40,
            targetLanguageCode = "fr-FR",
            displayOrder = 0,
            isGlobal = true,
            createdByUserId = null
        )

        every { tutorProfileRepository.findById(tutorId) } returns Optional.of(tutor)
        every { imageService.getImageByPerson("FR", TutorGender.Male, 40, "Paris, France strict professor}") } returns null

        mockMvc.perform(get("/api/v1/images/tutor/$tutorId/data"))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { tutorProfileRepository.findById(tutorId) }
        verify(exactly = 1) { imageService.getImageByPerson("FR", TutorGender.Male, 40, "Paris, France strict professor}") }
    }

    @Test
    @WithMockUser
    fun `getPersonImagePreview should return image with valid parameters`() {
        val imageBytes = ByteArray(100) { it.toByte() }
        val imageData = ImageData(
            data = imageBytes,
            format = "png",
            contentType = "image/png"
        )

        every { imageService.getImageByPerson("DE", TutorGender.Female, 25, "Berlin teacher") } returns imageData

        mockMvc.perform(
            get("/api/v1/images/person/preview")
                .param("languageCode", "de-DE")
                .param("gender", "Female")
                .param("age", "25")
                .param("location", "Berlin")
                .param("persona", "teacher")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control", "public, max-age=300"))
            .andExpect(content().bytes(imageBytes))

        verify(exactly = 1) { imageService.getImageByPerson("DE", TutorGender.Female, 25, "Berlin teacher") }
    }

    @Test
    @WithMockUser
    fun `getPersonImagePreview should handle invalid gender gracefully`() {
        val imageBytes = ByteArray(100) { it.toByte() }
        val imageData = ImageData(
            data = imageBytes,
            format = "png",
            contentType = "image/png"
        )

        every { imageService.getImageByPerson("ES", TutorGender.Neutral, 30, "Madrid friendly") } returns imageData

        mockMvc.perform(
            get("/api/v1/images/person/preview")
                .param("languageCode", "es-ES")
                .param("gender", "InvalidGender")
                .param("age", "30")
                .param("location", "Madrid")
                .param("persona", "friendly")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))

        verify(exactly = 1) { imageService.getImageByPerson("ES", TutorGender.Neutral, 30, "Madrid friendly") }
    }

    @Test
    @WithMockUser
    fun `getPersonImagePreview should work with optional parameters omitted`() {
        val imageBytes = ByteArray(100) { it.toByte() }
        val imageData = ImageData(
            data = imageBytes,
            format = "png",
            contentType = "image/png"
        )

        every { imageService.getImageByPerson("JP", TutorGender.Male, 35, " ") } returns imageData

        mockMvc.perform(
            get("/api/v1/images/person/preview")
                .param("languageCode", "ja-JP")
                .param("gender", "Male")
                .param("age", "35")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))

        verify(exactly = 1) { imageService.getImageByPerson("JP", TutorGender.Male, 35, " ") }
    }

    @Test
    @WithMockUser
    fun `getPersonImagePreview should return 404 when image not found`() {
        every { imageService.getImageByPerson("IT", TutorGender.Female, 28, "Rome artist") } returns null

        mockMvc.perform(
            get("/api/v1/images/person/preview")
                .param("languageCode", "it-IT")
                .param("gender", "Female")
                .param("age", "28")
                .param("location", "Rome")
                .param("persona", "artist")
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { imageService.getImageByPerson("IT", TutorGender.Female, 28, "Rome artist") }
    }
}
