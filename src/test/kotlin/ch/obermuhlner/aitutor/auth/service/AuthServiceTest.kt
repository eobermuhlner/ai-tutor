package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.JwtProperties
import ch.obermuhlner.aitutor.auth.dto.ChangePasswordRequest
import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.exception.AccountDisabledException
import ch.obermuhlner.aitutor.auth.exception.AccountLockedException
import ch.obermuhlner.aitutor.auth.exception.DuplicateEmailException
import ch.obermuhlner.aitutor.auth.exception.DuplicateUsernameException
import ch.obermuhlner.aitutor.auth.exception.InvalidCredentialsException
import ch.obermuhlner.aitutor.auth.exception.WeakPasswordException
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.RefreshTokenEntity
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.service.UserService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var userService: UserService
    private lateinit var jwtTokenService: JwtTokenService
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtProperties: JwtProperties

    @BeforeEach
    fun setUp() {
        userService = mockk()
        jwtTokenService = mockk()
        refreshTokenRepository = mockk()
        passwordEncoder = mockk()
        jwtProperties = JwtProperties(
            secret = "test-secret",
            expirationMs = 3600000,
            refreshExpirationMs = 2592000000
        )

        authService = AuthService(
            userService,
            jwtTokenService,
            refreshTokenRepository,
            passwordEncoder,
            jwtProperties
        )
    }

    @Test
    fun `register should create new user successfully`() {
        val request = RegisterRequest(
            username = "newuser",
            email = "new@example.com",
            password = "Password123",
            firstName = "New",
            lastName = "User"
        )

        every { userService.existsByUsername("newuser") } returns false
        every { userService.existsByEmail("new@example.com") } returns false
        every { userService.createUser(any()) } answers {
            val user = firstArg<UserEntity>()
            UserEntity(
                id = UUID.randomUUID(),
                username = user.username,
                email = user.email,
                passwordHash = user.passwordHash,
                firstName = user.firstName,
                lastName = user.lastName,
                roles = user.roles,
                provider = user.provider
            )
        }

        val result = authService.register(request)

        assertEquals("newuser", result.username)
        assertEquals("new@example.com", result.email)
        assertTrue(result.roles.contains(UserRole.USER))
        verify { userService.createUser(any()) }
    }

    @Test
    fun `register should throw exception for duplicate username`() {
        val request = RegisterRequest(
            username = "existinguser",
            email = "new@example.com",
            password = "Password123"
        )

        every { userService.existsByUsername("existinguser") } returns true

        assertThrows<DuplicateUsernameException> {
            authService.register(request)
        }
    }

    @Test
    fun `register should throw exception for duplicate email`() {
        val request = RegisterRequest(
            username = "newuser",
            email = "existing@example.com",
            password = "Password123"
        )

        every { userService.existsByUsername("newuser") } returns false
        every { userService.existsByEmail("existing@example.com") } returns true

        assertThrows<DuplicateEmailException> {
            authService.register(request)
        }
    }

    @Test
    fun `register should throw exception for weak password`() {
        val request = RegisterRequest(
            username = "newuser",
            email = "new@example.com",
            password = "weak"  // Too short, no uppercase, no digit
        )

        every { userService.existsByUsername("newuser") } returns false
        every { userService.existsByEmail("new@example.com") } returns false

        assertThrows<WeakPasswordException> {
            authService.register(request)
        }
    }

    @Test
    fun `login should return token for valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "Password123")
        val user = UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            locked = false,
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findByUsername("testuser") } returns user
        every { passwordEncoder.matches("Password123", "hashedpassword") } returns true
        every { jwtTokenService.generateAccessToken(user) } returns "access-token"
        every { jwtTokenService.generateRefreshToken(user) } returns "refresh-token"
        every { jwtTokenService.getExpirationFromToken("refresh-token") } returns Instant.now().plusSeconds(2592000)
        every { refreshTokenRepository.save(any()) } returns mockk()
        every { userService.updateUser(any()) } returns user

        val result = authService.login(request)

        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals("Bearer", result.tokenType)
        assertEquals("testuser", result.user.username)
    }

    @Test
    fun `login should throw exception for invalid credentials`() {
        val request = LoginRequest(username = "testuser", password = "wrongpassword")
        val user = UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findByUsername("testuser") } returns user
        every { passwordEncoder.matches("wrongpassword", "hashedpassword") } returns false

        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should throw exception for disabled account`() {
        val request = LoginRequest(username = "testuser", password = "Password123")
        val user = UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            roles = mutableSetOf(UserRole.USER),
            enabled = false,
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findByUsername("testuser") } returns user

        assertThrows<AccountDisabledException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should throw exception for locked account`() {
        val request = LoginRequest(username = "testuser", password = "Password123")
        val user = UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            locked = true,
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findByUsername("testuser") } returns user

        assertThrows<AccountLockedException> {
            authService.login(request)
        }
    }

    @Test
    fun `changePassword should update password successfully`() {
        val userId = UUID.randomUUID()
        val request = ChangePasswordRequest(
            currentPassword = "OldPassword123",
            newPassword = "NewPassword123"
        )
        val user = UserEntity(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "oldhash",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findById(userId) } returns user
        every { passwordEncoder.matches("OldPassword123", "oldhash") } returns true
        every { userService.updatePassword(userId, "NewPassword123") } just Runs
        every { refreshTokenRepository.findAllByUserId(userId) } returns emptyList()
        every { refreshTokenRepository.saveAll(any<List<RefreshTokenEntity>>()) } returns emptyList()

        authService.changePassword(userId, request)

        verify { userService.updatePassword(userId, "NewPassword123") }
    }

    @Test
    fun `changePassword should throw exception for wrong current password`() {
        val userId = UUID.randomUUID()
        val request = ChangePasswordRequest(
            currentPassword = "WrongPassword",
            newPassword = "NewPassword123"
        )
        val user = UserEntity(
            id = userId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "oldhash",
            roles = mutableSetOf(UserRole.USER),
            provider = AuthProvider.CREDENTIALS
        )

        every { userService.findById(userId) } returns user
        every { passwordEncoder.matches("WrongPassword", "oldhash") } returns false

        assertThrows<InvalidCredentialsException> {
            authService.changePassword(userId, request)
        }
    }

    @Test
    fun `logout should revoke all refresh tokens`() {
        val userId = UUID.randomUUID()
        val token1 = RefreshTokenEntity(
            userId = userId,
            token = "token1",
            expiresAt = Instant.now().plusSeconds(3600)
        )
        val token2 = RefreshTokenEntity(
            userId = userId,
            token = "token2",
            expiresAt = Instant.now().plusSeconds(3600)
        )

        every { refreshTokenRepository.findAllByUserId(userId) } returns listOf(token1, token2)
        every { refreshTokenRepository.saveAll(any<List<RefreshTokenEntity>>()) } returns listOf(token1, token2)

        authService.logout(userId)

        assertTrue(token1.revoked)
        assertTrue(token2.revoked)
        verify { refreshTokenRepository.saveAll(any<List<RefreshTokenEntity>>()) }
    }
}
