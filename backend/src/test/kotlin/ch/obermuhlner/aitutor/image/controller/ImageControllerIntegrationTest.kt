package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class ImageControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getImageData endpoint - concept image`() {
        // Test getting image data for a concept
        // This will likely return not found since we don't have a real image store
        val response = restTemplate.getForEntity(
            baseUrl("/images/concept/test-concept/data"),
            ByteArray::class.java
        )

        // Expect not found since there might not be such an image
        Assertions.assertThat(response.statusCode).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getTutorImageData endpoint - not found when tutor doesn't exist`() {
        // Test getting tutor image data for a non-existent tutor
        val nonExistentId = UUID.randomUUID()
        val response = restTemplate.getForEntity(
            baseUrl("/images/tutor/$nonExistentId/data"),
            ByteArray::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getTutorImageData endpoint - with existing tutor`() {
        // Create a tutor first
        val tutor = TutorProfileEntity(
            name = "Test Tutor",
            emoji = "👩‍🏫",
            personaEnglish = "Patient Spanish tutor",
            domainEnglish = "General conversation",
            descriptionEnglish = "A patient tutor for beginners",
            personaJson = """{"en": "Patient Spanish tutor"}""",
            domainJson = """{"en": "General conversation"}""",
            descriptionJson = """{"en": "A patient tutor for beginners"}""",
            location = "Madrid",
            age = 30,
            gender = TutorGender.Female,
            personality = TutorPersonality.Encouraging,
            teachingStyle = TeachingStyle.Reactive,
            targetLanguageCode = "es",
            displayOrder = 1,
            createdByUserId = testUserId,
            isGlobal = true
        )
        val savedTutor = tutorProfileRepository.save(tutor)

        // Test getting tutor image data for the created tutor
        val response = restTemplate.getForEntity(
            baseUrl("/images/tutor/${savedTutor.id}/data"),
            ByteArray::class.java
        )

        // Could return OK if image service works or NOT_FOUND if image doesn't exist in store
        Assertions.assertThat(response.statusCode).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test getPersonImagePreview endpoint`() {
        // Test getting person image preview
        val response = restTemplate.getForEntity(
            baseUrl("/images/person/preview?languageCode=es&gender=Female&age=30&location=Madrid&persona=Patient Spanish tutor"),
            ByteArray::class.java
        )

        // Could return OK if image service works or NOT_FOUND if image doesn't exist in store
        Assertions.assertThat(response.statusCode).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND)
    }
}