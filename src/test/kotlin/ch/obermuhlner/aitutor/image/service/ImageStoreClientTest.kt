package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.config.ImageStoreProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImageStoreClientTest {

    @Test
    fun `getImageUrl should return correct URL with default base URL`() {
        val properties = ImageStoreProperties(baseUrl = "http://localhost:7080")
        val client = ImageStoreClient(properties)
        val imageId = 123L

        val url = client.getImageUrl(imageId)

        assertEquals("http://localhost:7080/api/images/123", url)
    }

    @Test
    fun `getImageUrl should return correct URL with custom base URL`() {
        val properties = ImageStoreProperties(baseUrl = "https://example.com")
        val client = ImageStoreClient(properties)
        val imageId = 456L

        val url = client.getImageUrl(imageId)

        assertEquals("https://example.com/api/images/456", url)
    }

    @Test
    fun `ImageStoreClient should initialize with properties`() {
        val properties = ImageStoreProperties(baseUrl = "http://test:8080")

        assertDoesNotThrow {
            ImageStoreClient(properties)
        }
    }
}
