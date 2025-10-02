package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.JwtProperties
import ch.obermuhlner.aitutor.auth.dto.*
import ch.obermuhlner.aitutor.auth.exception.*
import ch.obermuhlner.aitutor.user.domain.RefreshTokenEntity
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProperties: JwtProperties
) {

    fun register(request: RegisterRequest): UserResponse {
        // Validate username and email uniqueness
        if (userService.existsByUsername(request.username)) {
            throw DuplicateUsernameException("Username '${request.username}' is already taken")
        }
        if (userService.existsByEmail(request.email)) {
            throw DuplicateEmailException("Email '${request.email}' is already registered")
        }

        // Validate username format
        validateUsername(request.username)

        // Validate password strength
        validatePassword(request.password)

        // Create user entity
        val user = UserEntity(
            username = request.username,
            email = request.email,
            passwordHash = request.password,  // Will be hashed by UserService
            firstName = request.firstName,
            lastName = request.lastName,
            roles = mutableSetOf(UserRole.USER),
            enabled = true,
            emailVerified = false
        )

        val savedUser = userService.createUser(user)
        return toUserResponse(savedUser)
    }

    fun login(request: LoginRequest): LoginResponse {
        // Find user by username or email
        val user = userService.findByUsername(request.username)
            ?: userService.findByEmail(request.username)
            ?: throw InvalidCredentialsException()

        // Check account status
        if (!user.enabled) {
            throw AccountDisabledException()
        }
        if (user.locked) {
            throw AccountLockedException()
        }

        // Verify password
        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        // Generate tokens
        val accessToken = jwtTokenService.generateAccessToken(user)
        val refreshToken = jwtTokenService.generateRefreshToken(user)

        // Save refresh token
        val refreshTokenEntity = RefreshTokenEntity(
            userId = user.id,
            token = refreshToken,
            expiresAt = jwtTokenService.getExpirationFromToken(refreshToken)
        )
        refreshTokenRepository.save(refreshTokenEntity)

        // Update last login time
        user.lastLoginAt = Instant.now()
        userService.updateUser(user)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.expirationMs / 1000,  // Convert to seconds
            user = toUserResponse(user)
        )
    }

    fun refreshAccessToken(request: RefreshTokenRequest): LoginResponse {
        // Validate refresh token
        if (!jwtTokenService.validateToken(request.refreshToken)) {
            throw InvalidTokenException("Invalid or expired refresh token")
        }

        // Check if token exists and is not revoked
        val tokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw InvalidTokenException("Refresh token not found")

        if (tokenEntity.revoked) {
            throw InvalidTokenException("Refresh token has been revoked")
        }

        if (tokenEntity.expiresAt.isBefore(Instant.now())) {
            throw ExpiredTokenException("Refresh token has expired")
        }

        // Get user
        val userId = jwtTokenService.getUserIdFromToken(request.refreshToken)
        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        // Generate new tokens
        val newAccessToken = jwtTokenService.generateAccessToken(user)
        val newRefreshToken = jwtTokenService.generateRefreshToken(user)

        // Revoke old refresh token
        tokenEntity.revoked = true
        refreshTokenRepository.save(tokenEntity)

        // Save new refresh token
        val newTokenEntity = RefreshTokenEntity(
            userId = user.id,
            token = newRefreshToken,
            expiresAt = jwtTokenService.getExpirationFromToken(newRefreshToken)
        )
        refreshTokenRepository.save(newTokenEntity)

        return LoginResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.expirationMs / 1000,
            user = toUserResponse(user)
        )
    }

    fun logout(userId: UUID) {
        // Revoke all refresh tokens for user
        val tokens = refreshTokenRepository.findAllByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)
    }

    fun changePassword(userId: UUID, request: ChangePasswordRequest) {
        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        // Verify current password
        if (user.passwordHash == null || !passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw InvalidCredentialsException("Current password is incorrect")
        }

        // Validate new password
        validatePassword(request.newPassword)

        // Update password
        userService.updatePassword(userId, request.newPassword)

        // Revoke all refresh tokens (force re-login)
        val tokens = refreshTokenRepository.findAllByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)
    }

    private fun validateUsername(username: String) {
        if (username.length < 3 || username.length > 32) {
            throw IllegalArgumentException("Username must be between 3 and 32 characters")
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw IllegalArgumentException("Username can only contain letters, numbers, underscores, and hyphens")
        }
        // Check for reserved words
        val reserved = setOf("admin", "root", "system", "api", "null", "undefined")
        if (username.lowercase() in reserved) {
            throw IllegalArgumentException("Username '$username' is reserved")
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw WeakPasswordException("Password must be at least 8 characters long")
        }
        if (!password.any { it.isUpperCase() }) {
            throw WeakPasswordException("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            throw WeakPasswordException("Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            throw WeakPasswordException("Password must contain at least one digit")
        }
    }

    private fun toUserResponse(user: UserEntity): UserResponse {
        return UserResponse(
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
    }
}
