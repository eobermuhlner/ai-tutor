package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
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

        val gender = tutor.gender ?: TutorGender.Neutral

        val countryCode = tutor.targetLanguageCode.substringAfterLast("-").uppercase()

        val combinedText = "${tutor.location} ${tutor.personaEnglish}}"

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

    @GetMapping("/person/preview")
    fun getPersonImagePreview(
        @org.springframework.web.bind.annotation.RequestParam languageCode: String,
        @org.springframework.web.bind.annotation.RequestParam gender: String,
        @org.springframework.web.bind.annotation.RequestParam age: Int,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "") location: String,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "") persona: String
    ): ResponseEntity<ByteArray> {
        val genderEnum = try {
            TutorGender.valueOf(gender)
        } catch (e: IllegalArgumentException) {
            TutorGender.Neutral
        }

        val countryCode = languageCode.substringAfterLast("-").uppercase()
        val combinedText = "$location $persona"

        val imageData = imageService.getImageByPerson(
            countryCode = countryCode,
            gender = genderEnum,
            age = age,
            text = combinedText
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(imageData.contentType))
            .header("Cache-Control", "public, max-age=300") // 5 minute cache for preview
            .body(imageData.data)
    }
}
