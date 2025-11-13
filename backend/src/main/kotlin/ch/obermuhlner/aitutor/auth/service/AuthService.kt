package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.JwtProperties
import ch.obermuhlner.aitutor.auth.dto.ChangeEmailRequest
import ch.obermuhlner.aitutor.auth.dto.ChangePasswordRequest
import ch.obermuhlner.aitutor.auth.dto.ForgotPasswordRequest
import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.LoginResponse
import ch.obermuhlner.aitutor.auth.dto.RefreshTokenRequest
import ch.obermuhlner.aitutor.auth.dto.RegisterRequest
import ch.obermuhlner.aitutor.auth.dto.ResetPasswordRequest
import ch.obermuhlner.aitutor.auth.dto.UserResponse
import ch.obermuhlner.aitutor.auth.dto.VerifyEmailRequest
import ch.obermuhlner.aitutor.auth.exception.AccountDisabledException
import ch.obermuhlner.aitutor.auth.exception.AccountLockedException
import ch.obermuhlner.aitutor.auth.exception.DuplicateEmailException
import ch.obermuhlner.aitutor.auth.exception.DuplicateUsernameException
import ch.obermuhlner.aitutor.auth.exception.ExpiredTokenException
import ch.obermuhlner.aitutor.auth.exception.InvalidCredentialsException
import ch.obermuhlner.aitutor.auth.exception.InvalidTokenException
import ch.obermuhlner.aitutor.auth.exception.UserNotFoundException
import ch.obermuhlner.aitutor.auth.exception.WeakPasswordException
import ch.obermuhlner.aitutor.auth.domain.EmailVerificationTokenEntity
import ch.obermuhlner.aitutor.auth.domain.PasswordResetTokenEntity
import ch.obermuhlner.aitutor.auth.repository.EmailVerificationTokenRepository
import ch.obermuhlner.aitutor.auth.repository.PasswordResetTokenRepository
import ch.obermuhlner.aitutor.email.config.EmailProperties
import ch.obermuhlner.aitutor.email.service.EmailService
import ch.obermuhlner.aitutor.user.domain.PronunciationPreference
import ch.obermuhlner.aitutor.user.domain.RefreshTokenEntity
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.service.UserService
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProperties: JwtProperties,
    private val emailService: EmailService,
    private val emailProperties: EmailProperties,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    fun register(request: RegisterRequest): UserResponse {
        logger.info("Registration attempt for username: ${request.username}, email: ${request.email}")

        // Validate username and email uniqueness
        if (userService.existsByUsername(request.username)) {
            logger.warn("Registration failed: username '${request.username}' already exists")
            throw DuplicateUsernameException("Username '${request.username}' is already taken")
        }
        if (userService.existsByEmail(request.email)) {
            logger.warn("Registration failed: email '${request.email}' already registered")
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
            emailVerified = false,
            pronunciationPreference = PronunciationPreference.NONE
        )

        val savedUser = userService.createUser(user)
        logger.info("User registered successfully: ${savedUser.username} (id: ${savedUser.id})")
        return toUserResponse(savedUser)
    }

    fun login(request: LoginRequest): LoginResponse {
        logger.debug("Login attempt for: ${request.username}")

        // Find user by username or email
        val user = userService.findByUsername(request.username)
            ?: userService.findByEmail(request.username)
            ?: run {
                logger.warn("Login failed: user not found for '${request.username}'")
                throw InvalidCredentialsException()
            }

        // Check account status
        if (!user.enabled) {
            logger.warn("Login failed: account disabled for user '${user.username}'")
            throw AccountDisabledException()
        }

        // Check if account is temporarily locked due to failed attempts
        if (user.lockedUntil != null && user.lockedUntil!!.isAfter(Instant.now())) {
            logger.warn("Login failed: account temporarily locked until ${user.lockedUntil} for user '${user.username}'")
            throw AccountLockedException("Account is temporarily locked. Please try again later or contact support.")
        }

        // Check permanent lock status
        if (user.locked) {
            logger.warn("Login failed: account permanently locked for user '${user.username}'")
            throw AccountLockedException("Account is locked. Please contact support.")
        }

        // Verify password
        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            // Track failed login attempt
            user.failedLoginAttempts++
            user.lastFailedLoginAt = Instant.now()

            // Auto-lock account after 5 failed attempts
            if (user.failedLoginAttempts >= 5) {
                user.lockedUntil = Instant.now().plus(Duration.ofMinutes(30))
                userService.updateUser(user)
                logger.warn("Account auto-locked for 30 minutes due to ${user.failedLoginAttempts} failed attempts: ${user.username}")

                // Send lockout notification email
                emailService.sendAccountLockedEmail(user.email, user.username, user.lockedUntil.toString())

                throw AccountLockedException("Account has been locked for 30 minutes due to too many failed login attempts.")
            }

            userService.updateUser(user)
            logger.warn("Login failed: invalid password for user '${user.username}' (attempt ${user.failedLoginAttempts}/5)")
            throw InvalidCredentialsException()
        }

        // Reset failed login attempts on successful login
        if (user.failedLoginAttempts > 0) {
            user.failedLoginAttempts = 0
            user.lastFailedLoginAt = null
            user.lockedUntil = null
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

        logger.info("User logged in successfully: ${user.username} (id: ${user.id})")

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.expirationMs / 1000,  // Convert to seconds
            user = toUserResponse(user)
        )
    }

    fun refreshAccessToken(request: RefreshTokenRequest): LoginResponse {
        logger.debug("Token refresh attempt")

        // Validate refresh token
        if (!jwtTokenService.validateToken(request.refreshToken)) {
            logger.warn("Token refresh failed: invalid or expired token")
            throw InvalidTokenException("Invalid or expired refresh token")
        }

        // Check if token exists and is not revoked
        val tokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: run {
                logger.warn("Token refresh failed: token not found in database")
                throw InvalidTokenException("Refresh token not found")
            }

        if (tokenEntity.revoked) {
            logger.warn("Token refresh failed: token already revoked for user ${tokenEntity.userId}")
            throw InvalidTokenException("Refresh token has been revoked")
        }

        if (tokenEntity.expiresAt.isBefore(Instant.now())) {
            logger.warn("Token refresh failed: token expired for user ${tokenEntity.userId}")
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

        logger.info("Token refreshed successfully for user: ${user.username}")

        return LoginResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.expirationMs / 1000,
            user = toUserResponse(user)
        )
    }

    fun logout(userId: UUID) {
        logger.info("Logout for user: $userId")

        // Revoke all refresh tokens for user
        val tokens = refreshTokenRepository.findAllByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)

        logger.debug("Revoked ${tokens.size} refresh token(s) for user: $userId")
    }

    fun changePassword(userId: UUID, request: ChangePasswordRequest) {
        logger.info("Password change request for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        // Verify current password
        if (user.passwordHash == null || !passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            logger.warn("Password change failed: incorrect current password for user ${user.username}")
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

        // Send password changed notification email
        emailService.sendPasswordChangedEmail(user.email, user.username)

        logger.info("Password changed successfully for user: ${user.username}")
    }

    fun changeEmail(userId: UUID, request: ChangeEmailRequest) {
        logger.info("Email change request for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        // Validate email format
        validateEmail(request.newEmail)

        // Check if email is already in use by another user
        val existingUser = userService.findByEmail(request.newEmail)
        if (existingUser != null && existingUser.id != userId) {
            logger.warn("Email change failed: email '${request.newEmail}' already in use")
            throw DuplicateEmailException("Email '${request.newEmail}' is already registered")
        }

        // Update email
        userService.updateEmail(userId, request.newEmail)

        logger.info("Email changed successfully for user: ${user.username}")
    }

    fun updateProfile(userId: UUID, firstName: String?, lastName: String?): UserResponse {
        logger.info("Profile update request for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        // Update profile fields if provided
        if (firstName != null) {
            user.firstName = firstName
        }
        if (lastName != null) {
            user.lastName = lastName
        }

        val updatedUser = userService.updateUser(user)

        logger.info("Profile updated successfully for user: ${updatedUser.username}")

        return toUserResponse(updatedUser)
    }

    fun updatePronunciationPreference(userId: UUID, pronunciationPreference: PronunciationPreference): UserResponse {
        logger.info("Pronunciation preference update request for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        user.pronunciationPreference = pronunciationPreference
        val updatedUser = userService.updateUser(user)

        logger.info("Pronunciation preference updated to ${pronunciationPreference} for user: ${updatedUser.username}")

        return toUserResponse(updatedUser)
    }

    fun sendVerificationEmail(userId: UUID) {
        logger.info("Sending verification email for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        if (user.emailVerified) {
            logger.warn("Email already verified for user: ${user.username}")
            throw IllegalStateException("Email is already verified")
        }

        // Generate verification token
        val token = generateSecureToken()
        val expiresAt = Instant.now().plus(Duration.ofHours(emailProperties.verificationTokenExpirationHours.toLong()))

        val tokenEntity = EmailVerificationTokenEntity(
            token = token,
            userId = userId,
            expiresAt = expiresAt
        )
        emailVerificationTokenRepository.save(tokenEntity)

        // Send verification email
        emailService.sendVerificationEmail(user.email, user.username, token)

        logger.info("Verification email sent to ${user.email} for user: ${user.username}")
    }

    fun verifyEmail(request: VerifyEmailRequest) {
        logger.info("Email verification attempt with token")

        val tokenEntity = emailVerificationTokenRepository.findByToken(request.token)
            ?: run {
                logger.warn("Email verification failed: token not found")
                throw InvalidTokenException("Invalid verification token")
            }

        if (!tokenEntity.isValid()) {
            logger.warn("Email verification failed: token is expired or already used")
            throw InvalidTokenException("Verification token is expired or already used")
        }

        // Mark token as used
        tokenEntity.usedAt = Instant.now()
        emailVerificationTokenRepository.save(tokenEntity)

        // Mark user email as verified
        val user = userService.findById(tokenEntity.userId)
            ?: throw UserNotFoundException("User not found: ${tokenEntity.userId}")

        user.emailVerified = true
        userService.updateUser(user)

        logger.info("Email verified successfully for user: ${user.username}")
    }

    fun resendVerificationEmail(userId: UUID) {
        logger.info("Resending verification email for user: $userId")

        val user = userService.findById(userId)
            ?: throw UserNotFoundException("User not found: $userId")

        if (user.emailVerified) {
            logger.warn("Email already verified for user: ${user.username}")
            throw IllegalStateException("Email is already verified")
        }

        // Invalidate any existing unused tokens for this user
        val existingTokens = emailVerificationTokenRepository.findByUserId(userId)
        existingTokens.filter { it.usedAt == null }.forEach {
            it.usedAt = Instant.now() // Mark as used to invalidate
        }
        emailVerificationTokenRepository.saveAll(existingTokens)

        // Send new verification email
        sendVerificationEmail(userId)
    }

    fun forgotPassword(request: ForgotPasswordRequest) {
        logger.info("Password reset request for email: ${request.email}")

        val user = userService.findByEmail(request.email)
        if (user == null) {
            // Don't reveal whether email exists - just log and return silently
            logger.warn("Password reset requested for non-existent email: ${request.email}")
            return
        }

        // Generate password reset token
        val token = generateSecureToken()
        val expiresAt = Instant.now().plus(Duration.ofHours(emailProperties.passwordResetTokenExpirationHours.toLong()))

        val tokenEntity = PasswordResetTokenEntity(
            token = token,
            userId = user.id,
            expiresAt = expiresAt
        )
        passwordResetTokenRepository.save(tokenEntity)

        // Send password reset email
        emailService.sendPasswordResetEmail(user.email, user.username, token)

        logger.info("Password reset email sent to ${user.email} for user: ${user.username}")
    }

    fun resetPassword(request: ResetPasswordRequest) {
        logger.info("Password reset attempt with token")

        val tokenEntity = passwordResetTokenRepository.findByToken(request.token)
            ?: run {
                logger.warn("Password reset failed: token not found")
                throw InvalidTokenException("Invalid password reset token")
            }

        if (!tokenEntity.isValid()) {
            logger.warn("Password reset failed: token is expired or already used")
            throw InvalidTokenException("Password reset token is expired or already used")
        }

        // Validate new password
        validatePassword(request.newPassword)

        // Mark token as used
        tokenEntity.usedAt = Instant.now()
        passwordResetTokenRepository.save(tokenEntity)

        // Update password
        val user = userService.findById(tokenEntity.userId)
            ?: throw UserNotFoundException("User not found: ${tokenEntity.userId}")

        userService.updatePassword(user.id, request.newPassword)

        // Reset failed login attempts
        user.failedLoginAttempts = 0
        user.lastFailedLoginAt = null
        user.lockedUntil = null
        userService.updateUser(user)

        // Revoke all refresh tokens (force re-login)
        val tokens = refreshTokenRepository.findAllByUserId(user.id)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)

        // Send password changed notification
        emailService.sendPasswordChangedEmail(user.email, user.username)

        logger.info("Password reset successfully for user: ${user.username}")
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
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

    private fun validateEmail(email: String) {
        // Basic email format validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!email.matches(emailRegex)) {
            throw IllegalArgumentException("Invalid email format")
        }
        if (email.length > 255) {
            throw IllegalArgumentException("Email address is too long")
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
            locked = user.locked,
            emailVerified = user.emailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            subscriptionPlan = user.subscriptionPlan,
            pronunciationPreference = user.pronunciationPreference
        )
    }
}
