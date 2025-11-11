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
            locked = user.locked,
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
            locked = user.locked,
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

    @PostMapping("/verify-email/send")
    @Operation(summary = "Send email verification", description = "Sends a verification email to the authenticated user's email address")
    fun sendVerificationEmail(): ResponseEntity<Map<String, String>> {
        val userId = authorizationService.getCurrentUserId()
        authService.sendVerificationEmail(userId)
        return ResponseEntity.ok(mapOf("message" to "Verification email sent"))
    }

    @PostMapping("/verify-email/resend")
    @Operation(summary = "Resend email verification", description = "Resends a verification email (invalidates previous tokens)")
    fun resendVerificationEmail(): ResponseEntity<Map<String, String>> {
        val userId = authorizationService.getCurrentUserId()
        authService.resendVerificationEmail(userId)
        return ResponseEntity.ok(mapOf("message" to "Verification email resent"))
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies user's email address using the token from the verification email")
    fun verifyEmail(
        @RequestBody request: VerifyEmailRequest
    ): ResponseEntity<Map<String, String>> {
        authService.verifyEmail(request)
        return ResponseEntity.ok(mapOf("message" to "Email verified successfully"))
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Forgot password", description = "Sends a password reset email to the user's email address")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        authService.forgotPassword(request)
        // Always return success to avoid revealing if email exists
        return ResponseEntity.ok(mapOf("message" to "If the email exists, a password reset link has been sent"))
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password", description = "Resets the user's password using the token from the reset email")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<Map<String, String>> {
        authService.resetPassword(request)
        return ResponseEntity.ok(mapOf("message" to "Password reset successfully"))
    }
}
