package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.image.service.ImageService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    private val imageService: ImageService
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
}
