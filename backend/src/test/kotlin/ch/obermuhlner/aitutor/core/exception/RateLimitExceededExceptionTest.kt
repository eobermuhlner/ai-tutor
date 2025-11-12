package ch.obermuhlner.aitutor.core.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import java.time.Duration
import java.time.Instant

class RateLimitExceededExceptionTest {

    @Test
    fun `RateLimitExceededException creates with correct properties`() {
        val userId = "test-user-id"
        val retryAfter = Duration.ofMinutes(10)
        val limitType = "hourly"
        val expectedMessage = "Rate limit exceeded. Please try again in 10 minutes."

        val exception = RateLimitExceededException(
            userId = userId,
            retryAfter = retryAfter,
            limitType = limitType
        )

        assertEquals(userId, exception.userId)
        assertEquals(retryAfter, exception.retryAfter)
        assertEquals(limitType, exception.limitType)
        assertEquals(expectedMessage, exception.message)
        assertEquals(600L, exception.retryAfterSeconds)
        assertTrue(exception.retryAfterTimestamp.isAfter(Instant.now()))
        assertTrue(exception.retryAfterTimestamp.isBefore(Instant.now().plusSeconds(601))) // Within 1 sec tolerance
    }

    @Test
    fun `RateLimitExceededException creates with custom message`() {
        val customMessage = "Custom rate limit exceeded message"

        val exception = RateLimitExceededException(
            userId = "test",
            retryAfter = Duration.ofMinutes(5),
            limitType = "daily",
            message = customMessage
        )

        assertEquals(customMessage, exception.message)
    }
}

class RateLimitExceptionHandlerTest {

    private val handler = RateLimitExceptionHandler()

    @Test
    fun `handleRateLimitExceededException returns proper response entity`() {
        // Given
        val request = MockHttpServletRequest()
        request.requestURI = "/test/endpoint"
        val webRequest = ServletWebRequest(request)

        val exception = RateLimitExceededException(
            userId = "test-user",
            retryAfter = Duration.ofMinutes(10),
            limitType = "hourly"
        )

        // When
        val responseEntity = handler.handleRateLimitExceededException(exception, webRequest)

        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.statusCode)
        assertNotNull(responseEntity.headers)
        assertEquals("600", responseEntity.headers.getFirst("Retry-After"))

        val body = responseEntity.body
        assertNotNull(body)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), body?.status)
        assertEquals("Too Many Requests", body?.error)
        assertTrue(body?.message?.contains("try again in 10 minutes") == true)
        assertEquals("/test/endpoint", body?.path)
        assertEquals(600L, body?.retryAfter)
        assertEquals("hourly", body?.limitType)
        assertNotNull(body?.timestamp)
        assertNotNull(body?.retryAfterTimestamp)
    }

    @Test
    fun `RateLimitErrorResponse creates with correct properties`() {
        val timestamp = Instant.now()
        val status = 429
        val error = "Too Many Requests"
        val message = "Rate limit exceeded"
        val path = "/api/test"
        val retryAfter = 300L
        val retryAfterTimestamp = Instant.now().plusSeconds(300)
        val limitType = "daily"

        val errorResponse = RateLimitErrorResponse(
            timestamp = timestamp,
            status = status,
            error = error,
            message = message,
            path = path,
            retryAfter = retryAfter,
            retryAfterTimestamp = retryAfterTimestamp,
            limitType = limitType
        )

        assertEquals(timestamp, errorResponse.timestamp)
        assertEquals(status, errorResponse.status)
        assertEquals(error, errorResponse.error)
        assertEquals(message, errorResponse.message)
        assertEquals(path, errorResponse.path)
        assertEquals(retryAfter, errorResponse.retryAfter)
        assertEquals(retryAfterTimestamp, errorResponse.retryAfterTimestamp)
        assertEquals(limitType, errorResponse.limitType)
    }
}