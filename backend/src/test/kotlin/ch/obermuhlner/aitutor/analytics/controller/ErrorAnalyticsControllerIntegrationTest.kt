package ch.obermuhlner.aitutor.analytics.controller

import ch.obermuhlner.aitutor.testutil.BaseControllerIntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

class ErrorAnalyticsControllerIntegrationTest : BaseControllerIntegrationTest() {

    @Test
    fun `test getErrorPatterns endpoint`() {
        // Test getting error patterns when none exist
        val response = restTemplate.getForEntity(
            baseUrl("/analytics/errors/patterns?lang=es&limit=5"),
            Array<ch.obermuhlner.aitutor.analytics.dto.ErrorPatternResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }

    @Test
    fun `test getErrorTrend endpoint`() {
        // Test getting error trend for a specific error type
        val response = restTemplate.getForEntity(
            baseUrl("/analytics/errors/trends/Articles?lang=es"),
            ch.obermuhlner.aitutor.analytics.dto.ErrorTrendResponse::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body!!.errorType).isEqualTo("Articles")
        // Trend could be any of the enum values, so just check it's not null
        Assertions.assertThat(response.body!!.trend).isNotNull()
    }

    @Test
    fun `test getRecentSamples endpoint`() {
        // Test getting recent error samples
        val response = restTemplate.getForEntity(
            baseUrl("/analytics/errors/samples?limit=20"),
            Array<ch.obermuhlner.aitutor.analytics.dto.ErrorSampleResponse>::class.java
        )

        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(response.body).isNotNull
        Assertions.assertThat(response.body).isEmpty()
    }
}