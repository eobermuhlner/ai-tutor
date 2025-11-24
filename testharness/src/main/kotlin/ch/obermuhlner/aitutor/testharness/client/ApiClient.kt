package ch.obermuhlner.aitutor.testharness.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Client to interact with the AI Tutor backend REST API
 */
@Service
class ApiClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${testharness.backend.base-url}") private val baseUrl: String,
    @Value("\${testharness.backend.username}") private val username: String,
    @Value("\${testharness.backend.password}") private val password: String
) {
    private var accessToken: String? = null
    private var currentUserId: String? = null

    init {
        // Log in with configured credentials to get an access token
        login()
    }

    private fun login() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "username" to username,
            "password" to password
        )

        val entity = HttpEntity(requestBody, headers)
        try {
            val response = restTemplate.postForEntity("$baseUrl/api/v1/auth/login", entity, Map::class.java)
            val responseBody = response.body as Map<*, *>
            this.accessToken = responseBody["accessToken"] as? String

            // Get user profile to obtain the user ID
            if (this.accessToken != null) {
                getUserProfile()
            }

            println("Successfully logged in as user: $username")
        } catch (e: Exception) {
            println("Failed to log in as user '$username': ${e.message}")
            println("Make sure the user exists and credentials are correct.")
            println("You can configure credentials via:")
            println("  - Environment variables: TESTHARNESS_USERNAME, TESTHARNESS_PASSWORD")
            println("  - application.yml: testharness.backend.username, testharness.backend.password")
            throw RuntimeException("Authentication failed", e)
        }
    }

    private fun getUserProfile() {
        val headers = HttpHeaders()
        headers.setBearerAuth(this.accessToken!!)

        val entity = HttpEntity<Map<String, Any>>(headers)
        try {
            val response = restTemplate.exchange("$baseUrl/api/v1/auth/me", HttpMethod.GET, entity, Map::class.java)
            val responseBody = response.body as Map<*, *>
            this.currentUserId = responseBody["id"] as? String
            if (this.currentUserId == null) {
                // Try to get from "id" field that might be UUID
                val idValue = responseBody["id"]
                this.currentUserId = if (idValue is java.util.UUID) idValue.toString() else idValue?.toString()
            }
            println("Successfully obtained user profile, user ID: $currentUserId")
        } catch (e: Exception) {
            println("Failed to get user profile: ${e.message}")
        }
    }

    /**
     * Create a new learning session
     */
    fun createSession(
        userId: String,
        language: String,
        level: String,
        tutorName: String = "Test Tutor",
        tutorPersona: String = "patient coach",
        initialPhase: String? = null
    ): String {
        val headers = createAuthHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val effectiveUserId = currentUserId ?: userId
        val requestBody = mutableMapOf(
            "userId" to effectiveUserId,  // Use current user ID if available, otherwise use provided userId
            "targetLanguageCode" to language,
            "estimatedCEFRLevel" to level,
            "tutorName" to tutorName,
            "sourceLanguageCode" to "en",
            "tutorPersona" to tutorPersona,
            "tutorDomain" to "general conversation, grammar, typography"
        )

        // Add initial phase if specified
        if (initialPhase != null) {
            requestBody["initialPhase"] = initialPhase
        }

        println("Creating session for user: $effectiveUserId (authenticated as: $currentUserId)")
        println("Session config: language=$language, level=$level, tutor=$tutorName, persona=$tutorPersona, phase=$initialPhase")

        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions", entity, Map::class.java)

        val responseBody = response.body as Map<*, *>
        val sessionId = (responseBody["id"] as? String) ?: (responseBody["id"] as? UUID)?.toString()
            ?: throw RuntimeException("Could not extract session ID from response: $responseBody")

        println("Session created successfully: $sessionId")
        return sessionId
    }

    /**
     * Create a session from a course
     */
    fun createSessionFromCourse(userId: String, courseCode: String): String {
        val headers = createAuthHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "userId" to (currentUserId ?: userId),  // Use current user ID if available
            "courseCode" to courseCode
        )

        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions/from-course", entity, Map::class.java)

        val responseBody = response.body as Map<*, *>
        return (responseBody["id"] as? String) ?: (responseBody["id"] as? UUID)?.toString()
            ?: throw RuntimeException("Could not extract session ID from response: $responseBody")
    }

    /**
     * Send a message to the backend
     */
    fun sendMessage(sessionId: String, message: String, userId: String = "test"): Map<String, Any> {
        val headers = createAuthHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "content" to message
        )

        println("Sending message to session $sessionId")
        println("Request body: $requestBody")
        println("Auth token present: ${accessToken != null}")
        println("Current user ID: $currentUserId")

        val entity = HttpEntity(requestBody, headers)
        try {
            val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions/$sessionId/messages", entity, Map::class.java)
            return response.body as Map<String, Any>
        } catch (e: org.springframework.web.client.HttpClientErrorException.TooManyRequests) {
            println("RATE LIMIT: ${e.message}")
            println("The backend has rate limiting enabled. Please wait and try again.")
            println("This is expected behavior to protect the AI provider API from excessive usage.")
            throw e
        } catch (e: org.springframework.web.client.HttpClientErrorException.Forbidden) {
            println("FORBIDDEN (403): ${e.message}")
            println("This could be a session ownership issue - the backend may be checking if the session belongs to the authenticated user")
            throw e
        } catch (e: Exception) {
            println("ERROR sending message: ${e.message}")
            throw e
        }
    }

    /**
     * Get session details with messages
     */
    fun getSession(sessionId: String): Map<String, Any> {
        val headers = createAuthHeaders()
        val entity = HttpEntity<Map<String, Any>>(headers)
        val response = restTemplate.exchange("$baseUrl/api/v1/chat/sessions/$sessionId", HttpMethod.GET, entity, Map::class.java)
        return response.body as Map<String, Any>
    }

    /**
     * Get messages for a specific session
     */
    fun getSessionMessages(sessionId: String): List<Map<String, Any>> {
        val session = getSession(sessionId)
        return session["messages"] as? List<Map<String, Any>> ?: emptyList()
    }

    /**
     * Get available languages from the catalog
     */
    fun getLanguages(): List<Map<String, Any>> {
        val headers = createAuthHeaders()
        val entity = HttpEntity<Map<String, Any>>(headers)
        val response = restTemplate.exchange("$baseUrl/api/v1/catalog/languages", HttpMethod.GET, entity, Map::class.java)
        val responseBody = response.body as Map<*, *>
        return responseBody["languages"] as? List<Map<String, Any>> ?: emptyList()
    }

    /**
     * Get courses for a specific language
     */
    fun getCoursesForLanguage(languageCode: String): List<Map<String, Any>> {
        val headers = createAuthHeaders()
        val entity = HttpEntity<Map<String, Any>>(headers)
        val response = restTemplate.exchange("$baseUrl/api/v1/catalog/languages/$languageCode/courses", HttpMethod.GET, entity, Map::class.java)
        val responseBody = response.body as Map<*, *>
        return responseBody["courses"] as? List<Map<String, Any>> ?: emptyList()
    }

    /**
     * Update session phase
     */
    fun updateSessionPhase(sessionId: String, phase: String) {
        val headers = createAuthHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf("phase" to phase)

        val entity = HttpEntity(requestBody, headers)
        restTemplate.exchange("$baseUrl/api/v1/chat/sessions/$sessionId/phase", HttpMethod.PATCH, entity, Map::class.java)
    }

    /**
     * Get session progress
     */
    fun getSessionProgress(sessionId: String): Map<String, Any> {
        val headers = createAuthHeaders()
        val entity = HttpEntity<Map<String, Any>>(headers)
        val response = restTemplate.exchange("$baseUrl/api/v1/chat/sessions/$sessionId/progress", HttpMethod.GET, entity, Map::class.java)
        return response.body as Map<String, Any>
    }

    private fun createAuthHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        accessToken?.let {
            headers.setBearerAuth(it)
        }
        return headers
    }
}