package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageService(
    private val imageStoreClient: ImageStoreClient
) {

    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    @Transactional(readOnly = true)
    fun getImageByConcept(concept: String): ImageData? {
        val searchResults = try {
            imageStoreClient.searchImagesByTags(listOf(concept))
        } catch (e: Exception) {
            logger.error("Failed to search images", e)
            emptyList()
        }

        if (searchResults.isEmpty()) {
            logger.debug("No image found for concept: $concept")
            return null
        }

        // Take first match
        val metadata = searchResults.first()

        return getImage(metadata)
    }

    val nonAlphaNumericRegex = Regex("[^A-Za-z0-9]+")

    @Transactional(readOnly = true)
    fun getImageByPerson(countryCode: String, gender: TutorGender, age: Int, text: String): ImageData? {
        val requiredTags = listOf("person", countryCode, gender.toString(), "age_$age")
        val optionalTags = text.split(nonAlphaNumericRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val searchResults = try {
            imageStoreClient.searchImagesByTags(
                requiredTags,
                optionalTags,
            )
        } catch (e: Exception) {
            logger.error("Failed to search images", e)
            emptyList()
        }

        if (searchResults.isEmpty()) {
            logger.debug("No image found for person: $requiredTags")
            return null
        }

        // Take first match
        val metadata = searchResults.first()

        return getImage(metadata)
    }

    private fun getImage(metadata: ImageMetadataResponse): ImageData? {
        return try {
            val data = imageStoreClient.getImageData(metadata.id)
            val format = metadata.contentType.substringAfter("/", "png")
            ImageData(data, format, metadata.contentType)
        } catch (e: Exception) {
            logger.error("Failed to fetch image from imagestore: id=${metadata.id}", e)
            null
        }
    }

}

data class ImageData(
    val data: ByteArray,
    val format: String,
    val contentType: String
)
