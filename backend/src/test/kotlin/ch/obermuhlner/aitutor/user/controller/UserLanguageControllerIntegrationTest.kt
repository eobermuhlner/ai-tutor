package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.dto.AddLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UpdateLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UserLanguageProficiencyResponse
import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

class UserLanguageControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getUserLanguages endpoint`() {
        // Test getting user languages when none exist
        val response = restTemplate.getForEntity(
            baseUrl("/users/$testUserId/languages"),
            Array<UserLanguageProficiencyResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }

    @Test
    fun `test addLanguage endpoint`() {
        // Test adding a language proficiency
        val addRequest = AddLanguageRequest(
            languageCode = "es",
            type = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(addRequest, headers)

        val response = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages"),
            HttpMethod.POST,
            entity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.userId).isEqualTo(testUserId)
        Assertions.assertThat(response.body!!.languageCode).isEqualTo("es")
        Assertions.assertThat(response.body!!.proficiencyType).isEqualTo(LanguageProficiencyType.Learning)
        Assertions.assertThat(response.body!!.cefrLevel).isEqualTo(CEFRLevel.A1)
        Assertions.assertThat(response.body!!.isNative).isFalse()
    }

    @Test
    fun `test getUserLanguages with specific type`() {
        // Add a language first
        val addRequest = AddLanguageRequest(
            languageCode = "es",
            type = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(addRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages"),
            HttpMethod.POST,
            entity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull

        // Test getting languages with specific type
        val response = restTemplate.getForEntity(
            baseUrl("/users/$testUserId/languages?type=Learning"),
            Array<UserLanguageProficiencyResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).hasSize(1)
        Assertions.assertThat(response.body!![0].languageCode).isEqualTo("es")
        Assertions.assertThat(response.body!![0].proficiencyType).isEqualTo(LanguageProficiencyType.Learning)
    }

    @Test
    fun `test updateLanguageLevel endpoint`() {
        // Add a language first
        val addRequest = AddLanguageRequest(
            languageCode = "fr",
            type = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(addRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages"),
            HttpMethod.POST,
            entity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull

        // Test updating the language level
        val updateRequest = UpdateLanguageRequest(CEFRLevel.B1)
        val updateHeaders = HttpHeaders()
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(updateRequest, updateHeaders)

        val response = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages/fr"),
            HttpMethod.PATCH,
            updateEntity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.languageCode).isEqualTo("fr")
        Assertions.assertThat(response.body!!.cefrLevel).isEqualTo(CEFRLevel.B1)
    }

    @Test
    fun `test setPrimaryTargetLanguage endpoint`() {
        // Add a language first
        val addRequest = AddLanguageRequest(
            languageCode = "de",
            type = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(addRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages"),
            HttpMethod.POST,
            entity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull

        // Test setting this language as primary
        val response = restTemplate.postForEntity(
            baseUrl("/users/$testUserId/languages/de/primary"),
            null,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.languageCode).isEqualTo("de")
        Assertions.assertThat(response.body!!.isPrimary).isTrue()
    }

    @Test
    fun `test removeLanguage endpoint`() {
        // Add a language first
        val addRequest = AddLanguageRequest(
            languageCode = "it",
            type = LanguageProficiencyType.Learning,
            cefrLevel = CEFRLevel.A1,
            isNative = false
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(addRequest, headers)

        val createResponse = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages"),
            HttpMethod.POST,
            entity,
            UserLanguageProficiencyResponse::class.java
        )

        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        Assertions.assertThat(createResponse.body).isNotNull

        // Verify the language was added
        val getResponse = restTemplate.getForEntity(
            baseUrl("/users/$testUserId/languages"),
            Array<UserLanguageProficiencyResponse>::class.java
        )
        Assertions.assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getResponse.body).hasSize(1)

        // Test removing the language
        val response = restTemplate.exchange(
            baseUrl("/users/$testUserId/languages/it"),
            HttpMethod.DELETE,
            null,
            Void::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        // Verify the language was removed
        val getResponseAfterDelete = restTemplate.getForEntity(
            baseUrl("/users/$testUserId/languages"),
            Array<UserLanguageProficiencyResponse>::class.java
        )
        Assertions.assertThat(getResponseAfterDelete.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(getResponseAfterDelete.body).isEmpty()
    }
}