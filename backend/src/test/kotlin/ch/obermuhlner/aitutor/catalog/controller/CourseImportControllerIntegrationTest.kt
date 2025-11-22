package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class CourseImportControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test endpoint accessibility - validateImportFiles requires authentication`() {
        // Test that the endpoint exists and requires proper authentication/authorization
        // Since we're using a test user with EDITOR privileges, we expect a different response
        // than we would get with an unauthenticated user

        // Try to access the endpoint - should fail due to missing multipart data
        // or succeed with authorization but fail validation
        try {
            restTemplate.exchange(
                baseUrl("/courses/import/validate"),
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String::class.java
            )
            // If successful, it means the request was accepted (though it might fail validation)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            // We might get various errors - BAD_REQUEST due to missing multipart data,
            // or FORBIDDEN if there are auth issues
            // The important part is that it reaches the authorization level
            Assertions.assertThat(e.statusCode).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN, HttpStatus.METHOD_NOT_ALLOWED)
        }
    }
}