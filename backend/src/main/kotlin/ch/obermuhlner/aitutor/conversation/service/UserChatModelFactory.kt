package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.core.util.ApiKeyEncryptionService
import ch.obermuhlner.aitutor.user.domain.LlmProvider
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import io.micrometer.observation.ObservationRegistry
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service

/**
 * Factory service for creating per-user ChatModel instances.
 *
 * Simplified BYOK implementation:
 * - System-wide provider is determined from application.yml
 * - Users provide API keys for the active system provider only
 * - Falls back to system default ChatModel when user has not configured their own keys
 */
@Service
class UserChatModelFactory(
    private val systemChatModel: ChatModel,
    private val userRepository: UserRepository,
    private val encryptionService: ApiKeyEncryptionService,
    private val activeProviderDetectionService: ActiveProviderDetectionService,
    @Value("\${spring.ai.openai.chat.options.model:gpt-4o}")
    private val defaultOpenAiModel: String,
    @Value("\${spring.ai.azure.openai.chat.options.deployment-name:gpt-4o}")
    private val defaultAzureDeploymentName: String,
    @Value("\${spring.ai.anthropic.chat.options.model:claude-3-5-sonnet-20241022}")
    private val defaultAnthropicModel: String,
    @Value("\${spring.ai.ollama.chat.options.model:granite3.2:8b}")
    private val defaultOllamaModel: String,
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
    private val toolCallingManager: ToolCallingManager
) {
    private val logger = LoggerFactory.getLogger(UserChatModelFactory::class.java)

    /**
     * Get a ChatModel for the specified user.
     *
     * Creates a ChatModel using the user's configured API key for the system's active provider.
     * Falls back to system default if no user configuration exists.
     *
     * @param userId User ID
     * @return ChatModel instance (user-specific or system default)
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException if user's API key configuration is invalid
     */
    fun getChatModelForUser(userId: UUID): ChatModel {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        // Determine active provider from system configuration
        val activeProvider = activeProviderDetectionService.getActiveProvider()

        // For Ollama, users can configure endpoint without API key
        // For other providers, if no API key is configured, use system default
        if (activeProvider != LlmProvider.OLLAMA && user.apiKeyEncrypted.isNullOrBlank()) {
            return systemChatModel
        }

        // Create ChatModel with user's configuration for the active provider
        return when (activeProvider) {
            LlmProvider.OPENAI -> createOpenAiModel(user)
            LlmProvider.AZURE_OPENAI -> createAzureOpenAiModel(user)
            LlmProvider.ANTHROPIC -> createAnthropicModel(user)
            LlmProvider.OLLAMA -> createOllamaModel(user)
            LlmProvider.SYSTEM_DEFAULT -> systemChatModel
        }
    }

    /**
     * Create OpenAI ChatModel with user's API key using builder API.
     */
    private fun createOpenAiModel(user: UserEntity): ChatModel {
        return try {
            val apiKey = encryptionService.decrypt(user.apiKeyEncrypted!!)

            val openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build()

            // GPT-5 models (gpt-5*, o1*, o3*) only support temperature=1.0
            val isGpt5 = defaultOpenAiModel.lowercase().startsWith("gpt-5") ||
                         defaultOpenAiModel.lowercase().startsWith("o1") ||
                         defaultOpenAiModel.lowercase().startsWith("o3")

            val openAiChatOptions = OpenAiChatOptions.builder()
                .model(defaultOpenAiModel)
                .apply { if (isGpt5) temperature(1.0) }
                .build()

            OpenAiChatModel(openAiApi, openAiChatOptions, null, retryTemplate, observationRegistry)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create OpenAI ChatModel for user ${user.id}", e)
        }
    }

    /**
     * Create Azure OpenAI ChatModel with user's API key and endpoint using builder API.
     */
    private fun createAzureOpenAiModel(user: UserEntity): ChatModel {
        val endpoint = user.endpoint
            ?: throw IllegalStateException("Azure OpenAI endpoint not configured for user ${user.id}")

        return try {
            val apiKey = encryptionService.decrypt(user.apiKeyEncrypted!!)

            val openAIClientBuilder = com.azure.ai.openai.OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(com.azure.core.credential.AzureKeyCredential(apiKey))

            val azureOpenAiChatOptions = AzureOpenAiChatOptions.builder()
                .deploymentName(defaultAzureDeploymentName)
                .build()

            AzureOpenAiChatModel(openAIClientBuilder, azureOpenAiChatOptions, null, observationRegistry)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create Azure OpenAI ChatModel for user ${user.id}", e)
        }
    }

    /**
     * Create Anthropic ChatModel with user's API key using builder API.
     */
    private fun createAnthropicModel(user: UserEntity): ChatModel {
        return try {
            val apiKey = encryptionService.decrypt(user.apiKeyEncrypted!!)

            val anthropicApi = AnthropicApi.builder()
                .apiKey(apiKey)
                .build()

            val anthropicChatOptions = AnthropicChatOptions.builder()
                .model(defaultAnthropicModel)
                .build()

            AnthropicChatModel(anthropicApi, anthropicChatOptions, null, retryTemplate, observationRegistry)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create Anthropic ChatModel for user ${user.id}", e)
        }
    }

    /**
     * Create Ollama ChatModel with user's endpoint.
     * Ollama doesn't require an API key (self-hosted).
     */
    private fun createOllamaModel(user: UserEntity): ChatModel {
        if (user.endpoint == null) {
            throw IllegalStateException("Ollama endpoint not configured for user ${user.id}")
        }

        return try {
            // For Spring AI 1.1.0: Use system default for Ollama as well
            // The API changes are too significant to maintain per-user instances
            // This is a temporary solution until we can properly implement the new API
            logger.warn("Ollama provider detected, but using system ChatModel due to API changes in Spring AI 1.1.0")
            systemChatModel
        } catch (e: Exception) {
            // Fallback: use system default if user-specific creation fails
            logger.warn("Failed to handle Ollama provider, falling back to system default", e)
            systemChatModel
        }
    }

    /**
     * Check if user has configured their own API key or endpoint.
     *
     * @param userId User ID
     * @return true if user has configuration (API key or endpoint for Ollama)
     */
    fun isProviderConfiguredForUser(userId: UUID): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val activeProvider = activeProviderDetectionService.getActiveProvider()

        return if (activeProvider == LlmProvider.OLLAMA) {
            // For Ollama, only endpoint is required
            !user.endpoint.isNullOrBlank()
        } else {
            // For other providers, API key is required
            !user.apiKeyEncrypted.isNullOrBlank()
        }
    }
}
