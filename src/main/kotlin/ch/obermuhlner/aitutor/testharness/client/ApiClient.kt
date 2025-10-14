package ch.obermuhlner.aitutor.testharness.client

import ch.obermuhlner.aitutor.auth.dto.LoginRequest
import ch.obermuhlner.aitutor.auth.dto.LoginResponse
import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/**
 * REST API client for the AI Tutor application with retry and rate limiting support.
 */
class ApiClient(
    private val baseUrl: String,
    private val timeout: Duration = Duration.ofSeconds(30),
    private val config: TestHarnessConfig? = null
) {
    private val logger = LoggerFactory.getLogger(ApiClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())

    private var accessToken: String? = null

    /**
     * Login and obtain access token.
     */
    fun login(username: String, password: String): LoginResponse {
        val request = LoginRequest(username, password)
        val response = post<LoginResponse>("/api/v1/auth/login", request, requireAuth = false)

        accessToken = response.accessToken
        logger.info("✅ Logged in as: $username")

        return response
    }

    /**
     * Create a new chat session.
     */
    fun createSession(
        userId: UUID,
        tutorName: String,
        sourceLanguageCode: String,
        targetLanguageCode: String,
        conversationPhase: ConversationPhase,
        estimatedCEFRLevel: CEFRLevel,
        teachingStyle: String = "Guided"
    ): SessionResponse {
        val request = mapOf(
            "userId" to userId.toString(),
            "tutorName" to tutorName,
            "tutorPersona" to "test tutor for pedagogical evaluation",
            "tutorDomain" to "general conversation",
            "sourceLanguageCode" to sourceLanguageCode,
            "targetLanguageCode" to targetLanguageCode,
            "conversationPhase" to conversationPhase.name,
            "estimatedCEFRLevel" to estimatedCEFRLevel.name,
            "teachingStyle" to teachingStyle
        )

        val response = post<SessionResponse>("/api/v1/chat/sessions", request)
        logger.debug("Created session: ${response.id}")
        return response
    }

    /**
     * Send a message in a chat session (with retry logic for rate limiting).
     */
    fun sendMessage(sessionId: UUID, content: String): MessageResponse {
        return withRetry {
            val request = SendMessageRequest(content)
            val response = post<MessageResponse>("/api/v1/chat/sessions/$sessionId/messages", request)
            logger.debug("Sent message to session: $sessionId")
            response
        }
    }

    /**
     * Get session with all messages.
     */
    fun getSessionWithMessages(sessionId: UUID): SessionWithMessagesResponse {
        val response = get<SessionWithMessagesResponse>("/api/v1/chat/sessions/$sessionId")
        logger.debug("Retrieved session: $sessionId with ${response.messages.size} messages")
        return response
    }

    /**
     * Get session progress.
     */
    fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
        val response = get<SessionProgressResponse>("/api/v1/chat/sessions/$sessionId/progress")
        logger.debug("Retrieved progress for session: $sessionId")
        return response
    }

    /**
     * Update session phase.
     */
    fun updateSessionPhase(sessionId: UUID, phase: ConversationPhase): SessionResponse {
        val request = UpdatePhaseRequest(phase)
        val response = patch<SessionResponse>("/api/v1/chat/sessions/$sessionId/phase", request)
        logger.debug("Updated session $sessionId phase to: ${phase.name}")
        return response
    }

    /**
     * Delete a session.
     */
    fun deleteSession(sessionId: UUID) {
        delete("/api/v1/chat/sessions/$sessionId")
        logger.debug("Deleted session: $sessionId")
    }

    /**
     * Get current user ID from token.
     */
    fun getCurrentUserId(): UUID {
        val response = get<Map<String, Any>>("/api/v1/auth/me")
        return UUID.fromString(response["id"] as String)
    }

    // Retry logic with exponential backoff

    /**
     * Execute an operation with retry logic for rate limiting (429) and service unavailable (503) errors.
     */
    private fun <T> withRetry(operation: () -> T): T {
        val maxRetries = config?.maxRetries ?: 3
        val backoffMultiplier = config?.retryBackoffMultiplier ?: 2.0
        var lastException: Exception? = null

        for (attempt in 0 until maxRetries) {
            try {
                return operation()
            } catch (e: ApiException) {
                // Check if this is a retryable error (429 or 503)
                val isRetryable = e.message?.let { msg ->
                    msg.contains("status 429") || msg.contains("status 503")
                } ?: false

                if (isRetryable && attempt < maxRetries - 1) {
                    val delayMs = (1000 * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
                    logger.warn("⚠️  Request failed (attempt ${attempt + 1}/$maxRetries): ${e.message}")
                    logger.info("⏳ Waiting ${delayMs}ms before retry...")
                    Thread.sleep(delayMs)
                    lastException = e
                } else {
                    throw e
                }
            }
        }

        // If we get here, all retries failed
        throw lastException ?: RuntimeException("All retry attempts failed")
    }

    // Generic HTTP methods

    private inline fun <reified T> get(path: String): T {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .GET()
            .timeout(timeout)
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw ApiException("GET $path failed with status ${response.statusCode()}: ${response.body()}")
        }

        return objectMapper.readValue(response.body())
    }

    private inline fun <reified T> post(path: String, body: Any, requireAuth: Boolean = true): T {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .timeout(timeout)

        if (requireAuth) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        val request = requestBuilder.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw ApiException("POST $path failed with status ${response.statusCode()}: ${response.body()}")
        }

        return objectMapper.readValue(response.body())
    }

    private inline fun <reified T> patch(path: String, body: Any): T {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .timeout(timeout)
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw ApiException("PATCH $path failed with status ${response.statusCode()}: ${response.body()}")
        }

        return objectMapper.readValue(response.body())
    }

    private fun delete(path: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $accessToken")
            .DELETE()
            .timeout(timeout)
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw ApiException("DELETE $path failed with status ${response.statusCode()}: ${response.body()}")
        }
    }
}

/**
 * Exception thrown when API call fails.
 */
class ApiException(message: String) : RuntimeException(message)
