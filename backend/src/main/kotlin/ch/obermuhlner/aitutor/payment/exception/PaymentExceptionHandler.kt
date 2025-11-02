package ch.obermuhlner.aitutor.payment.exception

import ch.obermuhlner.aitutor.auth.exception.ErrorResponse
import com.stripe.exception.*
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class PaymentExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(
        ex: InvalidRequestException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe invalid request: code=${ex.code}, requestId=${ex.requestId}", ex)

        val userMessage = when (ex.code) {
            "email_invalid" -> "Please update your email address to a valid format in your profile settings before upgrading."
            "parameter_invalid_empty" -> "Required information is missing. Please contact support."
            "parameter_invalid_integer" -> "Invalid value provided. Please try again."
            else -> "Invalid request: ${ex.message ?: "Please check your information and try again."}"
        }

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = userMessage,
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(CardException::class)
    fun handleCardException(
        ex: CardException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe card error: code=${ex.code}, declineCode=${ex.declineCode}, requestId=${ex.requestId}", ex)

        val userMessage = when (ex.code) {
            "card_declined" -> "Your card was declined. Please try a different payment method."
            "expired_card" -> "Your card has expired. Please use a different card."
            "incorrect_cvc" -> "The card security code is incorrect. Please check and try again."
            "processing_error" -> "An error occurred processing your card. Please try again."
            "incorrect_number" -> "The card number is incorrect. Please check and try again."
            else -> "Card error: ${ex.message ?: "Please try a different payment method."}"
        }

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.PAYMENT_REQUIRED.value(),
            error = "Payment Required",
            message = userMessage,
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorResponse)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe authentication error: requestId=${ex.requestId}", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "Payment service configuration error. Please contact support.",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(
        ex: RateLimitException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe rate limit exceeded: requestId=${ex.requestId}", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            error = "Too Many Requests",
            message = "Too many payment requests. Please wait a moment and try again.",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
    }

    @ExceptionHandler(ApiConnectionException::class)
    fun handleApiConnectionException(
        ex: ApiConnectionException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe API connection error", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Service Unavailable",
            message = "Payment service temporarily unavailable. Please try again in a few moments.",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }

    @ExceptionHandler(ApiException::class, StripeException::class)
    fun handleStripeException(
        ex: StripeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Stripe API error: requestId=${ex.requestId}", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An error occurred processing your payment. Please try again or contact support.",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
