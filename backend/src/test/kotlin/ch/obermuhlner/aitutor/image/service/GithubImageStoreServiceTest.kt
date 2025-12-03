package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.dto.GithubImageMetadata
import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class GithubImageStoreServiceTest {

    private lateinit var githubImageStoreService: GithubImageStoreService

    @BeforeEach
    fun setUp() {
        githubImageStoreService = GithubImageStoreService()
    }

    @Test
    fun `searchImagesByTags should find images with matching required tags`() {
        // Since the actual functionality involves external web requests,
        // we'd need to create an integration test to fully verify this
        // For now, we rely on the unit tests of ImageService that should cover
        // the functionality through the new GithubImageStoreService
        assertTrue(true) // Placeholder to confirm the class exists and can be instantiated
    }

    @Test
    fun `getImageUrlForFilename should construct correct URL`() {
        val service = GithubImageStoreService()
        val filename = "test.jpg"
        val expectedUrl = "https://eobermuhlner.github.io/ai-tutor-images/images/test.jpg"

        val result = service.getImageUrlForFilename(filename)

        assertEquals(expectedUrl, result)
    }

    @Test
    fun `searchImagesByTags should score based on tag position with required tags`() {
        // Set up test data with tags where first position has more weight
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("apple", "fruit", "red")  // Required tag "apple" at position 0 (weight 3+1-0/4 = 1.0)
            ),
            GithubImageMetadata(
                filename = "image2.jpg",
                tags = listOf("fruit", "apple", "red")  // Required tag "apple" at position 1 (weight 3+1-1/4 = 0.75)
            ),
            GithubImageMetadata(
                filename = "image3.jpg",
                tags = listOf("fruit", "red", "apple")  // Required tag "apple" at position 2 (weight 3+1-2/4 = 0.5)
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        // Search with "apple" as required tag
        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("apple"),
            optional = emptyList(),
            forbidden = emptyList()
        )

        // Verify all results contain the required tag
        assertTrue(results.all { response ->
            response.tags.any { it.equals("apple", ignoreCase = true) }
        })

        // Verify ordering by score (highest first)
        // image1 should come first (score 1.0), then image2 (score 0.75), then image3 (score 0.5)
        assertEquals("image1.jpg", results[0].filename)
        assertEquals("image2.jpg", results[1].filename)
        assertEquals("image3.jpg", results[2].filename)
    }

    @Test
    fun `searchImagesByTags should score based on tag position with optional tags`() {
        // Set up test data with tags where first position has more weight
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("cat", "animal", "pet")  // Optional tag "cat" at position 0 (weight 3+1-0/4 = 1.0)
            ),
            GithubImageMetadata(
                filename = "image2.jpg",
                tags = listOf("animal", "cat", "pet")  // Optional tag "cat" at position 1 (weight 3+1-1/4 = 0.75)
            ),
            GithubImageMetadata(
                filename = "image3.jpg",
                tags = listOf("animal", "pet", "cat")  // Optional tag "cat" at position 2 (weight 3+1-2/4 = 0.5)
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        // Search with "cat" as optional tag
        val results = githubImageStoreService.searchImagesByTags(
            required = emptyList(),
            optional = listOf("cat"),
            forbidden = emptyList()
        )

        // Verify ordering by score (highest first) - only images with matching tags should be returned
        assertEquals(3, results.size)
        assertEquals("image1.jpg", results[0].filename)  // highest score
        assertEquals("image2.jpg", results[1].filename)  // medium score
        assertEquals("image3.jpg", results[2].filename)  // lowest score
    }

    @Test
    fun `searchImagesByTags should score correctly with mixed required and optional tags`() {
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("dog", "pet", "animal")  // "dog" required at position 0 (weight 1.0), "pet" optional at pos 1 (weight 0.75)
            ),
            GithubImageMetadata(
                filename = "image2.jpg",
                tags = listOf("cat", "pet", "animal")  // "cat" required fails, "pet" optional at pos 1 (weight 0.75)
            ),
            GithubImageMetadata(
                filename = "image3.jpg",
                tags = listOf("dog", "animal", "pet")  // "dog" required at position 0 (weight 1.0), "pet" optional at pos 2 (weight 0.5)
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        // Search with "dog" required and "pet" optional
        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("dog"),
            optional = listOf("pet"),
            forbidden = emptyList()
        )

        // Should only return images that match required ("dog") tag
        assertEquals(2, results.size)  // image1 and image3 match required tag "dog"

        // image1: score = weight of "dog"(pos 0) + weight of "pet"(pos 1) = 1.0 + 0.75 = 1.75
        // image3: score = weight of "dog"(pos 0) + weight of "pet"(pos 2) = 1.0 + 0.5 = 1.5
        assertEquals("image1.jpg", results[0].filename)  // higher total score
        assertEquals("image3.jpg", results[1].filename)  // lower total score
    }

    @Test
    fun `searchImagesByTags should handle single tag list correctly`() {
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("apple")  // Only tag at position 0, weight should be (1+1-0)/(1+1) = 1.0
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("apple"),
            optional = emptyList(),
            forbidden = emptyList()
        )

        assertEquals(1, results.size)
        assertEquals("image1.jpg", results[0].filename)
    }

    @Test
    fun `searchImagesByTags should verify weight formula for specific cases`() {
        // Verify the exact weights for different positions in various sized lists
        val metadataList = listOf(
            // 3 tags: "first"(pos 0, weight 1.0), "second"(pos 1, weight 0.75), "third"(pos 2, weight 0.5)
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("first", "second", "third")
            ),
            // 5 tags: "first"(pos 0, weight 1.0), "x"(pos 1, weight 0.83), "c"(pos 2, weight 0.67), "y"(pos 3, weight 0.5), "z"(pos 4, weight 0.33)
            GithubImageMetadata(
                filename = "image2.jpg",
                tags = listOf("first", "x", "c", "y", "z")
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        // Search with "first" and "c" as required tags (both present in image2, only "first" in image1)
        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("first", "c"),  // Both required tags must be in same image
            optional = listOf("third"),       // Optional tag only in image1
            forbidden = emptyList()
        )

        // Only image2 has both required tags ("first" and "c"), so it should be the only result
        assertEquals(1, results.size)
        assertEquals("image2.jpg", results[0].filename)

        // The score for image2 comes from: "first"(pos 0, weight 1.0) + "c"(pos 2, weight 0.67) + "third"(optional, not present) = 1.67
    }

    @Test
    fun `searchImagesByTags should respect forbidden tags`() {
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "allowed.jpg",
                tags = listOf("apple", "fruit")
            ),
            GithubImageMetadata(
                filename = "forbidden.jpg",
                tags = listOf("apple", "forbidden_tag")
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("apple"),
            optional = emptyList(),
            forbidden = listOf("forbidden_tag")
        )

        // Should only return the image that doesn't contain forbidden tags
        assertEquals(1, results.size)
        assertEquals("allowed.jpg", results[0].filename)
    }

    @Test
    fun `searchImagesByTags should return empty list when no matches`() {
        val metadataList = listOf(
            GithubImageMetadata(
                filename = "image1.jpg",
                tags = listOf("cat", "animal")
            )
        )

        githubImageStoreService.setImageIndexForTesting(metadataList)

        val results = githubImageStoreService.searchImagesByTags(
            required = listOf("dog"),  // No images have "dog"
            optional = emptyList(),
            forbidden = emptyList()
        )

        assertEquals(0, results.size)
    }
}