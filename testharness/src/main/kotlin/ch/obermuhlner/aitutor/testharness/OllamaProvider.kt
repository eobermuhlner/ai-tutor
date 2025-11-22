package ch.obermuhlner.aitutor.testharness.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Ollama provider implementation.
 */
class OllamaProvider(private val config: AiProviderConfig) : AiProvider {
    private val logger = LoggerFactory.getLogger(OllamaProvider::class.java)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(60))
        .build()

    private val apiEndpoint: String = config.apiEndpoint
        ?: System.getenv("OLLAMA_ENDPOINT")
        ?: "http://localhost:11434/api/chat"

    override fun chat(prompt: String, model: String, temperature: Double): String {
        logger.debug("Ollama chat request: model=$model, temperature=$temperature, endpoint=$apiEndpoint")

        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "temperature" to temperature,
            "stream" to false
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiEndpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(600)) // Ollama can be slower on local hardware, especially on first load
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Ollama API call failed with status ${response.statusCode()}: ${response.body()}")
        }

        val responseJson = objectMapper.readTree(response.body())
        val content = responseJson.get("message").get("content").asText()

        logger.debug("Ollama response received (${content.length} chars)")
        return content
    }
}
