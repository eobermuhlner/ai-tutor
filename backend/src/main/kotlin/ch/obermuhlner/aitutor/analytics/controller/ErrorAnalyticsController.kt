package ch.obermuhlner.aitutor.analytics.controller

import ch.obermuhlner.aitutor.analytics.dto.ErrorPatternResponse
import ch.obermuhlner.aitutor.analytics.dto.ErrorSampleResponse
import ch.obermuhlner.aitutor.analytics.dto.ErrorTrendResponse
import ch.obermuhlner.aitutor.analytics.dto.toResponse
import ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.core.model.ErrorType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/analytics")
class ErrorAnalyticsController(
    private val errorAnalyticsService: ErrorAnalyticsService,
    private val authorizationService: AuthorizationService
) {

    /**
     * Get top error patterns for user.
     * GET /api/v1/analytics/errors/patterns?lang=es&limit=5
     */
    @GetMapping("/errors/patterns")
    fun getErrorPatterns(
        @RequestParam lang: String,
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<List<ErrorPatternResponse>> {
        val userId = authorizationService.getCurrentUserId()

        return ResponseEntity.ok(
            errorAnalyticsService.getTopPatterns(userId, lang, limit)
                .map { it.toResponse() }
        )
    }

    /**
     * Get trend for specific error type.
     * GET /api/v1/analytics/errors/trends/{errorType}?lang=es
     */
    @GetMapping("/errors/trends/{errorType}")
    fun getErrorTrend(
        @PathVariable errorType: String,
        @RequestParam lang: String
    ): ResponseEntity<ErrorTrendResponse> {
        val userId = authorizationService.getCurrentUserId()
        val errorTypeEnum = ErrorType.valueOf(errorType)

        val trend = errorAnalyticsService.computeTrend(userId, errorTypeEnum)

        return ResponseEntity.ok(
            ErrorTrendResponse(
                errorType = errorType,
                trend = trend
            )
        )
    }

    /**
     * Get recent error samples for debugging/UI.
     * GET /api/v1/analytics/errors/samples?limit=20
     */
    @GetMapping("/errors/samples")
    fun getRecentSamples(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<ErrorSampleResponse>> {
        val userId = authorizationService.getCurrentUserId()

        return ResponseEntity.ok(
            errorAnalyticsService.getRecentSamples(userId, limit)
                .map { it.toResponse() }
        )
    }
}
