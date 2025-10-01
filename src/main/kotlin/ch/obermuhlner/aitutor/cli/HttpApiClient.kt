package ch.obermuhlner.aitutor.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

class HttpApiClient(private val baseUrl: String) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    data class CreateSessionRequest(
        val userId: String,
        val tutorName: String,
        val tutorPersona: String,
        val tutorDomain: String,
        val sourceLanguageCode: String,
        val targetLanguageCode: String,
        val conversationPhase: String,
        val estimatedCEFRLevel: String
    )

    @Serializable
    data class SessionResponse(
        val id: String,
        val userId: String,
        val tutorName: String,
        val conversationPhase: String,
        val sourceLanguageCode: String,
        val targetLanguageCode: String
    )

    @Serializable
    data class SendMessageRequest(
        val content: String
    )

    @Serializable
    data class MessageResponse(
        val id: String,
        val role: String,
        val content: String,
        val corrections: List<Correction>? = null,
        val newVocabulary: List<NewVocabulary>? = null
    )

    @Serializable
    data class Correction(
        val span: String,
        val errorType: String,
        val severity: String,
        val correctedTargetLanguage: String,
        val whySourceLanguage: String,
        val whyTargetLanguage: String
    )

    @Serializable
    data class NewVocabulary(
        val lemma: String,
        val context: String
    )

    @Serializable
    data class UpdatePhaseRequest(
        val phase: String
    )

    fun createSession(
        userId: UUID,
        tutorName: String,
        tutorPersona: String,
        tutorDomain: String,
        sourceLanguage: String,
        targetLanguage: String,
        phase: String,
        cefrLevel: String
    ): SessionResponse {
        val requestBody = CreateSessionRequest(
            userId = userId.toString(),
            tutorName = tutorName,
            tutorPersona = tutorPersona,
            tutorDomain = tutorDomain,
            sourceLanguageCode = sourceLanguage,
            targetLanguageCode = targetLanguage,
            conversationPhase = phase,
            estimatedCEFRLevel = cefrLevel
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(CreateSessionRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to create session: ${response.statusCode()} - ${response.body()}")
        }

        return json.decodeFromString(SessionResponse.serializer(), response.body())
    }

    fun getUserSessions(userId: UUID): List<SessionResponse> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions?userId=$userId"))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get sessions: ${response.statusCode()}")
        }

        return json.decodeFromString(response.body())
    }

    fun sendMessage(sessionId: UUID, content: String): MessageResponse {
        val requestBody = SendMessageRequest(content)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/messages"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(SendMessageRequest.serializer(), requestBody)))
            .timeout(Duration.ofMinutes(2))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to send message: ${response.statusCode()} - ${response.body()}")
        }

        return json.decodeFromString(MessageResponse.serializer(), response.body())
    }

    fun sendMessageStream(sessionId: UUID, content: String, onChunk: (String) -> Unit): MessageResponse {
        val requestBody = SendMessageRequest(content)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/messages/stream"))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(SendMessageRequest.serializer(), requestBody)))
            .timeout(Duration.ofMinutes(2))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofLines())

        var completeMessage: MessageResponse? = null
        val lines = response.body()

        lines.forEach { line ->
            when {
                line.startsWith("event:chunk") -> {
                    // Next line should be data
                }
                line.startsWith("data:") && completeMessage == null -> {
                    val data = line.substring(5).trim()
                    if (data.isNotEmpty() && !data.startsWith("{")) {
                        // It's a chunk
                        onChunk(data)
                    } else if (data.startsWith("{")) {
                        // Could be complete message
                        try {
                            completeMessage = json.decodeFromString(MessageResponse.serializer(), data)
                        } catch (e: Exception) {
                            // Not complete message, treat as chunk
                            onChunk(data)
                        }
                    }
                }
                line.startsWith("event:complete") -> {
                    // Next line should have the complete message
                }
            }
        }

        return completeMessage ?: throw RuntimeException("No complete message received from stream")
    }

    fun updatePhase(sessionId: UUID, phase: String): SessionResponse {
        val requestBody = UpdatePhaseRequest(phase)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/phase"))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json.encodeToString(UpdatePhaseRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to update phase: ${response.statusCode()}")
        }

        return json.decodeFromString(SessionResponse.serializer(), response.body())
    }

    fun deleteSession(sessionId: UUID) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId"))
            .DELETE()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to delete session: ${response.statusCode()}")
        }
    }
}
