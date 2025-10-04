package ch.obermuhlner.aitutor.image.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageServiceImpl(
    private val imageStoreClient: ImageStoreClient
) : ImageService {

    private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun getImageByConcept(concept: String): ImageData? {
        // Search imagestore by tag instead of querying local references
        val searchResults = imageStoreClient.searchImagesByTag(concept)

        if (searchResults.isEmpty()) {
            logger.debug("No image found for concept: $concept")
            return null
        }

        // Take first match
        val metadata = searchResults.first()

        return try {
            val data = imageStoreClient.getImageData(metadata.id)
            val format = metadata.contentType.substringAfter("/", "png")
            ImageData(data, format, metadata.contentType)
        } catch (e: Exception) {
            logger.error("Failed to fetch image from imagestore: concept=$concept, id=${metadata.id}", e)
            null
        }
    }

}
