package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.image.service.ImageService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    private val imageService: ImageService,
    private val tutorProfileRepository: TutorProfileRepository
) {

    @GetMapping("/concept/{concept}/data")
    fun getImageData(@PathVariable concept: String): ResponseEntity<ByteArray> {
        val imageData = imageService.getImageByConcept(concept)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageData.contentType))
            .header("Cache-Control", "public, max-age=31536000")
            .body(imageData.data)
    }

    @GetMapping("/tutor/{tutorId}/data")
    fun getTutorImageData(@PathVariable tutorId: UUID): ResponseEntity<ByteArray> {
        val tutor = tutorProfileRepository.findById(tutorId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // If no gender is set, cannot fetch image
        val gender = tutor.gender ?: return ResponseEntity.notFound().build()

        // Extract country code from language code (e.g., "es-ES" -> "ES")
        val countryCode = tutor.targetLanguageCode.substringAfterLast("-").uppercase()

        // Combine text from multiple fields for image search
        val combinedText = "${tutor.personaEnglish} ${tutor.domainEnglish} ${tutor.culturalBackgroundJson ?: ""}"

        val imageData = imageService.getImageByPerson(
            countryCode = countryCode,
            gender = gender,
            age = tutor.age,
            text = combinedText
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageData.contentType))
            .header("Cache-Control", "public, max-age=31536000")
            .body(imageData.data)
    }
}
