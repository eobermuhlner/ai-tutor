package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.core.util.ApiKeyEncryptionService
import ch.obermuhlner.aitutor.user.domain.LlmProvider
import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.repository.UserRepository
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Factory service for creating per-user ChatModel instances.
 *
 * Uses Spring AI builder APIs to create ChatModel instances at runtime with user-provided API keys.
 * Falls back to system default ChatModel when user has not configured their own keys.
 */
@Service
class UserChatModelFactory(
    private val systemChatModel: ChatModel,
    private val userRepository: UserRepository,
    private val encryptionService: ApiKeyEncryptionService,
    @Value("\${spring.ai.openai.chat.options.model:gpt-4o}")
    private val defaultOpenAiModel: String,
    @Value("\${spring.ai.azure.openai.chat.options.deployment-name:gpt-4o}")
    private val defaultAzureDeploymentName: String,
    @Value("\${spring.ai.anthropic.chat.options.model:claude-3-5-sonnet-20241022}")
    private val defaultAnthropicModel: String,
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry
) {

    /**
     * Get a ChatModel for the specified user.
     *
     * Creates a ChatModel using the user's configured API key and preferred provider.
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

        // Determine effective provider
        val effectiveProvider = user.preferredProvider ?: LlmProvider.SYSTEM_DEFAULT

        return when (effectiveProvider) {
            LlmProvider.OPENAI -> createOpenAiModelOrFallback(user)
            LlmProvider.AZURE_OPENAI -> createAzureOpenAiModelOrFallback(user)
            LlmProvider.ANTHROPIC -> createAnthropicModelOrFallback(user)
            LlmProvider.SYSTEM_DEFAULT -> systemChatModel
        }
    }

    /**
     * Create OpenAI ChatModel with user's API key using builder API.
     */
    private fun createOpenAiModelOrFallback(user: UserEntity): ChatModel {
        val encryptedKey = user.openaiApiKeyEncrypted

        if (encryptedKey.isNullOrBlank()) {
            return systemChatModel
        }

        return try {
            val apiKey = encryptionService.decrypt(encryptedKey)

            val openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build()

            val openAiChatOptions = OpenAiChatOptions.builder()
                .model(defaultOpenAiModel)
                .build()

            OpenAiChatModel(openAiApi, openAiChatOptions, null, retryTemplate, observationRegistry)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create OpenAI ChatModel for user ${user.id}", e)
        }
    }

    /**
     * Create Azure OpenAI ChatModel with user's API key and endpoint using builder API.
     */
    private fun createAzureOpenAiModelOrFallback(user: UserEntity): ChatModel {
        val encryptedKey = user.azureOpenaiApiKeyEncrypted
        val endpoint = user.azureOpenaiEndpoint

        if (encryptedKey.isNullOrBlank() || endpoint.isNullOrBlank()) {
            return systemChatModel
        }

        return try {
            val apiKey = encryptionService.decrypt(encryptedKey)

            // Azure OpenAI uses OpenAIClientBuilder from Azure SDK
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
    private fun createAnthropicModelOrFallback(user: UserEntity): ChatModel {
        val encryptedKey = user.anthropicApiKeyEncrypted

        if (encryptedKey.isNullOrBlank()) {
            return systemChatModel
        }

        return try {
            val apiKey = encryptionService.decrypt(encryptedKey)

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
     * Check if user has a specific provider configured with valid credentials.
     *
     * @param userId User ID
     * @param provider Provider to check
     * @return true if user has that provider configured with valid API key
     */
    fun isProviderConfiguredForUser(userId: UUID, provider: LlmProvider): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false

        return when (provider) {
            LlmProvider.OPENAI -> !user.openaiApiKeyEncrypted.isNullOrBlank()
            LlmProvider.AZURE_OPENAI ->
                !user.azureOpenaiApiKeyEncrypted.isNullOrBlank() && !user.azureOpenaiEndpoint.isNullOrBlank()
            LlmProvider.ANTHROPIC -> !user.anthropicApiKeyEncrypted.isNullOrBlank()
            LlmProvider.SYSTEM_DEFAULT -> true // System default is always available
        }
    }
}
