package ch.obermuhlner.aitutor.auth.handler

import ch.obermuhlner.aitutor.auth.service.AuthService
import ch.obermuhlner.aitutor.auth.service.JwtTokenService
import ch.obermuhlner.aitutor.user.domain.AuthProvider
import ch.obermuhlner.aitutor.user.domain.RefreshTokenEntity
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.repository.RefreshTokenRepository
import ch.obermuhlner.aitutor.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

@Component
class OAuth2AuthenticationSuccessHandler(
    private val userRepository: UserRepository,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.oauth2.redirect-base-url:http://localhost:5173}")
    private val frontendRedirectUrl: String
) : SimpleUrlAuthenticationSuccessHandler() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2Authentication = authentication as OAuth2AuthenticationToken
        val oAuth2User: OAuth2User = oAuth2Authentication.principal
        val registrationId = oAuth2Authentication.authorizedClientRegistrationId

        // Check if this is an account linking request
        val state = request.getParameter("state")
        val isLinkingRequest = state?.contains("link=true") == true

        try {
            val provider = when (registrationId.lowercase()) {
                "google" -> AuthProvider.GOOGLE
                else -> {
                    logger.error("Unsupported OAuth2 provider: $registrationId")
                    redirectToErrorPage(response, "Unsupported OAuth2 provider")
                    return
                }
            }

            // Extract user information from OAuth2 provider
            val providerId = oAuth2User.getAttribute<String>("sub")
                ?: throw IllegalStateException("Provider ID (sub) not found in OAuth2 user attributes")

            val email = oAuth2User.getAttribute<String>("email")
                ?: throw IllegalStateException("Email not found in OAuth2 user attributes")

            val givenName = oAuth2User.getAttribute<String>("given_name")
            val familyName = oAuth2User.getAttribute<String>("family_name")
            val picture = oAuth2User.getAttribute<String>("picture")

            // Handle account linking vs. login/registration
            val user = if (isLinkingRequest) {
                handleAccountLinking(request, providerId, provider, email, givenName, familyName, picture)
            } else {
                handleLoginOrRegistration(providerId, provider, email, givenName, familyName, picture)
            }

            if (user == null) {
                redirectToErrorPage(response, "Failed to authenticate user")
                return
            }

            // Generate JWT tokens
            val accessToken = jwtTokenService.generateAccessToken(user)
            val refreshToken = jwtTokenService.generateRefreshToken(user)

            // Save refresh token
            val refreshTokenEntity = RefreshTokenEntity(
                userId = user.id,
                token = refreshToken,
                expiresAt = jwtTokenService.getExpirationFromToken(refreshToken)
            )
            refreshTokenRepository.save(refreshTokenEntity)

            // Redirect to frontend with tokens
            val redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .path("/auth/callback")
                .fragment("access_token=$accessToken&refresh_token=$refreshToken")
                .build()
                .toUriString()

            logger.info("OAuth2 authentication successful for user: ${user.username}, redirecting to: $redirectUrl")
            redirectStrategy.sendRedirect(request, response, redirectUrl)

        } catch (e: Exception) {
            logger.error("OAuth2 authentication failed", e)
            redirectToErrorPage(response, "Authentication failed: ${e.message}")
        }
    }

    private fun handleLoginOrRegistration(
        providerId: String,
        provider: AuthProvider,
        email: String,
        givenName: String?,
        familyName: String?,
        picture: String?
    ): UserEntity? {
        // Try to find user by provider and providerId first
        var user = userRepository.findByProviderAndProviderId(provider, providerId)

        if (user != null) {
            // Existing OAuth2 user - update last login and avatar if changed
            user.lastLoginAt = Instant.now()
            if (picture != null && picture != user.avatarUrl) {
                user.avatarUrl = picture
            }
            // Update name if not set
            if (user.firstName == null && givenName != null) {
                user.firstName = givenName
            }
            if (user.lastName == null && familyName != null) {
                user.lastName = familyName
            }
            return userRepository.save(user)
        }

        // Check if user exists with same email but different provider
        val existingUserByEmail = userRepository.findByEmail(email)
        if (existingUserByEmail != null) {
            // Email already exists with different provider - create separate account
            // User can manually link accounts later if desired
            logger.info("User with email $email already exists with provider ${existingUserByEmail.provider}, creating separate OAuth2 account")
        }

        // Create new OAuth2 user
        val username = generateUniqueUsername(email, givenName, familyName)
        user = UserEntity(
            username = username,
            email = email,
            passwordHash = null,  // OAuth2 users don't have passwords
            firstName = givenName,
            lastName = familyName,
            avatarUrl = picture,
            provider = provider,
            providerId = providerId,
            emailVerified = true,  // Trust Google's email verification
            roles = mutableSetOf(UserRole.USER)
        )
        user.lastLoginAt = Instant.now()

        logger.info("Creating new OAuth2 user: $username with provider: $provider")
        return userRepository.save(user)
    }

    private fun handleAccountLinking(
        request: HttpServletRequest,
        providerId: String,
        provider: AuthProvider,
        email: String,
        givenName: String?,
        familyName: String?,
        picture: String?
    ): UserEntity? {
        // For account linking, we need to extract the current user from the request
        // This requires the user to be already authenticated
        // Implementation note: This would require additional session management
        // For now, we'll throw an exception as this feature needs more infrastructure
        throw UnsupportedOperationException("Account linking not yet implemented in OAuth2 flow - use API endpoint instead")
    }

    private fun generateUniqueUsername(email: String, givenName: String?, familyName: String?): String {
        // Try email prefix first
        val baseUsername = email.substringBefore("@").lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")

        // If username is available, use it
        if (!userRepository.existsByUsername(baseUsername)) {
            return baseUsername
        }

        // Try with given name
        if (givenName != null) {
            val nameUsername = givenName.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
            if (!userRepository.existsByUsername(nameUsername)) {
                return nameUsername
            }
        }

        // Append numbers until we find unique username
        var counter = 1
        var username = baseUsername
        while (userRepository.existsByUsername(username)) {
            username = "${baseUsername}${counter}"
            counter++
            if (counter > 1000) {
                // Fallback to random UUID suffix
                username = "${baseUsername}_${System.currentTimeMillis()}"
                break
            }
        }

        return username
    }

    private fun redirectToErrorPage(response: HttpServletResponse, errorMessage: String) {
        val redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
            .path("/login")
            .queryParam("error", "oauth2_failed")
            .queryParam("message", errorMessage)
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }
}
