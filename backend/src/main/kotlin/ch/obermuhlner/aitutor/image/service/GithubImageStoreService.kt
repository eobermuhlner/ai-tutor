package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.dto.GithubImageMetadata
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
open class GithubImageStoreService {
    private val logger = LoggerFactory.getLogger(GithubImageStoreService::class.java)

    private val restClient = RestClient.builder()
        .build()

    private val githubImageUrl = "https://eobermuhlner.github.io/ai-tutor-images/images/"
    private val githubIndexUrl = "https://eobermuhlner.github.io/ai-tutor-images/images/index.jsonl"

    // Cache for storing the image index
    protected var imageIndex: List<GithubImageMetadata> = emptyList()
    protected val cacheUpdateLock = ReentrantReadWriteLock()
    private var lastCacheUpdate: Instant? = null

    companion object {
        // Maximum cache duration (1 hour)
        private const val CACHE_DURATION_MILLIS = 3600000L
    }

    // For testing purposes - allows setting the image index directly
    fun setImageIndexForTesting(imageIndex: List<GithubImageMetadata>) {
        cacheUpdateLock.write {
            this.imageIndex = imageIndex
            lastCacheUpdate = Instant.now()
        }
    }

    fun searchImagesByTags(
        required: List<String>,
        optional: List<String> = emptyList(),
        forbidden: List<String> = emptyList(),
    ): List<ImageMetadataResponse> {
        logger.debug("Searching images by required: $required, optional: $optional, forbidden: $forbidden")

        // Update the cache if needed
        updateImageIndexIfNeeded()

        // Read from cache
        val cachedIndex = cacheUpdateLock.read { imageIndex }

        return cachedIndex.filter { metadata ->
            // Check required tags: all required tags must be present
            val hasAllRequired = required.all { tag -> tag.lowercase() in metadata.tags.map { it.lowercase() } }

            // Check forbidden tags: none of the forbidden tags should be present
            val hasForbidden = forbidden.any { tag -> tag.lowercase() in metadata.tags.map { it.lowercase() } }

            hasAllRequired && !hasForbidden
        }.map { metadata ->
            // Calculate weighted gradient score based on position in metadata.tags
            val totalTags = metadata.tags.size
            if (totalTags == 0) {
                // If no tags, return score of 0
                Pair(0.0, createImageMetadataResponse(metadata))
            } else {
                // Calculate score by summing weights of matching tags
                var score = 0.0

                // Calculate weights for all tags in metadata based on position
                for ((index, tag) in metadata.tags.withIndex()) {
                    val normalizedTag = tag.lowercase()
                    val tagWeight = (totalTags + 1 - index) / (totalTags + 1.0)

                    // Add weight to score if tag matches required or optional tags
                    if (required.any { it.lowercase() == normalizedTag } ||
                        optional.any { it.lowercase() == normalizedTag }) {
                        score += tagWeight
                    }
                }

                Pair(score, createImageMetadataResponse(metadata))
            }
        }.sortedByDescending { (score, _) -> score }
        .map { (_, response) -> response }
    }

    private fun createImageMetadataResponse(metadata: GithubImageMetadata): ImageMetadataResponse {
        // Create ImageMetadataResponse with a fake ID for compatibility
        val fakeId = metadata.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL // Make sure it's positive

        return ImageMetadataResponse(
            id = fakeId,
            filename = metadata.filename,
            contentType = "image/jpeg", // Assume JPEG for now, could make this more dynamic
            size = 0, // Size not available in index
            uploadDate = Instant.now(), // Use current time as approximation
            storageType = "GITHUB_PAGES",
            tags = metadata.tags
        )
    }

    private fun updateImageIndexIfNeeded() {
        val shouldUpdate = cacheUpdateLock.read {
            lastCacheUpdate == null || 
            Instant.now().toEpochMilli() - (lastCacheUpdate?.toEpochMilli() ?: 0) > CACHE_DURATION_MILLIS
        }

        if (shouldUpdate) {
            cacheUpdateLock.write {
                try {
                    logger.info("Updating image index from GitHub Pages...")
                    val newIndex = fetchImageIndexFromGithub()
                    imageIndex = newIndex
                    lastCacheUpdate = Instant.now()
                    logger.info("Updated image index with ${imageIndex.size} images")
                } catch (e: Exception) {
                    logger.error("Failed to update image index from GitHub Pages", e)
                    // Keep the old index if update fails
                }
            }
        }
    }

    private fun fetchImageIndexFromGithub(): List<GithubImageMetadata> {
        val response = restClient.get()
            .uri(githubIndexUrl)
            .retrieve()
            .body(String::class.java) ?: throw IllegalStateException("Failed to fetch image index")

        return response.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line: String ->
                try {
                    parseGithubImageMetadata(line.trim())
                } catch (e: Exception) {
                    logger.warn("Failed to parse image metadata line: $line", e)
                    null
                }
            }
    }

    private fun parseGithubImageMetadata(jsonLine: String): GithubImageMetadata {
        return Json.decodeFromString<GithubImageMetadata>(jsonLine)
    }

    fun getImageUrlForFilename(filename: String): String {
        return "$githubImageUrl$filename"
    }
}