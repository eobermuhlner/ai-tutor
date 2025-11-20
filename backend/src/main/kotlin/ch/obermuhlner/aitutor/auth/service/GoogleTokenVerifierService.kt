package ch.obermuhlner.aitutor.auth.service

import ch.obermuhlner.aitutor.auth.config.GoogleOAuthProperties
import ch.obermuhlner.aitutor.auth.exception.InvalidTokenException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Collections

/**
 * Data class representing verified Google user information.
 */
data class GoogleUserInfo(
    val googleUserId: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String?,
    val givenName: String?,
    val familyName: String?,
    val pictureUrl: String?
)

/**
 * Service for verifying Google ID tokens server-side.
 * Uses Google's official API client library to validate tokens and extract user information.
 */
@Service
class GoogleTokenVerifierService(
    private val googleOAuthProperties: GoogleOAuthProperties
) {
    private val logger = LoggerFactory.getLogger(GoogleTokenVerifierService::class.java)

    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(Collections.singletonList(googleOAuthProperties.clientId))
            .build()
    }

    /**
     * Verify a Google ID token and extract user information.
     *
     * @param idTokenString The Google ID token from the frontend
     * @return GoogleUserInfo with verified user data
     * @throws InvalidTokenException if the token is invalid or verification fails
     */
    fun verifyToken(idTokenString: String): GoogleUserInfo {
        try {
            logger.debug("Attempting to verify Google ID token (length: ${idTokenString.length})")
            val idToken: GoogleIdToken? = verifier.verify(idTokenString)

            if (idToken == null) {
                logger.warn("Google ID token verification failed - token is invalid (returned null)")
                logger.debug("Token snippet: ${idTokenString.take(50)}...")
                throw InvalidTokenException("Invalid Google ID token")
            }

            val payload = idToken.payload

            // Extract user information from the verified token
            val googleUserId = payload.subject
            val email = payload.email
            val emailVerified = payload.emailVerified
            val name = payload["name"] as? String
            val givenName = payload["given_name"] as? String
            val familyName = payload["family_name"] as? String
            val pictureUrl = payload["picture"] as? String

            logger.info("Successfully verified Google ID token for user: $email")

            return GoogleUserInfo(
                googleUserId = googleUserId,
                email = email,
                emailVerified = emailVerified,
                name = name,
                givenName = givenName,
                familyName = familyName,
                pictureUrl = pictureUrl
            )
        } catch (e: InvalidTokenException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error verifying Google ID token: ${e.javaClass.simpleName} - ${e.message}", e)
            logger.debug("Full token that failed: $idTokenString")
            throw InvalidTokenException("Failed to verify Google ID token: ${e.message}")
        }
    }
}
