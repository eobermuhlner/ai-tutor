package ch.obermuhlner.aitutor.conversation.exception

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

data class AiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: String? = null
)

@RestControllerAdvice
class AiServiceExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NonTransientAiException::class)
    fun handleNonTransientAiException(
        ex: NonTransientAiException,
        request: WebRequest
    ): ResponseEntity<AiErrorResponse> {
        logger.error("AI service error: ${ex.message}", ex)

        val status: HttpStatus
        val userMessage: String
        val details: String?

        // Check if this is a quota/rate limit error
        when {
            ex.message?.contains("insufficient_quota", ignoreCase = true) == true ||
            ex.message?.contains("429", ignoreCase = true) == true -> {
                status = HttpStatus.SERVICE_UNAVAILABLE
                userMessage = "AI service is temporarily unavailable due to quota limits"
                details = "The AI service has exceeded its current usage quota. Please try again later."
            }
            ex.message?.contains("rate_limit", ignoreCase = true) == true -> {
                status = HttpStatus.SERVICE_UNAVAILABLE
                userMessage = "AI service is temporarily unavailable due to rate limiting"
                details = "The AI service is currently rate limited. Please try again in a moment."
            }
            else -> {
                // Generic AI service error
                status = HttpStatus.SERVICE_UNAVAILABLE
                userMessage = "AI service is temporarily unavailable"
                details = "The AI service encountered an error. Please try again later."
            }
        }

        val errorResponse = AiErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = userMessage,
            path = request.getDescription(false).removePrefix("uri="),
            details = details
        )

        return ResponseEntity.status(status).body(errorResponse)
    }
}
