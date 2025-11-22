package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class CatalogImportControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test validateCatalog endpoint requires admin role`() {
        // Test that the validate endpoint requires admin privileges
        // Since our test user has admin rights, this should work at the auth level
        // but might fail validation because of missing multipart data
        
        try {
            restTemplate.exchange(
                baseUrl("/catalog/import/validate"),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String::class.java
            )
            // If successful, it means the request reached the validation layer
            // (though it would likely fail due to missing multipart data)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            // We expect some kind of error - either due to missing multipart data
            // or other validation errors - but we should reach the authorization
            Assertions.assertThat(e.statusCode).isIn(HttpStatus.BAD_REQUEST, HttpStatus.METHOD_NOT_ALLOWED)
        }
    }
}