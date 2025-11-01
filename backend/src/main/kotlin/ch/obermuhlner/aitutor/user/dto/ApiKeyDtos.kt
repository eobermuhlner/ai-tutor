package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.user.domain.LlmProvider
import jakarta.validation.constraints.NotBlank

/**
 * Response showing which API keys/providers are configured for a user.
 * Never exposes actual API keys, only configuration status.
 */
data class ApiKeyConfigurationResponse(
    val openaiConfigured: Boolean,
    val azureOpenaiConfigured: Boolean,
    val anthropicConfigured: Boolean,
    val preferredProvider: LlmProvider?,
    val azureOpenaiEndpoint: String? // Show endpoint (not sensitive) for user reference
)

/**
 * Request to set or update OpenAI API key.
 */
data class UpdateOpenAiKeyRequest(
    @field:NotBlank(message = "API key is required")
    val apiKey: String
)

/**
 * Request to set or update Azure OpenAI API key and endpoint.
 */
data class UpdateAzureOpenAiKeyRequest(
    @field:NotBlank(message = "API key is required")
    val apiKey: String,

    @field:NotBlank(message = "Endpoint is required")
    val endpoint: String
)

/**
 * Request to set or update Anthropic API key.
 */
data class UpdateAnthropicKeyRequest(
    @field:NotBlank(message = "API key is required")
    val apiKey: String
)

/**
 * Request to update preferred LLM provider.
 */
data class UpdatePreferredProviderRequest(
    val provider: LlmProvider
)
