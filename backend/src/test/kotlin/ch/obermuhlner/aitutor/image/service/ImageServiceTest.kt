package ch.obermuhlner.aitutor.image.service

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class ImageServiceTest {

    private lateinit var imageStoreClient: ImageStoreClient
    private lateinit var imageServiceImpl: ImageService

    @BeforeEach
    fun setUp() {
        imageStoreClient = mock(ImageStoreClient::class.java)
        imageServiceImpl = ImageService(imageStoreClient)
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

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNotNull(result)
        assertEquals(imageBytes, result?.data)
        assertEquals("png", result?.format)
        assertEquals("image/png", result?.contentType)

        verify(imageStoreClient, times(1)).searchImagesByTags(listOf(concept), emptyList(), emptyList())
        verify(imageStoreClient, times(1)).getImageData(1L)
    }

    @Test
    fun `getImageByConcept should return null if no search results`() {
        val concept = "nonexistent"

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(emptyList())

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageStoreClient, times(1)).searchImagesByTags(listOf(concept), emptyList(), emptyList())
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

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenThrow(RuntimeException("Network error"))

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageStoreClient, times(1)).searchImagesByTags(listOf(concept), emptyList(), emptyList())
        verify(imageStoreClient, times(1)).getImageData(1L)
    }

    @Test
    fun `getImageByConcept should return null if search throws exception`() {
        val concept = "search-error"

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenThrow(RuntimeException("Search failed"))

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(imageStoreClient, times(1)).searchImagesByTags(listOf(concept), emptyList(), emptyList())
        verify(imageStoreClient, never()).getImageData(any())
    }

    @Test
    fun `getImageByConcept should handle different content types`() {
        val concept = "jpeg-test"
        val imageBytes = ByteArray(200) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 2L,
            filename = "test.jpg",
            contentType = "image/jpeg",
            size = 200L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf(concept)
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNotNull(result)
        assertEquals("jpeg", result?.format)
        assertEquals("image/jpeg", result?.contentType)
    }

    @Test
    fun `getImageByPerson should search with required and optional tags`() {
        val countryCode = "JP"
        val gender = TutorGender.Female
        val age = 25
        val text = "teacher school"
        val imageBytes = ByteArray(150) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 3L,
            filename = "teacher.png",
            contentType = "image/png",
            size = 150L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person", countryCode, gender.toString(), "age_$age", "teacher")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)
        assertEquals(imageBytes, result?.data)
        assertEquals("png", result?.format)

        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(listOf("teacher", "school")),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByPerson should return null if no results`() {
        val countryCode = "FR"
        val gender = TutorGender.Male
        val age = 40
        val text = "doctor"

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(emptyList())

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)
        verify(imageStoreClient, never()).getImageData(any())
    }

    @Test
    fun `getImageByPerson should normalize accented characters to ASCII`() {
        val countryCode = "ES"
        val gender = TutorGender.Male
        val age = 30
        val text = "café niño"
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 4L,
            filename = "person.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)

        // Verify normalized tags are included (café -> cafe, niño -> nino)
        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(listOf("café", "cafe", "niño", "nino")),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByPerson should handle empty text`() {
        val countryCode = "US"
        val gender = TutorGender.Female
        val age = 35
        val text = ""
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 5L,
            filename = "person.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)

        // Should only have required tags, no optional tags
        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(emptyList()),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByPerson should handle text with punctuation and whitespace`() {
        val countryCode = "DE"
        val gender = TutorGender.Male
        val age = 45
        val text = "  engineer,  programmer;  developer  "
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 6L,
            filename = "engineer.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person", "engineer")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)

        // Verify tags are split and trimmed correctly
        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(listOf("engineer", "programmer", "developer")),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByPerson should return null if search throws exception`() {
        val countryCode = "IT"
        val gender = TutorGender.Female
        val age = 28
        val text = "artist"

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenThrow(RuntimeException("Search error"))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)
        verify(imageStoreClient, never()).getImageData(any())
    }

    @Test
    fun `getImageByPerson should return null if image fetch fails`() {
        val countryCode = "BR"
        val gender = TutorGender.Male
        val age = 50
        val text = "musician"

        val metadata = ImageMetadataResponse(
            id = 7L,
            filename = "musician.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person", "musician")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenThrow(RuntimeException("Fetch failed"))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)
        verify(imageStoreClient, times(1)).getImageData(7L)
    }

    @Test
    fun `getImageByPerson should deduplicate normalized and original tags`() {
        val countryCode = "FR"
        val gender = TutorGender.Female
        val age = 32
        val text = "école school école"  // Duplicate words, one needs normalization
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 8L,
            filename = "school.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person", "school")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)

        // Verify deduplication: école (twice) + ecole + school (twice) -> école, ecole, school
        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(listOf("école", "ecole", "school")),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByPerson should handle words that don't need normalization`() {
        val countryCode = "GB"
        val gender = TutorGender.Male
        val age = 40
        val text = "doctor nurse"  // No accents, should not duplicate
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 9L,
            filename = "doctor.png",
            contentType = "image/png",
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf("person", "doctor")
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNotNull(result)

        // Words without accents should only appear once
        verify(imageStoreClient, times(1)).searchImagesByTags(
            eq(listOf("person", countryCode, gender.toString(), "age_$age")),
            eq(listOf("doctor", "nurse")),
            eq(emptyList())
        )
    }

    @Test
    fun `getImageByConcept should handle content type without slash`() {
        val concept = "special-format"
        val imageBytes = ByteArray(100) { it.toByte() }

        val metadata = ImageMetadataResponse(
            id = 10L,
            filename = "special.img",
            contentType = "customtype",  // No slash
            size = 100L,
            uploadDate = Instant.now(),
            storageType = "filesystem",
            tags = listOf(concept)
        )

        whenever(imageStoreClient.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))
        whenever(imageStoreClient.getImageData(any())).thenReturn(imageBytes)

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNotNull(result)
        assertEquals("png", result?.format)  // Default to png when no slash
        assertEquals("customtype", result?.contentType)
    }
}
