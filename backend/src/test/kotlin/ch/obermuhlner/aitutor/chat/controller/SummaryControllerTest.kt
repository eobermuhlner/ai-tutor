package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryLevelInfo
import ch.obermuhlner.aitutor.chat.service.SummaryQueryService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(SummaryController::class)
@Import(ch.obermuhlner.aitutor.auth.config.SecurityConfig::class)
class SummaryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var summaryQueryService: SummaryQueryService

    @MockkBean(relaxed = true)
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @Test
    @WithMockUser
    fun `getSessionSummaryInfo should return summary statistics for valid session`() {
        val sessionId = UUID.randomUUID()
        val levelInfo = SummaryLevelInfo(
            level = 1,
            count = 3,
            totalTokens = 450,
            coveredSequences = 1..10
        )
        val info = SessionSummaryInfoResponse(
            sessionId = sessionId,
            totalMessages = 30,
            summaryLevels = listOf(levelInfo),
            lastSummarizedSequence = 10,
            estimatedTokenSavings = 750,
            compressionRatio = 2.67
        )

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryInfo(sessionId) } returns info

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
            .andExpect(jsonPath("$.totalMessages").value(30))
            .andExpect(jsonPath("$.summaryLevels[0].level").value(1))
            .andExpect(jsonPath("$.summaryLevels[0].count").value(3))
            .andExpect(jsonPath("$.estimatedTokenSavings").value(750))
            .andExpect(jsonPath("$.compressionRatio").value(2.67))

        verify { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        verify { summaryQueryService.getSessionSummaryInfo(sessionId) }
    }

    @Test
    @WithMockUser
    fun `getSessionSummaryDetails should return detailed summaries for valid session`() {
        val sessionId = UUID.randomUUID()
        val createdAt = Instant.parse("2025-01-15T10:00:00Z")
        val details = listOf(
            SummaryDetailResponse(
                id = UUID.randomUUID(),
                summaryLevel = 1,
                startSequence = 1,
                endSequence = 10,
                summaryText = "User practiced greetings and introductions",
                tokenCount = 12,
                sourceType = "MESSAGE",
                sourceIds = listOf(UUID.randomUUID()),
                supersededById = null,
                isActive = true,
                createdAt = createdAt
            ),
            SummaryDetailResponse(
                id = UUID.randomUUID(),
                summaryLevel = 1,
                startSequence = 11,
                endSequence = 20,
                summaryText = "Discussed daily routines and hobbies",
                tokenCount = 10,
                sourceType = "MESSAGE",
                sourceIds = listOf(UUID.randomUUID()),
                supersededById = null,
                isActive = true,
                createdAt = createdAt.plusSeconds(600)
            )
        )

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryDetails(sessionId) } returns details

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/details"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].summaryLevel").value(1))
            .andExpect(jsonPath("$[0].startSequence").value(1))
            .andExpect(jsonPath("$[0].endSequence").value(10))
            .andExpect(jsonPath("$[0].summaryText").value("User practiced greetings and introductions"))
            .andExpect(jsonPath("$[0].tokenCount").value(12))
            .andExpect(jsonPath("$[1].startSequence").value(11))

        verify { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        verify { summaryQueryService.getSessionSummaryDetails(sessionId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `triggerSummarization should trigger manual summarization for admin`() {
        val sessionId = UUID.randomUUID()

        justRun { authorizationService.requireAdmin(any()) }
        justRun { summaryQueryService.triggerManualSummarization(sessionId) }

        mockMvc.perform(post("/api/v1/summaries/sessions/$sessionId/trigger"))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.message").value("Summarization triggered asynchronously for session $sessionId"))

        verify { authorizationService.requireAdmin(any()) }
        verify { summaryQueryService.triggerManualSummarization(sessionId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getGlobalStats should return global summarization statistics for admin`() {
        val stats = mapOf(
            "totalSummaries" to 125,
            "totalSessions" to 45,
            "averageCompressionRatio" to 2.8,
            "totalCompressedTokens" to 15000,
            "estimatedOriginalTokens" to 42000
        )

        justRun { authorizationService.requireAdmin(any()) }
        every { summaryQueryService.getGlobalStats() } returns stats

        mockMvc.perform(get("/api/v1/summaries/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalSummaries").value(125))
            .andExpect(jsonPath("$.totalSessions").value(45))
            .andExpect(jsonPath("$.averageCompressionRatio").value(2.8))
            .andExpect(jsonPath("$.totalCompressedTokens").value(15000))
            .andExpect(jsonPath("$.estimatedOriginalTokens").value(42000))

        verify { authorizationService.requireAdmin(any()) }
        verify { summaryQueryService.getGlobalStats() }
    }

    @Test
    @WithMockUser
    fun `getSessionSummaryInfo should call authorization check`() {
        val sessionId = UUID.randomUUID()
        val info = SessionSummaryInfoResponse(
            sessionId = sessionId,
            totalMessages = 10,
            summaryLevels = emptyList(),
            lastSummarizedSequence = null,
            estimatedTokenSavings = 0,
            compressionRatio = 1.0
        )

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryInfo(sessionId) } returns info

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/info"))
            .andExpect(status().isOk)

        verify { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
    }

    @Test
    @WithMockUser
    fun `getSessionSummaryDetails should call authorization check`() {
        val sessionId = UUID.randomUUID()

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryDetails(sessionId) } returns emptyList()

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/details"))
            .andExpect(status().isOk)

        verify { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `triggerSummarization should call admin authorization check`() {
        val sessionId = UUID.randomUUID()

        justRun { authorizationService.requireAdmin(any()) }
        justRun { summaryQueryService.triggerManualSummarization(sessionId) }

        mockMvc.perform(post("/api/v1/summaries/sessions/$sessionId/trigger"))
            .andExpect(status().isAccepted)

        verify { authorizationService.requireAdmin(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getGlobalStats should call admin authorization check`() {
        val stats = mapOf("totalSummaries" to 0)

        justRun { authorizationService.requireAdmin(any()) }
        every { summaryQueryService.getGlobalStats() } returns stats

        mockMvc.perform(get("/api/v1/summaries/stats"))
            .andExpect(status().isOk)

        verify { authorizationService.requireAdmin(any()) }
    }

    @Test
    @WithMockUser
    fun `getSessionSummaryInfo should handle empty summaries`() {
        val sessionId = UUID.randomUUID()
        val info = SessionSummaryInfoResponse(
            sessionId = sessionId,
            totalMessages = 0,
            summaryLevels = emptyList(),
            lastSummarizedSequence = null,
            estimatedTokenSavings = 0,
            compressionRatio = 0.0
        )

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryInfo(sessionId) } returns info

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalMessages").value(0))
            .andExpect(jsonPath("$.summaryLevels").isEmpty)
            .andExpect(jsonPath("$.compressionRatio").value(0.0))
    }

    @Test
    @WithMockUser
    fun `getSessionSummaryDetails should handle empty summaries`() {
        val sessionId = UUID.randomUUID()

        justRun { authorizationService.requireSessionAccessOrAdmin(sessionId, any()) }
        every { summaryQueryService.getSessionSummaryDetails(sessionId) } returns emptyList()

        mockMvc.perform(get("/api/v1/summaries/sessions/$sessionId/details"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }
}
