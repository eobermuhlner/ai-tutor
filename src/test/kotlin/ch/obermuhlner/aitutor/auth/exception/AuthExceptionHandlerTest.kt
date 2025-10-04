package ch.obermuhlner.aitutor.auth.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest

class AuthExceptionHandlerTest {

    private lateinit var handler: AuthExceptionHandler
    private lateinit var request: WebRequest

    @BeforeEach
    fun setUp() {
        handler = AuthExceptionHandler()
        request = mock(WebRequest::class.java)
        `when`(request.getDescription(false)).thenReturn("uri=/test/path")
    }

    @Test
    fun `handleUserNotFoundException should return 404 NOT_FOUND`() {
        val exception = UserNotFoundException("User not found")

        val response = handler.handleUserNotFoundException(exception, request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("Not Found", response.body?.error)
        assertEquals("User not found", response.body?.message)
        assertEquals("/test/path", response.body?.path)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `handleUserNotFoundException with null message should use default`() {
        val exception = UserNotFoundException(null as String? ?: "User not found")

        val response = handler.handleUserNotFoundException(exception, request)

        assertEquals("User not found", response.body?.message)
    }

    @Test
    fun `handleDuplicateException with DuplicateUsernameException should return 409 CONFLICT`() {
        val exception = DuplicateUsernameException("Username already exists")

        val response = handler.handleDuplicateException(exception, request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body?.status)
        assertEquals("Conflict", response.body?.error)
        assertEquals("Username already exists", response.body?.message)
        assertEquals("/test/path", response.body?.path)
    }

    @Test
    fun `handleDuplicateException with DuplicateEmailException should return 409 CONFLICT`() {
        val exception = DuplicateEmailException("Email already exists")

        val response = handler.handleDuplicateException(exception, request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals(409, response.body?.status)
        assertEquals("Conflict", response.body?.error)
        assertEquals("Email already exists", response.body?.message)
    }

    @Test
    fun `handleUnauthorizedException with InvalidCredentialsException should return 401 UNAUTHORIZED`() {
        val exception = InvalidCredentialsException()

        val response = handler.handleUnauthorizedException(exception, request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(401, response.body?.status)
        assertEquals("Unauthorized", response.body?.error)
        assertEquals("Invalid username or password", response.body?.message)
        assertEquals("/test/path", response.body?.path)
    }

    @Test
    fun `handleUnauthorizedException with InvalidTokenException should return 401 UNAUTHORIZED`() {
        val exception = InvalidTokenException()

        val response = handler.handleUnauthorizedException(exception, request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(401, response.body?.status)
        assertEquals("Unauthorized", response.body?.error)
        assertEquals("Invalid or malformed token", response.body?.message)
    }

    @Test
    fun `handleUnauthorizedException with ExpiredTokenException should return 401 UNAUTHORIZED`() {
        val exception = ExpiredTokenException()

        val response = handler.handleUnauthorizedException(exception, request)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(401, response.body?.status)
        assertEquals("Unauthorized", response.body?.error)
        assertEquals("Token has expired", response.body?.message)
    }

    @Test
    fun `handleForbiddenException should return 403 FORBIDDEN`() {
        val exception = InsufficientPermissionsException()

        val response = handler.handleForbiddenException(exception, request)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(403, response.body?.status)
        assertEquals("Forbidden", response.body?.error)
        assertEquals("Insufficient permissions to perform this action", response.body?.message)
        assertEquals("/test/path", response.body?.path)
    }

    @Test
    fun `handleForbiddenException with custom message`() {
        val exception = InsufficientPermissionsException("Admin role required")

        val response = handler.handleForbiddenException(exception, request)

        assertEquals("Admin role required", response.body?.message)
    }

    @Test
    fun `handleBadRequestException with WeakPasswordException should return 400 BAD_REQUEST`() {
        val exception = WeakPasswordException("Password too weak")

        val response = handler.handleBadRequestException(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Password too weak", response.body?.message)
        assertEquals("/test/path", response.body?.path)
    }

    @Test
    fun `handleBadRequestException with AccountDisabledException should return 400 BAD_REQUEST`() {
        val exception = AccountDisabledException()

        val response = handler.handleBadRequestException(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Account is disabled", response.body?.message)
    }

    @Test
    fun `handleBadRequestException with AccountLockedException should return 400 BAD_REQUEST`() {
        val exception = AccountLockedException()

        val response = handler.handleBadRequestException(exception, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Account is locked", response.body?.message)
    }

    @Test
    fun `ErrorResponse should be created correctly`() {
        val now = java.time.Instant.now()
        val errorResponse = ErrorResponse(
            timestamp = now,
            status = 404,
            error = "Not Found",
            message = "Resource not found",
            path = "/api/test"
        )

        assertEquals(now, errorResponse.timestamp)
        assertEquals(404, errorResponse.status)
        assertEquals("Not Found", errorResponse.error)
        assertEquals("Resource not found", errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
    }
}
