package ch.obermuhlner.aitutor.testharness.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Client to interact with the AI Tutor backend REST API
 */
@Service
class ApiClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${testharness.backend.base-url}") private val baseUrl: String
) {
    
    /**
     * Create a new learning session
     */
    fun createSession(userId: String, language: String, level: String): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        
        val requestBody = mapOf(
            "userId" to userId,
            "targetLanguage" to language,
            "level" to level
        )
        
        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions", entity, Map::class.java)
        
        val responseBody = response.body as Map<*, *>
        return responseBody["id"] as String
    }
    
    /**
     * Send a message to the backend
     */
    fun sendMessage(sessionId: String, message: String, userId: String = "test"): Map<String, Any> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        
        val requestBody = mapOf(
            "message" to message,
            "userId" to userId
        )
        
        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions/$sessionId/messages", entity, Map::class.java)
        
        return response.body as Map<String, Any>
    }
    
    /**
     * Get session details with messages
     */
    fun getSession(sessionId: String): Map<String, Any> {
        val response = restTemplate.getForEntity("$baseUrl/api/v1/chat/sessions/$sessionId", Map::class.java)
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
     * Create a session from a course
     */
    fun createSessionFromCourse(userId: String, courseCode: String): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        
        val requestBody = mapOf(
            "userId" to userId,
            "courseCode" to courseCode
        )
        
        val entity = HttpEntity(requestBody, headers)
        val response = restTemplate.postForEntity("$baseUrl/api/v1/chat/sessions/from-course", entity, Map::class.java)
        
        val responseBody = response.body as Map<*, *>
        return responseBody["id"] as String
    }
    
    /**
     * Get available languages from the catalog
     */
    fun getLanguages(): List<Map<String, Any>> {
        val response = restTemplate.getForEntity("$baseUrl/api/v1/catalog/languages", Map::class.java)
        val responseBody = response.body as Map<*, *>
        return responseBody["languages"] as? List<Map<String, Any>> ?: emptyList()
    }
    
    /**
     * Get courses for a specific language
     */
    fun getCoursesForLanguage(languageCode: String): List<Map<String, Any>> {
        val response = restTemplate.getForEntity("$baseUrl/api/v1/catalog/languages/$languageCode/courses", Map::class.java)
        val responseBody = response.body as Map<*, *>
        return responseBody["courses"] as? List<Map<String, Any>> ?: emptyList()
    }
    
    /**
     * Update session phase
     */
    fun updateSessionPhase(sessionId: String, phase: String) {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        
        val requestBody = mapOf("phase" to phase)
        
        val entity = HttpEntity(requestBody, headers)
        restTemplate.patchForObject("$baseUrl/api/v1/chat/sessions/$sessionId/phase", entity, Map::class.java)
    }
    
    /**
     * Get session progress
     */
    fun getSessionProgress(sessionId: String): Map<String, Any> {
        val response = restTemplate.getForEntity("$baseUrl/api/v1/chat/sessions/$sessionId/progress", Map::class.java)
        return response.body as Map<String, Any>
    }
}