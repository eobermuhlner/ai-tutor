package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.config.ImageStoreProperties
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import ch.obermuhlner.aitutor.image.dto.ImageUploadResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder.*

@Service
@EnableConfigurationProperties(ImageStoreProperties::class)
class ImageStoreClient(
    private val properties: ImageStoreProperties
) {
    private val logger = LoggerFactory.getLogger(ImageStoreClient::class.java)

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    fun uploadImage(
        concept: String,
        data: ByteArray,
        filename: String,
        contentType: String
    ): ImageUploadResponse {
        logger.info("Uploading image to imagestore: concept=$concept, filename=$filename")

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", object : ByteArrayResource(data) {
            override fun getFilename(): String = filename
        }).contentType(MediaType.parseMediaType(contentType))
        bodyBuilder.part("tags", concept)

        val response = restClient.post()
            .uri("/api/images?tags={concept}", concept)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(bodyBuilder.build())
            .retrieve()
            .body<ImageUploadResponse>()

        logger.info("Image uploaded successfully: imagestore id=${response?.id}")
        return response ?: throw IllegalStateException("Failed to upload image")
    }

    fun getImageData(imagestoreId: Long): ByteArray {
        logger.debug("Fetching image data from imagestore: id=$imagestoreId")

        return restClient.get()
            .uri("/api/images/{id}", imagestoreId)
            .retrieve()
            .body<ByteArray>() ?: throw IllegalStateException("Failed to fetch image")
    }

    fun getImageMetadata(imagestoreId: Long): ImageMetadataResponse {
        logger.debug("Fetching image metadata from imagestore: id=$imagestoreId")

        return restClient.get()
            .uri("/api/images/{id}/metadata", imagestoreId)
            .retrieve()
            .body<ImageMetadataResponse>() ?: throw IllegalStateException("Failed to fetch metadata")
    }

    fun deleteImage(imagestoreId: Long) {
        logger.info("Deleting image from imagestore: id=$imagestoreId")

        restClient.delete()
            .uri("/api/images/{id}", imagestoreId)
            .retrieve()
            .toBodilessEntity()
    }

    fun searchImagesByTags(
        required: List<String>,
        optional: List<String> = emptyList(),
        forbidden: List<String> = emptyList(),
    ): List<ImageMetadataResponse> {
        logger.debug("Searching images by required: $required, optional: $optional, forbidden: $forbidden")

        val uri = fromPath("/api/images/search")
            .addQueryParamIfAny("required", required)
            .addQueryParamIfAny("optional", optional)
            .addQueryParamIfAny("forbidden", forbidden)
            .toUriString()

        return restClient.get()
            .uri(uri)
            .retrieve()
            .body<List<ImageMetadataResponse>>() ?: emptyList()
    }

    private fun UriBuilder.addQueryParamIfAny(name: String, values: List<String>): UriBuilder {
        val cleaned = values.map { it.trim() }.filter { it.isNotEmpty() }
        return if (cleaned.isEmpty()) this else this.queryParam(name, *cleaned.toTypedArray())
    }

    fun getImageUrl(imagestoreId: Long): String {
        return "${properties.baseUrl}/api/images/$imagestoreId"
    }
}
