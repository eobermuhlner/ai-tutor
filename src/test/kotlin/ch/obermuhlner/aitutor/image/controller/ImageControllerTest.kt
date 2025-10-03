package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.image.domain.ImageEntity
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [ImageController::class])
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class ImageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var imageService: ImageService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `getImageData should return image when found`() {
        val concept = "apple"
        val imageData = ByteArray(100) { it.toByte() }
        val entity = ImageEntity(
            concept = concept,
            data = imageData,
            format = "png",
            widthPx = 100,
            heightPx = 100
        )

        every { imageService.getImageByConcept(concept) } returns entity

        mockMvc.perform(get("/api/v1/images/concept/$concept/data"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000"))
            .andExpect(content().bytes(imageData))

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
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteImage should delete image as admin`() {
        val concept = "water"

        every { imageService.deleteImage(concept) } returns Unit

        mockMvc.perform(delete("/api/v1/images/concept/$concept")
            .with(csrf()))
            .andExpect(status().isOk)

        verify(exactly = 1) { imageService.deleteImage(concept) }
    }

    @Test
    fun `deleteImage should return 403 when not authenticated`() {
        val concept = "water"

        mockMvc.perform(delete("/api/v1/images/concept/$concept")
            .with(csrf()))
            .andExpect(status().isForbidden)

        verify(exactly = 0) { imageService.deleteImage(any()) }
    }
}
