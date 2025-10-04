package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.image.dto.ImageMetadataResponse
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class ImageServiceTest {

    private lateinit var imageStoreClient: ImageStoreClient
    private lateinit var imageServiceImpl: ImageServiceImpl

    @BeforeEach
    fun setUp() {
        imageStoreClient = mock(ImageStoreClient::class.java)
        imageServiceImpl = ImageServiceImpl(imageStoreClient)
    }

    @Test
    fun `getImageByConcept should search and fetch from imagestore`() {
        val concept = "apple"
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 1L,
            filename = "apple.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf(concept)
        )

        whenever(imageStoreClient.searchImagesByTag(any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNotNull(result)
        assertEquals(imageBytes, result?.data)
        assertEquals("png", result?.format)
        assertEquals("image/png", result?.contentType)

        verify(imageStoreClient, times(1)).searchImagesByTag(concept)
        verify(imageStoreClient, times(1)).getImageData(1L)
    }

    @Test
    fun `getImageByConcept should return null if no search results`() {
        val concept = "nonexistent"

        whenever(imageStoreClient.searchImagesByTag(any())).thenReturn(emptyList())

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageStoreClient, times(1)).searchImagesByTag(concept)
        verify(imageStoreClient, never()).getImageData(any())
    }

    @Test
    fun `getImageByConcept should return null if fetch fails`() {
        val concept = "error-case"

        val metadata = ImageMetadataResponse(
            id = 1L,
            filename = "error.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf(concept)
        )

        whenever(imageStoreClient.searchImagesByTag(any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenThrow(RuntimeException("Network error"))

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageStoreClient, times(1)).searchImagesByTag(concept)
        verify(imageStoreClient, times(1)).getImageData(1L)
    }
}
