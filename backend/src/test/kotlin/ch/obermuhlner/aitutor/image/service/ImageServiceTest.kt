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

    private lateinit var githubImageStoreService: GithubImageStoreService
    private lateinit var imageServiceImpl: ImageService

    @BeforeEach
    fun setUp() {
        githubImageStoreService = mock(GithubImageStoreService::class.java)
        imageServiceImpl = ImageService(githubImageStoreService)
    }

    @Test
    fun `getImageByConcept should search from github image store`() {
        val concept = "apple"

        val metadata = ImageMetadataResponse(
            id = 1L,
            filename = "apple.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf(concept)
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByConcept(concept)

        // Result will be null since we can't fetch raw image data from GitHub Pages
        // This method is not fully supported with the GitHub approach
        assertNull(result)

        verify(githubImageStoreService, times(1)).searchImagesByTags(listOf(concept))
    }

    @Test
    fun `getImageByConcept should return null if no search results`() {
        val concept = "nonexistent"

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(emptyList())

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(githubImageStoreService, times(1)).searchImagesByTags(listOf(concept))
    }

    @Test
    fun `getImageByConcept should return null if search throws exception`() {
        val concept = "search-error"

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenThrow(RuntimeException("Search failed"))

        val result = imageServiceImpl.getImageByConcept(concept)

        assertNull(result)

        verify(githubImageStoreService, times(1)).searchImagesByTags(listOf(concept))
    }

    // This test was already updated above

    // This test is no longer applicable since we can't fetch raw data from GitHub Pages

    @Test
    fun `getImageByPerson should search with required and optional tags`() {
        val countryCode = "JP"
        val gender = TutorGender.Female
        val age = 25
        val text = "teacher school"

        val metadata = ImageMetadataResponse(
            id = 3L,
            filename = "teacher.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person", countryCode, gender.toString(), "age_$age", "teacher")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        // Result will be null since we can't fetch raw image data from GitHub Pages
        assertNull(result)

        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}", "teacher", "school")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    @Test
    fun `getImageByPerson should return null if no results`() {
        val countryCode = "FR"
        val gender = TutorGender.Male
        val age = 40
        val text = "doctor"

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(emptyList())

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            listOf("person", countryCode, gender.toString()),
            listOf("age_$age", "age_40_50", "doctor")
        )
    }

    @Test
    fun `getImageByPerson should normalize accented characters to ASCII`() {
        val countryCode = "ES"
        val gender = TutorGender.Male
        val age = 30
        val text = "café niño"

        val metadata = ImageMetadataResponse(
            id = 4L,
            filename = "person.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)  // Result will be null since we can't fetch raw image data

        // Verify normalized tags are included (café -> cafe, niño -> nino)
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}", "café", "cafe", "niño", "nino")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    @Test
    fun `getImageByPerson should handle empty text`() {
        val countryCode = "US"
        val gender = TutorGender.Female
        val age = 35
        val text = ""

        val metadata = ImageMetadataResponse(
            id = 5L,
            filename = "person.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)  // Result will be null since we can't fetch raw image data

        // Should only have required tags, and age range as optional tags
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    @Test
    fun `getImageByPerson should handle text with punctuation and whitespace`() {
        val countryCode = "DE"
        val gender = TutorGender.Male
        val age = 45
        val text = "  engineer,  programmer;  developer  "

        val metadata = ImageMetadataResponse(
            id = 6L,
            filename = "engineer.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person", "engineer")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)  // Result will be null since we can't fetch raw image data

        // Verify tags are split and trimmed correctly
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}", "engineer", "programmer", "developer")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    @Test
    fun `getImageByPerson should return null if search throws exception`() {
        val countryCode = "IT"
        val gender = TutorGender.Female
        val age = 28
        val text = "artist"

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenThrow(RuntimeException("Search error"))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            listOf("person", countryCode, gender.toString()),
            listOf("age_$age", "age_20_30", "artist")
        )
    }

    // This test method is no longer applicable since we don't fetch raw image data from GitHub Pages

    @Test
    fun `getImageByPerson should deduplicate normalized and original tags`() {
        val countryCode = "FR"
        val gender = TutorGender.Female
        val age = 32
        val text = "école school école"  // Duplicate words, one needs normalization

        val metadata = ImageMetadataResponse(
            id = 8L,
            filename = "school.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person", "school")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)  // Result will be null since we can't fetch raw image data

        // Verify deduplication: école (twice) + ecole + school (twice) -> école, ecole, school
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}", "école", "ecole", "school")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    @Test
    fun `getImageByPerson should handle words that don't need normalization`() {
        val countryCode = "GB"
        val gender = TutorGender.Male
        val age = 40
        val text = "doctor nurse"  // No accents, should not duplicate

        val metadata = ImageMetadataResponse(
            id = 9L,
            filename = "doctor.jpg",
            contentType = "image/jpeg",
            size = 0L,
            uploadDate = Instant.now(),
            storageType = "GITHUB_PAGES",
            tags = listOf("person", "doctor")
        )

        whenever(githubImageStoreService.searchImagesByTags(any(), any(), any())).thenReturn(listOf(metadata))

        val result = imageServiceImpl.getImageByPerson(countryCode, gender, age, text)

        assertNull(result)  // Result will be null since we can't fetch raw image data

        // Words without accents should only appear once
        val ageLower = (age / 10) * 10
        val ageUpper = ageLower + 10
        val expectedRequired = listOf("person", countryCode, gender.toString())
        val expectedOptional = listOf("age_$age", "age_${ageLower}_${ageUpper}", "doctor", "nurse")
        verify(githubImageStoreService, times(1)).searchImagesByTags(
            expectedRequired,
            expectedOptional
        )
    }

    // This test is no longer fully applicable since we can't fetch raw data from GitHub Pages
}
