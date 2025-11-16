package ch.obermuhlner.aitutor.auth.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.oauth2.redirect-base-url:http://localhost:5173}")
    private val frontendRedirectUrl: String
) : SimpleUrlAuthenticationFailureHandler() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        logger.error("OAuth2 authentication failed", exception)

        val errorMessage = exception.message ?: "Authentication failed"

        val redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
            .path("/login")
            .queryParam("error", "oauth2_failed")
            .queryParam("message", errorMessage)
            .build()
            .toUriString()

        logger.info("Redirecting to error page: $redirectUrl")
        redirectStrategy.sendRedirect(request, response, redirectUrl)
    }
}
