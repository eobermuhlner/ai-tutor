package ch.obermuhlner.aitutor.core.exception

import java.time.Instant
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

/**
 * Error response for rate limit exceeded errors.
 * Includes additional rate limit metadata to help clients handle the error.
 */
data class RateLimitErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val retryAfter: Long,  // Seconds until retry is allowed
    val retryAfterTimestamp: Instant,
    val limitType: String  // "hourly" or "daily"
)

/**
 * Global exception handler for rate limiting errors.
 * Returns HTTP 429 (Too Many Requests) with Retry-After header.
 */
@RestControllerAdvice
class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceededException(
        ex: RateLimitExceededException,
        request: WebRequest
    ): ResponseEntity<RateLimitErrorResponse> {
        val errorResponse = RateLimitErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Too Many Requests",
            message = ex.message ?: "Rate limit exceeded",
            path = request.getDescription(false).removePrefix("uri="),
            retryAfter = ex.retryAfterSeconds,
            retryAfterTimestamp = ex.retryAfterTimestamp,
            limitType = ex.limitType
        )

        // Add Retry-After header per HTTP spec
        val headers = HttpHeaders()
        headers.add("Retry-After", ex.retryAfterSeconds.toString())

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .headers(headers)
            .body(errorResponse)
    }
}
