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
    @Value("\${testharness.backend.base-url}") private val baseUrl: String
) {
    private var accessToken: String? = null
    private var currentUserId: String? = null

    init {
        // Log in with the demo user to get an access token
        loginAsDemoUser()
    }

    private fun loginAsDemoUser() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "username" to "demo",
            "password" to "demo"
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

            println("Successfully logged in as demo user")
        } catch (e: Exception) {
            println("Failed to log in as demo user: ${e.message}")
            // Continue without authentication, may result in 403 errors
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
    fun createSession(userId: String, language: String, level: String): String {
        val headers = createAuthHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val requestBody = mapOf(
            "userId" to (currentUserId ?: userId),  // Use current user ID if available, otherwise use provided userId
            "targetLanguageCode" to language,
            "estimatedCEFRLevel" to level,
            "tutorName" to "Test Tutor",
            "sourceLanguageCode" to "en",
            "tutorPersona" to "patient coach",
            "tutorDomain" to "general conversation, grammar, typography"
        )

        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions", entity, Map::class.java)

        val responseBody = response.body as Map<*, *>
        return (responseBody["id"] as? String) ?: (responseBody["id"] as? UUID)?.toString()
            ?: throw RuntimeException("Could not extract session ID from response: $responseBody")
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
            "message" to message
        )

        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions/$sessionId/messages", entity, Map::class.java)

        return response.body as Map<String, Any>
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