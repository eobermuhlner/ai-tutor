package ch.obermuhlner.aitutor.auth.controller

import ch.obermuhlner.aitutor.auth.dto.*
import ch.obermuhlner.aitutor.auth.service.AuthService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and management")
class AuthController(
    private val authService: AuthService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates a user and returns access/refresh tokens")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val loginResponse = authService.login(request)
        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses a refresh token to get a new access token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<LoginResponse> {
        val loginResponse = authService.refreshAccessToken(request)
        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the user's refresh token")
    fun logout(): ResponseEntity<Void> {
        val userId = authorizationService.getCurrentUserId()
        authService.logout(userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieves information about the currently authenticated user")
    fun getCurrentUser(): ResponseEntity<UserResponse> {
        val user = authorizationService.getCurrentUser()

        val response = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            enabled = user.enabled,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan,
            pronunciationPreference = user.pronunciationPreference
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/password")
    @Operation(summary = "Change user password", description = "Allows authenticated user to change their password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        val userId = authorizationService.getCurrentUserId()
        authService.changePassword(userId, request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/email")
    @Operation(summary = "Change user email", description = "Allows authenticated user to change their email address")
    fun changeEmail(
        @RequestBody request: ChangeEmailRequest
    ): ResponseEntity<UserResponse> {
        val userId = authorizationService.getCurrentUserId()
        authService.changeEmail(userId, request)

        // Return updated user info
        val user = authorizationService.getCurrentUser()
        val response = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roles = user.roles,
            enabled = user.enabled,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan,
            pronunciationPreference = user.pronunciationPreference
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/profile")
    @Operation(summary = "Update user profile", description = "Allows authenticated user to update their profile information (firstName, lastName)")
    fun updateProfile(
        @RequestBody request: ch.obermuhlner.aitutor.auth.dto.UpdateUserProfileRequest
    ): ResponseEntity<UserResponse> {
        val userId = authorizationService.getCurrentUserId()
        val response = authService.updateProfile(userId, request.firstName, request.lastName)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/pronunciation-preference")
    @Operation(summary = "Update pronunciation preference", description = "Allows authenticated user to update their pronunciation guide preference")
    fun updatePronunciationPreference(
        @RequestBody request: ch.obermuhlner.aitutor.auth.dto.UpdatePronunciationPreferenceRequest
    ): ResponseEntity<UserResponse> {
        val userId = authorizationService.getCurrentUserId()
        val response = authService.updatePronunciationPreference(userId, request.pronunciationPreference)
        return ResponseEntity.ok(response)
    }
}
