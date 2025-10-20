package ch.obermuhlner.aitutor.cli

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpApiClient(private val baseUrl: String) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var accessToken: String? = null

    fun setAccessToken(token: String?) {
        this.accessToken = token
    }

    private fun HttpRequest.Builder.withAuth(): HttpRequest.Builder {
        accessToken?.let { token ->
            this.header("Authorization", "Bearer $token")
        }
        return this
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
        val targetLanguageCode: String,
        val currentTopic: String? = null
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
        val newVocabulary: List<NewVocabulary>? = null,
        val wordCards: List<WordCard>? = null,
        val errorMessage: String? = null
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
    data class WordCard(
        val titleSourceLanguage: String,
        val titleTargetLanguage: String,
        val descriptionSourceLanguage: String,
        val descriptionTargetLanguage: String
    )

    @Serializable
    data class UpdatePhaseRequest(
        val phase: String
    )

    @Serializable
    data class UpdateTopicRequest(
        val currentTopic: String?
    )

    @Serializable
    data class TopicHistoryResponse(
        val currentTopic: String?,
        val pastTopics: List<String>
    )

    // Authentication DTOs
    @Serializable
    data class RegisterRequest(
        val username: String,
        val email: String,
        val password: String,
        val firstName: String?,
        val lastName: String?
    )

    @Serializable
    data class LoginRequest(
        val username: String,
        val password: String
    )

    @Serializable
    data class LoginResponse(
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String,
        val expiresIn: Long,
        val user: UserResponse
    )

    @Serializable
    data class RefreshTokenRequest(
        val refreshToken: String
    )

    @Serializable
    data class UserResponse(
        val id: String,
        val username: String,
        val email: String,
        val firstName: String?,
        val lastName: String?,
        val roles: List<String>,
        val enabled: Boolean,
        val emailVerified: Boolean
    )

    // Catalog DTOs
    @Serializable
    data class LanguageResponse(
        val code: String,
        val name: String,
        val flagEmoji: String,
        val nativeName: String,
        val difficulty: String,
        val description: String,
        val courseCount: Int = 0
    )

    @Serializable
    data class CourseResponse(
        val id: String,
        val languageCode: String,
        val name: String,
        val shortDescription: String,
        val category: String,
        val targetAudience: String,
        val startingLevel: String,
        val targetLevel: String,
        val estimatedWeeks: Int?,
        val displayOrder: Int
    )

    @Serializable
    data class CourseDetailResponse(
        val id: String,
        val languageCode: String,
        val name: String,
        val shortDescription: String,
        val description: String,
        val category: String,
        val targetAudience: String,
        val startingLevel: String,
        val targetLevel: String,
        val estimatedWeeks: Int?,
        val suggestedTutors: List<TutorResponse>,
        val defaultPhase: String,
        val topicSequence: List<String>?,
        val learningGoals: List<String>,
        val tags: List<String>
    )

    @Serializable
    data class TutorResponse(
        val id: String,
        val name: String,
        val emoji: String,
        val persona: String,
        val domain: String,
        val personality: String,
        val description: String,
        val targetLanguageCode: String,
        val culturalBackground: String?,
        val displayOrder: Int
    )

    @Serializable
    data class CreateSessionFromCourseRequest(
        val userId: String,
        val courseTemplateId: String,
        val tutorProfileId: String,
        val sourceLanguageCode: String,
        val customName: String?
    )

    @Serializable
    data class SessionWithProgressResponse(
        val session: SessionResponse,
        val progress: SessionProgressResponse
    )

    @Serializable
    data class SessionProgressResponse(
        val sessionId: String,
        val messageCount: Int,
        val vocabularyCount: Int,
        val daysActive: Int,
        val lastAccessedAt: String
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
            .withAuth()
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
            .withAuth()
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
            .withAuth()
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
            .withAuth()
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
            .withAuth()
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
            .withAuth()
            .DELETE()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to delete session: ${response.statusCode()}")
        }
    }

    fun updateTopic(sessionId: UUID, topic: String?): SessionResponse {
        val requestBody = UpdateTopicRequest(topic)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/topic"))
            .header("Content-Type", "application/json")
            .withAuth()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json.encodeToString(UpdateTopicRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to update topic: ${response.statusCode()}")
        }

        return json.decodeFromString(SessionResponse.serializer(), response.body())
    }

    fun getTopicHistory(sessionId: UUID): TopicHistoryResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/topics/history"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get topic history: ${response.statusCode()}")
        }

        return json.decodeFromString(TopicHistoryResponse.serializer(), response.body())
    }

    // Authentication endpoints
    fun login(username: String, password: String): LoginResponse {
        val requestBody = LoginRequest(username, password)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(LoginRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Login failed: ${response.statusCode()} - ${response.body()}")
        }

        return json.decodeFromString(LoginResponse.serializer(), response.body())
    }

    fun refreshAccessToken(refreshToken: String): LoginResponse {
        val requestBody = RefreshTokenRequest(refreshToken)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/auth/refresh"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(RefreshTokenRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Token refresh failed: ${response.statusCode()}")
        }

        return json.decodeFromString(LoginResponse.serializer(), response.body())
    }

    fun register(
        username: String,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): UserResponse {
        val requestBody = RegisterRequest(username, email, password, firstName, lastName)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(RegisterRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Registration failed: ${response.statusCode()} - ${response.body()}")
        }

        return json.decodeFromString(UserResponse.serializer(), response.body())
    }

    // Catalog endpoints
    fun getAvailableLanguages(sourceLanguage: String = "en"): List<LanguageResponse> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/catalog/languages?sourceLanguage=$sourceLanguage"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get languages: ${response.statusCode()}")
        }

        return json.decodeFromString(response.body())
    }

    fun getCoursesForLanguage(
        languageCode: String,
        sourceLanguage: String = "en",
        userLevel: String? = null
    ): List<CourseResponse> {
        val levelParam = userLevel?.let { "&userLevel=$it" } ?: ""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/catalog/languages/$languageCode/courses?sourceLanguage=$sourceLanguage$levelParam"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get courses: ${response.statusCode()}")
        }

        return json.decodeFromString(response.body())
    }

    fun getTutorsForLanguage(languageCode: String, sourceLanguage: String = "en"): List<TutorResponse> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/catalog/languages/$languageCode/tutors?sourceLanguage=$sourceLanguage"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get tutors: ${response.statusCode()}")
        }

        return json.decodeFromString(response.body())
    }

    fun getCourseDetails(courseId: UUID, sourceLanguage: String = "en"): CourseDetailResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/catalog/courses/$courseId?sourceLanguage=$sourceLanguage"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get course details: ${response.statusCode()}")
        }

        return json.decodeFromString(CourseDetailResponse.serializer(), response.body())
    }

    // Course-based session endpoints
    fun createSessionFromCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String,
        customName: String? = null
    ): SessionResponse {
        val requestBody = CreateSessionFromCourseRequest(
            userId = userId.toString(),
            courseTemplateId = courseTemplateId.toString(),
            tutorProfileId = tutorProfileId.toString(),
            sourceLanguageCode = sourceLanguageCode,
            customName = customName
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/from-course"))
            .header("Content-Type", "application/json")
            .withAuth()
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(CreateSessionFromCourseRequest.serializer(), requestBody)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to create session from course: ${response.statusCode()} - ${response.body()}")
        }

        return json.decodeFromString(SessionResponse.serializer(), response.body())
    }

    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/active?userId=$userId"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get active sessions: ${response.statusCode()}")
        }

        return json.decodeFromString(response.body())
    }

    fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/v1/chat/sessions/$sessionId/progress"))
            .header("Accept", "application/json")
            .withAuth()
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Failed to get session progress: ${response.statusCode()}")
        }

        return json.decodeFromString(SessionProgressResponse.serializer(), response.body())
    }
}
