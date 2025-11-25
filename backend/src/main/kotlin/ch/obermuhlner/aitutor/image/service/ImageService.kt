package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import java.text.Normalizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageService(
    private val githubImageStoreService: GithubImageStoreService
) {

    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    @Transactional(readOnly = true)
    fun getImageByConcept(concept: String): ImageData? {
        val searchResults = try {
            githubImageStoreService.searchImagesByTags(listOf(concept))
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

    @Transactional(readOnly = true)
    fun getImageUrlByConcept(concept: String): String? {
        val searchResults = try {
            githubImageStoreService.searchImagesByTags(listOf(concept))
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

        return getImageUrl(metadata)
    }

    private val separatorRegex = Regex("[\\p{Punct}\\s]+")
    private val markRegex = Regex("\\p{M}+")

    @Transactional(readOnly = true)
    fun getImageByPerson(countryCode: String, gender: TutorGender, age: Int, text: String): ImageData? {
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10

        val requiredTags = listOf("person", countryCode, gender.toString())
        val optionalTags = listOf("age_$age", "age_${ageLower}_${ageUpper}")

        val textTags = text.split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { word ->
                val normalized = normalizeToAscii(word)
                if (normalized.equals(word, ignoreCase = true)) listOf(word)
                else listOf(word, normalized)
            }
            .distinct()

        val searchResults = try {
            githubImageStoreService.searchImagesByTags(
                requiredTags,
                optionalTags + textTags,
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

    @Transactional(readOnly = true)
    fun getImageUrlByPerson(countryCode: String, gender: TutorGender, age: Int, text: String): String? {
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10

        val requiredTags = listOf("person", countryCode, gender.toString(), "age_$age", "age_${ageLower}_${ageUpper}")
        val optionalTags = listOf("age_$age", "age_${ageLower}_${ageUpper}")

        val textTags = text.split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { word ->
                val normalized = normalizeToAscii(word)
                if (normalized.equals(word, ignoreCase = true)) listOf(word)
                else listOf(word, normalized)
            }
            .distinct()

        val searchResults = try {
            githubImageStoreService.searchImagesByTags(
                requiredTags,
                optionalTags + textTags,
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

        return getImageUrl(metadata)
    }

    private fun normalizeToAscii(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(markRegex, "")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getImage(metadata: ImageMetadataResponse): ImageData? {
        // With GitHub Pages, we can't fetch the raw image data, only URLs
        // We'll return a placeholder or handle this differently based on requirements
        // For now, we'll return null since we can't fetch the actual image data

        // If we want to actually fetch image data, we'd need to download from the URL
        // But this would require additional network requests and caching
        // For now, we'll log that this method isn't fully supported with the GitHub approach
        logger.warn("getImage() not fully supported with GitHub Pages - only URLs available")
        return null
    }

    private fun getImageUrl(metadata: ImageMetadataResponse): String? {
        // Create URL from filename for GitHub Pages
        try {
            return githubImageStoreService.getImageUrlForFilename(metadata.filename)
        } catch (e: Exception) {
            logger.error("Failed to get image URL for filename: ${metadata.filename}", e)
            return null
        }
    }

}

data class ImageData(
    val data: ByteArray,
    val format: String,
    val contentType: String
)
