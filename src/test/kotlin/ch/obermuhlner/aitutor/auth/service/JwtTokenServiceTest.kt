package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.JwtProperties
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JwtTokenServiceTest {

    private lateinit var jwtTokenService: JwtTokenService
    private lateinit var jwtProperties: JwtProperties
    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties(
            secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm",
            expirationMs = 3600000,  // 1 hour
            refreshExpirationMs = 2592000000  // 30 days
        )
        jwtTokenService = JwtTokenService(jwtProperties)

        testUser = UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )
    }

    @Test
    fun `generateAccessToken should create valid JWT token`() {
        val token = jwtTokenService.generateAccessToken(testUser)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertTrue(token.split(".").size == 3)  // JWT has 3 parts
    }

    @Test
    fun `generateRefreshToken should create valid JWT token`() {
        val token = jwtTokenService.generateRefreshToken(testUser)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertTrue(token.split(".").size == 3)
    }

    @Test
    fun `validateToken should return true for valid token`() {
        val token = jwtTokenService.generateAccessToken(testUser)

        val isValid = jwtTokenService.validateToken(token)

        assertTrue(isValid)
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        val invalidToken = "invalid.jwt.token"

        val isValid = jwtTokenService.validateToken(invalidToken)

        assertFalse(isValid)
    }

    @Test
    fun `getUserIdFromToken should extract correct user ID`() {
        val token = jwtTokenService.generateAccessToken(testUser)

        val userId = jwtTokenService.getUserIdFromToken(token)

        assertEquals(testUser.id, userId)
    }

    @Test
    fun `getUsernameFromToken should extract correct username`() {
        val token = jwtTokenService.generateAccessToken(testUser)

        val username = jwtTokenService.getUsernameFromToken(token)

        assertEquals("testuser", username)
    }

    @Test
    fun `getRolesFromToken should extract correct roles`() {
        testUser.roles.add(UserRole.ADMIN)
        val token = jwtTokenService.generateAccessToken(testUser)

        val roles = jwtTokenService.getRolesFromToken(token)

        assertEquals(2, roles.size)
        assertTrue(roles.contains(UserRole.USER))
        assertTrue(roles.contains(UserRole.ADMIN))
    }

    @Test
    fun `getExpirationFromToken should return future date`() {
        val token = jwtTokenService.generateAccessToken(testUser)

        val expiration = jwtTokenService.getExpirationFromToken(token)

        assertTrue(expiration.isAfter(Instant.now()))
    }

    @Test
    fun `access token expiration should be approximately 1 hour`() {
        val token = jwtTokenService.generateAccessToken(testUser)
        val expiration = jwtTokenService.getExpirationFromToken(token)

        val now = Instant.now()
        val diff = expiration.toEpochMilli() - now.toEpochMilli()

        // Should be approximately 1 hour (within 1 second tolerance)
        assertTrue(diff > jwtProperties.expirationMs - 1000)
        assertTrue(diff <= jwtProperties.expirationMs + 1000)
    }

    @Test
    fun `refresh token expiration should be approximately 30 days`() {
        val token = jwtTokenService.generateRefreshToken(testUser)
        val expiration = jwtTokenService.getExpirationFromToken(token)

        val now = Instant.now()
        val diff = expiration.toEpochMilli() - now.toEpochMilli()

        // Should be approximately 30 days (within 1 second tolerance)
        assertTrue(diff > jwtProperties.refreshExpirationMs - 1000)
        assertTrue(diff <= jwtProperties.refreshExpirationMs + 1000)
    }
}
