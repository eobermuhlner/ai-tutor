package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.core.dto.ErrorResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class VocabularyControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getUserVocabulary endpoint`() {
        // Test getting user vocabulary when none exist
        val response = restTemplate.getForEntity(
            baseUrl("/vocabulary?userId=$testUserId"),
            Array<ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }

    @Test
    fun `test getUserVocabulary with language filter`() {
        // Test getting user vocabulary filtered by language
        val response = restTemplate.getForEntity(
            baseUrl("/vocabulary?userId=$testUserId&lang=es"),
            Array<ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }

    @Test
    fun `test getVocabularyItemWithContexts endpoint - not found when item doesn't exist`() {
        // Test getting a vocabulary item that doesn't exist
        val nonExistentId = UUID.randomUUID()
        val response = restTemplate.getForEntity(
            baseUrl("/vocabulary/$nonExistentId"),
            ErrorResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Assertions.assertThat(response.body?.message).contains("Vocabulary item not found")
    }

    @Test
    fun `test getDueVocabulary endpoint`() {
        // Test getting due vocabulary for review
        val response = restTemplate.getForEntity(
            baseUrl("/vocabulary/due?lang=es&limit=20"),
            Array<ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }

    @Test
    fun `test getDueCount endpoint`() {
        // Test getting count of due vocabulary
        val response = restTemplate.getForEntity(
            baseUrl("/vocabulary/due/count?lang=es"),
            ch.obermuhlner.aitutor.vocabulary.controller.DueCountResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.count).isEqualTo(0)
    }

}