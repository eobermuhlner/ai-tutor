package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.SessionSummaryInfoResponse
import ch.obermuhlner.aitutor.chat.dto.SummaryDetailResponse
import ch.obermuhlner.aitutor.chat.service.SummaryQueryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST endpoints for querying message summarization metadata.
 * Useful for debugging, monitoring, and admin dashboards.
 */
@RestController
@RequestMapping("/api/v1/summaries")
class SummaryController(
    private val summaryQueryService: SummaryQueryService,
    private val authorizationService: AuthorizationService
) {
    /**
     * Get summary statistics for a session.
     * Shows how many summaries exist at each level, token savings, etc.
     *
     * Accessible by: session owner OR admin
     */
    @GetMapping("/sessions/{sessionId}/info")
    fun getSessionSummaryInfo(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<SessionSummaryInfoResponse> {
        authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
        val info = summaryQueryService.getSessionSummaryInfo(sessionId)
        return ResponseEntity.ok(info)
    }

    /**
     * Get detailed view of all summaries for a session (hierarchical tree).
     * Includes summary text, sequence ranges, tokens, etc.
     *
     * Accessible by: session owner OR admin
     */
    @GetMapping("/sessions/{sessionId}/details")
    fun getSessionSummaryDetails(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<List<SummaryDetailResponse>> {
        authorizationService.requireSessionAccessOrAdmin(sessionId, authentication)
        val details = summaryQueryService.getSessionSummaryDetails(sessionId)
        return ResponseEntity.ok(details)
    }

    /**
     * Trigger manual summarization for a session (admin only).
     * Useful for backfilling or re-summarizing with updated prompts.
     *
     * Accessible by: admin only
     */
    @PostMapping("/sessions/{sessionId}/trigger")
    fun triggerSummarization(
        @PathVariable sessionId: UUID,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        authorizationService.requireAdmin(authentication)
        summaryQueryService.triggerManualSummarization(sessionId)
        return ResponseEntity.accepted().body(mapOf(
            "status" to "accepted",
            "message" to "Summarization triggered asynchronously for session $sessionId"
        ))
    }

    /**
     * Get global summarization statistics (all sessions).
     * Shows total summaries, average compression ratio, etc.
     *
     * Accessible by: admin only
     */
    @GetMapping("/stats")
    fun getGlobalStats(
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireAdmin(authentication)
        val stats = summaryQueryService.getGlobalStats()
        return ResponseEntity.ok(stats)
    }
}
