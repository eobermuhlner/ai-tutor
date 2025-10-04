package ch.obermuhlner.aitutor.auth.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthExceptionsTest {

    @Test
    fun `UserNotFoundException should have custom message`() {
        val message = "User with id 123 not found"
        val exception = UserNotFoundException(message)

        assertEquals(message, exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `DuplicateUsernameException should have custom message`() {
        val message = "Username already exists"
        val exception = DuplicateUsernameException(message)

        assertEquals(message, exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `DuplicateEmailException should have custom message`() {
        val message = "Email already exists"
        val exception = DuplicateEmailException(message)

        assertEquals(message, exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `InvalidCredentialsException should have default message`() {
        val exception = InvalidCredentialsException()

        assertEquals("Invalid username or password", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `InvalidCredentialsException should accept custom message`() {
        val message = "Custom invalid credentials message"
        val exception = InvalidCredentialsException(message)

        assertEquals(message, exception.message)
    }

    @Test
    fun `InvalidTokenException should have default message`() {
        val exception = InvalidTokenException()

        assertEquals("Invalid or malformed token", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `InvalidTokenException should accept custom message`() {
        val message = "Token signature invalid"
        val exception = InvalidTokenException(message)

        assertEquals(message, exception.message)
    }

    @Test
    fun `ExpiredTokenException should have default message`() {
        val exception = ExpiredTokenException()

        assertEquals("Token has expired", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `ExpiredTokenException should accept custom message`() {
        val message = "Access token expired at 2025-01-01"
        val exception = ExpiredTokenException(message)

        assertEquals(message, exception.message)
    }

    @Test
    fun `InsufficientPermissionsException should have default message`() {
        val exception = InsufficientPermissionsException()

        assertEquals("Insufficient permissions to perform this action", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `InsufficientPermissionsException should accept custom message`() {
        val message = "Admin role required"
        val exception = InsufficientPermissionsException(message)

        assertEquals(message, exception.message)
    }

    @Test
    fun `WeakPasswordException should have custom message`() {
        val message = "Password must be at least 8 characters"
        val exception = WeakPasswordException(message)

        assertEquals(message, exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `AccountDisabledException should have default message`() {
        val exception = AccountDisabledException()

        assertEquals("Account is disabled", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `AccountDisabledException should accept custom message`() {
        val message = "Account disabled due to violation"
        val exception = AccountDisabledException(message)

        assertEquals(message, exception.message)
    }

    @Test
    fun `AccountLockedException should have default message`() {
        val exception = AccountLockedException()

        assertEquals("Account is locked", exception.message)
        assertTrue(exception is RuntimeException)
    }

    @Test
    fun `AccountLockedException should accept custom message`() {
        val message = "Account locked after 5 failed attempts"
        val exception = AccountLockedException(message)

        assertEquals(message, exception.message)
    }
}
