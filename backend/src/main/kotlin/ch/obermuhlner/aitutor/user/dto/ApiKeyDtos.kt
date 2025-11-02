package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.user.domain.LlmProvider
import jakarta.validation.constraints.NotBlank

/**
 * Response showing API key configuration status for the system's active provider.
 * Never exposes actual API keys, only configuration status.
 */
data class ApiKeyConfigurationResponse(
    val hasApiKey: Boolean,
    val requiresEndpoint: Boolean,
    val endpoint: String?, // Show endpoint (not sensitive) for user reference
    val activeProvider: LlmProvider
)

/**
 * Request to set or update API key (generic for any provider).
 * Endpoint is optional and only required for Azure OpenAI and Ollama.
 * API key is optional for Ollama (self-hosted, no authentication).
 */
data class UpdateApiKeyRequest(
    val apiKey: String = "",

    val endpoint: String? = null
)
