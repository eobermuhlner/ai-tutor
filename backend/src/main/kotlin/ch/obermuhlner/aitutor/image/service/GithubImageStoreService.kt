package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.dto.GithubImageMetadata
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
class GithubImageStoreService {
    private val logger = LoggerFactory.getLogger(GithubImageStoreService::class.java)

    private val restClient = RestClient.builder()
        .build()

    private val githubImageUrl = "https://eobermuhlner.github.io/ai-tutor-images/images/"
    private val githubIndexUrl = "https://eobermuhlner.github.io/ai-tutor-images/images/index.jsonl"

    // Cache for storing the image index
    private var imageIndex: List<GithubImageMetadata> = emptyList()
    private val cacheUpdateLock = ReentrantReadWriteLock()
    private var lastCacheUpdate: Instant? = null

    companion object {
        // Maximum cache duration (1 hour)
        private const val CACHE_DURATION_MILLIS = 3600000L
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
            // Calculate score based on how many optional tags match
            val optionalMatchCount = optional.count { tag ->
                tag.lowercase() in metadata.tags.map { it.lowercase() }
            }

            // Sort results by how many optional tags match (higher score first)
            // Create ImageMetadataResponse with a fake ID for compatibility
            val fakeId = metadata.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL // Make sure it's positive

            Pair(optionalMatchCount, ImageMetadataResponse(
                id = fakeId,
                filename = metadata.filename,
                contentType = "image/jpeg", // Assume JPEG for now, could make this more dynamic
                size = 0, // Size not available in index
                uploadDate = Instant.now(), // Use current time as approximation
                storageType = "GITHUB_PAGES",
                tags = metadata.tags
            ))
        }.sortedByDescending { (matchCount, _) -> matchCount }
        .map { (_, response) -> response }
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