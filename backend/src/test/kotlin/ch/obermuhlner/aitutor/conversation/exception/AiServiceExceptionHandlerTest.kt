package ch.obermuhlner.aitutor.conversation.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest

class AiServiceExceptionHandlerTest {

    private lateinit var handler: AiServiceExceptionHandler
    private lateinit var request: WebRequest

    @BeforeEach
    fun setUp() {
        handler = AiServiceExceptionHandler()
        request = mock(WebRequest::class.java)
        `when`(request.getDescription(false)).thenReturn("uri=/api/v1/chat/sessions/123/messages")
    }

    @Test
    fun `handleNonTransientAiException with quota error should return 503 SERVICE_UNAVAILABLE`() {
        val exception = NonTransientAiException(
            """HTTP 429 - {
                "error": {
                    "message": "You exceeded your current quota, please check your plan and billing details.",
                    "type": "insufficient_quota",
                    "param": null,
                    "code": "insufficient_quota"
                }
            }"""
        )

        val response = handler.handleNonTransientAiException(exception, request)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(503, response.body?.status)
        assertEquals("Service Unavailable", response.body?.error)
        assertEquals("AI service is temporarily unavailable due to quota limits", response.body?.message)
        assertEquals("The AI service has exceeded its current usage quota. Please try again later.", response.body?.details)
        assertEquals("/api/v1/chat/sessions/123/messages", response.body?.path)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `handleNonTransientAiException with 429 in message should return 503 SERVICE_UNAVAILABLE`() {
        val exception = NonTransientAiException("HTTP 429 - Rate limit exceeded")

        val response = handler.handleNonTransientAiException(exception, request)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(503, response.body?.status)
        assertEquals("Service Unavailable", response.body?.error)
        assertEquals("AI service is temporarily unavailable due to quota limits", response.body?.message)
        assertEquals("The AI service has exceeded its current usage quota. Please try again later.", response.body?.details)
    }

    @Test
    fun `handleNonTransientAiException with rate_limit should return 503 SERVICE_UNAVAILABLE`() {
        val exception = NonTransientAiException("rate_limit exceeded: too many requests")

        val response = handler.handleNonTransientAiException(exception, request)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(503, response.body?.status)
        assertEquals("Service Unavailable", response.body?.error)
        assertEquals("AI service is temporarily unavailable due to rate limiting", response.body?.message)
        assertEquals("The AI service is currently rate limited. Please try again in a moment.", response.body?.details)
        assertEquals("/api/v1/chat/sessions/123/messages", response.body?.path)
    }

    @Test
    fun `handleNonTransientAiException with generic error should return 503 SERVICE_UNAVAILABLE`() {
        val exception = NonTransientAiException("Unknown AI service error")

        val response = handler.handleNonTransientAiException(exception, request)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(503, response.body?.status)
        assertEquals("Service Unavailable", response.body?.error)
        assertEquals("AI service is temporarily unavailable", response.body?.message)
        assertEquals("The AI service encountered an error. Please try again later.", response.body?.details)
        assertEquals("/api/v1/chat/sessions/123/messages", response.body?.path)
    }

    @Test
    fun `handleNonTransientAiException should handle case-insensitive matching`() {
        val exception = NonTransientAiException("ERROR: INSUFFICIENT_QUOTA available")

        val response = handler.handleNonTransientAiException(exception, request)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("AI service is temporarily unavailable due to quota limits", response.body?.message)
    }

    @Test
    fun `AiErrorResponse should be created correctly with all fields`() {
        val now = java.time.Instant.now()
        val errorResponse = AiErrorResponse(
            timestamp = now,
            status = 503,
            error = "Service Unavailable",
            message = "AI service is temporarily unavailable",
            path = "/api/v1/chat/sessions/123/messages",
            details = "The AI service encountered an error. Please try again later."
        )

        assertEquals(now, errorResponse.timestamp)
        assertEquals(503, errorResponse.status)
        assertEquals("Service Unavailable", errorResponse.error)
        assertEquals("AI service is temporarily unavailable", errorResponse.message)
        assertEquals("/api/v1/chat/sessions/123/messages", errorResponse.path)
        assertEquals("The AI service encountered an error. Please try again later.", errorResponse.details)
    }

    @Test
    fun `AiErrorResponse should allow null details`() {
        val errorResponse = AiErrorResponse(
            timestamp = java.time.Instant.now(),
            status = 503,
            error = "Service Unavailable",
            message = "AI service is temporarily unavailable",
            path = "/api/test"
        )

        assertEquals(null, errorResponse.details)
    }
}
