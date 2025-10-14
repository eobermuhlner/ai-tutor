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
 * Azure OpenAI provider implementation.
 */
class AzureOpenAiProvider(private val config: AiProviderConfig) : AiProvider {
    private val logger = LoggerFactory.getLogger(AzureOpenAiProvider::class.java)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(60))
        .build()

    private val apiKey: String = config.apiKey
        ?: System.getenv("AZURE_OPENAI_API_KEY")
        ?: throw IllegalStateException("Azure OpenAI API key not provided (set AZURE_OPENAI_API_KEY env var or configure in config file)")

    private val apiEndpoint: String = config.apiEndpoint
        ?: System.getenv("AZURE_OPENAI_ENDPOINT")
        ?: throw IllegalStateException("Azure OpenAI endpoint not provided (set AZURE_OPENAI_ENDPOINT env var or configure in config file)")

    private val deploymentName: String = config.deploymentName
        ?: System.getenv("AZURE_OPENAI_DEPLOYMENT")
        ?: throw IllegalStateException("Azure OpenAI deployment name not provided (set AZURE_OPENAI_DEPLOYMENT env var or configure in config file)")

    private val apiVersion: String = "2024-02-15-preview"

    override fun chat(prompt: String, model: String, temperature: Double): String {
        logger.debug("Azure OpenAI chat request: deployment=$deploymentName, temperature=$temperature")

        // Azure uses deployment name, not model name
        val url = "${apiEndpoint.trimEnd('/')}/openai/deployments/$deploymentName/chat/completions?api-version=$apiVersion"

        val requestBody = mapOf(
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "temperature" to temperature
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(120))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Azure OpenAI API call failed with status ${response.statusCode()}: ${response.body()}")
        }

        val responseJson = objectMapper.readTree(response.body())
        val content = responseJson.get("choices").get(0).get("message").get("content").asText()

        logger.debug("Azure OpenAI response received (${content.length} chars)")
        return content
    }
}
