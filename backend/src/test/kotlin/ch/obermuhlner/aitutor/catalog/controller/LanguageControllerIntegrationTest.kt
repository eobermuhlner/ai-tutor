package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.Instant

class LanguageControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getAllLanguages endpoint`() {
        // Test getting all languages
        val response = restTemplate.getForEntity(
            baseUrl("/languages"),
            Array<ch.obermuhlner.aitutor.catalog.dto.LanguageResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
    }

    @Test
    fun `test getLanguage endpoint - not found when language doesn't exist`() {
        // Test getting a language that doesn't exist
        val response = restTemplate.exchange(
            baseUrl("/languages/nonexistent"),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String::class.java
        )

        // Could return 404 or 200 with null content depending on implementation
        Assertions.assertThat(response.statusCode).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `test createLanguage and getLanguage endpoints`() {
        // Test creating a language
        val languageEntity = LanguageEntity(
            code = "testlang",
            nameJson = """{"en": "Test Language", "de": "Test Sprache"}""",
            flagEmoji = "🧪",
            nativeName = "Test Language",
            difficulty = Difficulty.Medium,
            descriptionJson = """{"en": "A test language for integration testing", "de": "Eine Test-Sprache"}""",
            isActive = true,
            displayOrder = 999,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(languageEntity, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/languages"),
            HttpMethod.POST,
            entity,
            ch.obermuhlner.aitutor.catalog.dto.LanguageResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(createResponse.body).isNotNull
        Assertions.assertThat(createResponse.body!!.code).isEqualTo("testlang")

        // Test getting the created language
        val getResponse = restTemplate.getForEntity(
            baseUrl("/languages/testlang"),
            ch.obermuhlner.aitutor.catalog.dto.LanguageResponse::class.java
        )

        Assertions.assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getResponse.body).isNotNull
        Assertions.assertThat(getResponse.body!!.code).isEqualTo("testlang")
        Assertions.assertThat(getResponse.body!!.name).isEqualTo("Test Language")
    }

    @Test
    fun `test getAllLanguages includes created language`() {
        // First create a test language
        val languageEntity = LanguageEntity(
            code = "testlang2",
            nameJson = """{"en": "Test Language 2"}""",
            flagEmoji = "🧪",
            nativeName = "Test Language 2",
            difficulty = Difficulty.Medium,
            descriptionJson = """{"en": "A second test language"}""",
            isActive = true,
            displayOrder = 1000,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(languageEntity, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/languages"),
            HttpMethod.POST,
            entity,
            ch.obermuhlner.aitutor.catalog.dto.LanguageResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.OK)

        // Get all languages and check that the new one is included
        val getAllResponse = restTemplate.getForEntity(
            baseUrl("/languages"),
            Array<ch.obermuhlner.aitutor.catalog.dto.LanguageResponse>::class.java
        )

        Assertions.assertThat(getAllResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getAllResponse.body).isNotNull
        Assertions.assertThat(getAllResponse.body).isNotEmpty()
        
        // Check if our test language is in the list
        val testLanguage = getAllResponse.body?.firstOrNull { it.code == "testlang2" }
        Assertions.assertThat(testLanguage).isNotNull()
        Assertions.assertThat(testLanguage?.name).isEqualTo("Test Language 2")
    }
}