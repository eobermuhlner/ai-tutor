package ch.obermuhlner.aitutor.auth.controller

import ch.obermuhlner.aitutor.auth.dto.ChangePasswordRequest
import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.LoginResponse
import ch.obermuhlner.aitutor.auth.dto.RefreshTokenRequest
import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.auth.service.AuthService
import ch.obermuhlner.aitutor.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val loginResponse = authService.login(request)
        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<LoginResponse> {
        val loginResponse = authService.refreshAccessToken(request)
        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Void> {
        val user = userService.findByUsername(userDetails.username)
            ?: return ResponseEntity.notFound().build()

        authService.logout(user.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<UserResponse> {
        val user = userService.findByUsername(userDetails.username)
            ?: return ResponseEntity.notFound().build()

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
            lastLoginAt = user.lastLoginAt
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        val user = userService.findByUsername(userDetails.username)
            ?: return ResponseEntity.notFound().build()

        authService.changePassword(user.id, request)
        return ResponseEntity.noContent().build()
    }
}
