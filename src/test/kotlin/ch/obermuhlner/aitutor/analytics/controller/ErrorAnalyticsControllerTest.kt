package ch.obermuhlner.aitutor.analytics.controller

import ch.obermuhlner.aitutor.analytics.domain.ErrorPatternEntity
import ch.obermuhlner.aitutor.analytics.domain.RecentErrorSampleEntity
import ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(ErrorAnalyticsController::class)
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class ErrorAnalyticsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var errorAnalyticsService: ErrorAnalyticsService

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `getErrorPatterns should return top patterns for user`() {
        val userId = UUID.randomUUID()
        val patterns = listOf(
            ErrorPatternEntity(
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                totalCount = 20,
                criticalCount = 5,
                highCount = 10,
                mediumCount = 5,
                lowCount = 0,
                firstSeenAt = Instant.parse("2025-01-01T10:00:00Z"),
                lastSeenAt = Instant.parse("2025-01-10T15:00:00Z")
            )
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getTopPatterns(userId, "es", 5) } returns patterns

        mockMvc.perform(get("/api/v1/analytics/errors/patterns")
            .param("lang", "es")
            .param("limit", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].errorType").value("Agreement"))
            .andExpect(jsonPath("$[0].totalCount").value(20))
            .andExpect(jsonPath("$[0].criticalCount").value(5))
            .andExpect(jsonPath("$[0].highCount").value(10))
            .andExpect(jsonPath("$[0].mediumCount").value(5))
            .andExpect(jsonPath("$[0].lowCount").value(0))
            .andExpect(jsonPath("$[0].weightedScore").value(40.0))
    }

    @Test
    @WithMockUser
    fun `getErrorPatterns should use default limit when not specified`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getTopPatterns(userId, "es", 5) } returns emptyList()

        mockMvc.perform(get("/api/v1/analytics/errors/patterns")
            .param("lang", "es"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @WithMockUser
    fun `getErrorPatterns should return empty list when no patterns exist`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getTopPatterns(userId, "fr", 5) } returns emptyList()

        mockMvc.perform(get("/api/v1/analytics/errors/patterns")
            .param("lang", "fr"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    @WithMockUser
    fun `getErrorTrend should return trend for specific error type`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.computeTrend(userId, ErrorType.Agreement) } returns "IMPROVING"

        mockMvc.perform(get("/api/v1/analytics/errors/trends/Agreement")
            .param("lang", "es"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errorType").value("Agreement"))
            .andExpect(jsonPath("$.trend").value("IMPROVING"))
    }

    @Test
    @WithMockUser
    fun `getErrorTrend should return INSUFFICIENT_DATA when not enough samples`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.computeTrend(userId, ErrorType.TenseAspect) } returns "INSUFFICIENT_DATA"

        mockMvc.perform(get("/api/v1/analytics/errors/trends/TenseAspect")
            .param("lang", "de"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errorType").value("TenseAspect"))
            .andExpect(jsonPath("$.trend").value("INSUFFICIENT_DATA"))
    }

    @Test
    @WithMockUser
    fun `getErrorTrend should return WORSENING trend`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.computeTrend(userId, ErrorType.WordOrder) } returns "WORSENING"

        mockMvc.perform(get("/api/v1/analytics/errors/trends/WordOrder")
            .param("lang", "ja"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errorType").value("WordOrder"))
            .andExpect(jsonPath("$.trend").value("WORSENING"))
    }

    @Test
    @WithMockUser
    fun `getErrorTrend should return STABLE trend`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.computeTrend(userId, ErrorType.Lexis) } returns "STABLE"

        mockMvc.perform(get("/api/v1/analytics/errors/trends/Lexis")
            .param("lang", "zh"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errorType").value("Lexis"))
            .andExpect(jsonPath("$.trend").value("STABLE"))
    }

    @Test
    @WithMockUser
    fun `getRecentSamples should return recent error samples`() {
        val userId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val sampleId = UUID.randomUUID()
        val occurredAt = Instant.parse("2025-01-12T14:30:00Z")

        val samples = listOf(
            RecentErrorSampleEntity(
                id = sampleId,
                userId = userId,
                lang = "es",
                errorType = ErrorType.Agreement,
                severity = ErrorSeverity.Medium,
                messageId = messageId,
                errorSpan = "gehen",
                occurredAt = occurredAt
            )
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getRecentSamples(userId, 20) } returns samples

        mockMvc.perform(get("/api/v1/analytics/errors/samples")
            .param("limit", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(sampleId.toString()))
            .andExpect(jsonPath("$[0].errorType").value("Agreement"))
            .andExpect(jsonPath("$[0].severity").value("Medium"))
            .andExpect(jsonPath("$[0].errorSpan").value("gehen"))
            .andExpect(jsonPath("$[0].occurredAt").value("2025-01-12T14:30:00Z"))
    }

    @Test
    @WithMockUser
    fun `getRecentSamples should use default limit when not specified`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getRecentSamples(userId, 20) } returns emptyList()

        mockMvc.perform(get("/api/v1/analytics/errors/samples"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    @WithMockUser
    fun `getRecentSamples should return empty list when no samples exist`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { errorAnalyticsService.getRecentSamples(userId, 10) } returns emptyList()

        mockMvc.perform(get("/api/v1/analytics/errors/samples")
            .param("limit", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `all endpoints should require authentication`() {
        mockMvc.perform(get("/api/v1/analytics/errors/patterns")
            .param("lang", "es"))
            .andExpect(status().isForbidden)

        mockMvc.perform(get("/api/v1/analytics/errors/trends/Agreement")
            .param("lang", "es"))
            .andExpect(status().isForbidden)

        mockMvc.perform(get("/api/v1/analytics/errors/samples"))
            .andExpect(status().isForbidden)
    }
}
