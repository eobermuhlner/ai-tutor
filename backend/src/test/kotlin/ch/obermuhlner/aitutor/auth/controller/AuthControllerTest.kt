package ch.obermuhlner.aitutor.auth.controller

import ch.obermuhlner.aitutor.auth.dto.ChangePasswordRequest
import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.LoginResponse
import ch.obermuhlner.aitutor.auth.dto.RefreshTokenRequest
import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.auth.service.AuthService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(
    ch.obermuhlner.aitutor.auth.config.SecurityConfig::class,
    ch.obermuhlner.aitutor.auth.service.JwtTokenService::class,
    ch.obermuhlner.aitutor.user.service.CustomUserDetailsService::class
)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var authorizationService: AuthorizationService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: ch.obermuhlner.aitutor.auth.service.JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var customUserDetailsService: ch.obermuhlner.aitutor.user.service.CustomUserDetailsService

    @MockkBean(relaxed = true)
    private lateinit var authenticationManager: AuthenticationManager

    @Test
    fun `POST register should create new user and return 201`() {
        val request = RegisterRequest(
            username = "newuser",
            email = "new@example.com",
            password = "Password123",
            firstName = "New",
            lastName = "User"
        )

        val response = UserResponse(
            id = UUID.randomUUID(),
            username = "newuser",
            email = "new@example.com",
            firstName = "New",
            lastName = "User",
            roles = setOf(UserRole.USER),
            enabled = true,
            locked = false,
            emailVerified = false,
            createdAt = Instant.now(),
            lastLoginAt = null,
            subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE,
            pronunciationPreference = ch.obermuhlner.aitutor.user.domain.PronunciationPreference.NONE,
            provider = AuthProvider.CREDENTIALS
        )

        every { authService.register(request) } returns response

        mockMvc.perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.email").value("new@example.com"))

        verify { authService.register(request) }
    }

    @Test
    fun `POST login should return token for valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "Password123")

        val response = LoginResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            tokenType = "Bearer",
            expiresIn = 3600,
            user = UserResponse(
                id = UUID.randomUUID(),
                username = "testuser",
                email = "test@example.com",
                firstName = null,
                lastName = null,
                roles = setOf(UserRole.USER),
                enabled = true,
                locked = false,
                emailVerified = false,
                createdAt = Instant.now(),
                lastLoginAt = Instant.now(),
                subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE,
                pronunciationPreference = ch.obermuhlner.aitutor.user.domain.PronunciationPreference.NONE,
                provider = AuthProvider.CREDENTIALS
            )
        )

        every { authService.login(request) } returns response

        mockMvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("testuser"))

        verify { authService.login(request) }
    }

    @Test
    fun `POST refresh should return new token for valid refresh token`() {
        val request = RefreshTokenRequest(refreshToken = "valid-refresh-token")

        val response = LoginResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            tokenType = "Bearer",
            expiresIn = 3600,
            user = UserResponse(
                id = UUID.randomUUID(),
                username = "testuser",
                email = "test@example.com",
                firstName = null,
                lastName = null,
                roles = setOf(UserRole.USER),
                enabled = true,
                locked = false,
                emailVerified = false,
                createdAt = Instant.now(),
                lastLoginAt = null,
                subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE,
                pronunciationPreference = ch.obermuhlner.aitutor.user.domain.PronunciationPreference.NONE,
                provider = AuthProvider.CREDENTIALS
            )
        )

        every { authService.refreshAccessToken(request) } returns response

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))

        verify { authService.refreshAccessToken(request) }
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `GET me should return current user profile`() {
        val user = ch.obermuhlner.aitutor.user.domain.UserEntity(
            id = UUID.randomUUID(),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            emailVerified = false,
            createdAt = Instant.now(),
            lastLoginAt = null,
            subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE,
            provider = ch.obermuhlner.aitutor.user.domain.AuthProvider.CREDENTIALS
        )

        every { authorizationService.getCurrentUser() } returns user

        mockMvc.perform(
            get("/api/v1/auth/me")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.provider").value("CREDENTIALS"))

        verify { authorizationService.getCurrentUser() }
    }

    @Test
    @WithMockUser(username = "googleuser")
    fun `GET me should return Google OAuth provider for Google users`() {
        val user = ch.obermuhlner.aitutor.user.domain.UserEntity(
            id = UUID.randomUUID(),
            username = "googleuser",
            email = "google@example.com",
            firstName = "Google",
            lastName = "User",
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            emailVerified = true,
            createdAt = Instant.now(),
            lastLoginAt = Instant.now(),
            subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE,
            provider = ch.obermuhlner.aitutor.user.domain.AuthProvider.GOOGLE
        )

        every { authorizationService.getCurrentUser() } returns user

        mockMvc.perform(
            get("/api/v1/auth/me")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("googleuser"))
            .andExpect(jsonPath("$.email").value("google@example.com"))
            .andExpect(jsonPath("$.provider").value("GOOGLE"))

        verify { authorizationService.getCurrentUser() }
    }

    @Test
    fun `GET me without authentication should return 403`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isForbidden)  // Spring Security returns 403 for missing auth
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `POST logout should revoke tokens and return 204`() {
        val userId = UUID.randomUUID()

        every { authorizationService.getCurrentUserId() } returns userId
        every { authService.logout(userId) } returns Unit

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify { authorizationService.getCurrentUserId() }
        verify { authService.logout(userId) }
    }

    @Test
    @WithMockUser(username = "testuser")
    fun `POST password should change password and return 204`() {
        val userId = UUID.randomUUID()
        val request = ChangePasswordRequest(
            currentPassword = "OldPassword123",
            newPassword = "NewPassword123"
        )

        every { authorizationService.getCurrentUserId() } returns userId
        every { authService.changePassword(userId, request) } returns Unit

        mockMvc.perform(
            post("/api/v1/auth/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNoContent)

        verify { authorizationService.getCurrentUserId() }
        verify { authService.changePassword(userId, request) }
    }
}
