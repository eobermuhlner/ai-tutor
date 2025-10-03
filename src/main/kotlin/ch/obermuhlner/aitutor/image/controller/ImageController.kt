package ch.obermuhlner.aitutor.image.controller

import ch.obermuhlner.aitutor.image.service.ImageService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.imageio.ImageIO

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
    private val imageService: ImageService
) {

    @GetMapping("/concept/{concept}/data")
    fun getImageData(@PathVariable concept: String): ResponseEntity<ByteArray> {
        val image = imageService.getImageByConcept(concept)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/${image.format}"))
            .header("Cache-Control", "public, max-age=31536000")
            .body(image.data)
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadImage(
        @RequestParam concept: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<Void> {
        val image = ImageIO.read(file.inputStream)
        imageService.createImage(
            concept = concept,
            data = file.bytes,
            format = file.contentType?.substringAfter("/") ?: "png",
            width = image.width,
            height = image.height
        )
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/concept/{concept}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteImage(@PathVariable concept: String): ResponseEntity<Void> {
        imageService.deleteImage(concept)
        return ResponseEntity.ok().build()
    }
}
