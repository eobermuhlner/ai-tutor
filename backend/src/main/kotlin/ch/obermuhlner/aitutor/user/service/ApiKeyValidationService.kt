package ch.obermuhlner.aitutor.user.service

import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service

/**
 * Service for validating LLM provider API keys by making test requests.
 *
 * Tests API keys by creating temporary ChatModel instances and making a simple test call.
 */
@Service
class ApiKeyValidationService(
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
    @Value("\${spring.ai.openai.chat.options.model:gpt-4o}")
    private val defaultOpenAiModel: String,
    @Value("\${spring.ai.azure.openai.chat.options.deployment-name:gpt-4o}")
    private val defaultAzureDeploymentName: String,
    @Value("\${spring.ai.anthropic.chat.options.model:claude-3-5-sonnet-20241022}")
    private val defaultAnthropicModel: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MIN_KEY_LENGTH = 20  // Minimum reasonable API key length
        private const val TEST_PROMPT = "Hi"  // Simple test prompt to verify API key works
    }

    /**
     * Validation result containing success status and optional error message.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success() = ValidationResult(true)
            fun failure(message: String) = ValidationResult(false, message)
        }
    }

    /**
     * Validate an OpenAI API key by making a test API call.
     *
     * @param apiKey The OpenAI API key to validate
     * @return ValidationResult with success/failure and error message
     */
    fun validateOpenAiKey(apiKey: String): ValidationResult {
        // Basic format checks first
        if (apiKey.isBlank()) {
            return ValidationResult.failure("API key cannot be blank")
        }

        if (apiKey.length < MIN_KEY_LENGTH) {
            return ValidationResult.failure("API key appears to be too short")
        }

        if (!apiKey.startsWith("sk-")) {
            logger.warn("OpenAI API key does not start with 'sk-' - may be invalid")
        }

        // Test the API key by making a real API call
        return try {
            val openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build()

            val openAiChatOptions = OpenAiChatOptions.builder()
                .model(defaultOpenAiModel)
                .maxTokens(5)  // Minimal response to minimize cost
                .build()

            val chatModel = OpenAiChatModel(openAiApi, openAiChatOptions, null, retryTemplate, observationRegistry)

            // Make a simple test call
            chatModel.call(Prompt(TEST_PROMPT))

            logger.info("OpenAI API key validated successfully")
            ValidationResult.success()
        } catch (e: Exception) {
            logger.warn("OpenAI API key validation failed: ${e.message}")
            ValidationResult.failure("API key validation failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Validate an Azure OpenAI API key and endpoint by making a test API call.
     *
     * @param apiKey The Azure OpenAI API key to validate
     * @param endpoint The Azure OpenAI endpoint (e.g., https://your-resource.openai.azure.com)
     * @param deploymentName The deployment name (optional, defaults to configured default)
     * @return ValidationResult with success/failure and error message
     */
    fun validateAzureOpenAiKey(
        apiKey: String,
        endpoint: String,
        deploymentName: String = defaultAzureDeploymentName
    ): ValidationResult {
        // Basic format checks first
        if (apiKey.isBlank()) {
            return ValidationResult.failure("API key cannot be blank")
        }

        if (apiKey.length < MIN_KEY_LENGTH) {
            return ValidationResult.failure("API key appears to be too short")
        }

        if (endpoint.isBlank()) {
            return ValidationResult.failure("Endpoint cannot be blank")
        }

        if (!endpoint.startsWith("https://")) {
            return ValidationResult.failure("Endpoint must start with https://")
        }

        if (!endpoint.contains("openai.azure.com")) {
            logger.warn("Azure endpoint does not contain 'openai.azure.com' - may be invalid")
        }

        // Test the API key by making a real API call
        return try {
            val openAIClientBuilder = com.azure.ai.openai.OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(com.azure.core.credential.AzureKeyCredential(apiKey))

            val azureOpenAiChatOptions = AzureOpenAiChatOptions.builder()
                .deploymentName(deploymentName)
                .maxTokens(5)  // Minimal response to minimize cost
                .build()

            val chatModel = AzureOpenAiChatModel(openAIClientBuilder, azureOpenAiChatOptions, null, observationRegistry)

            // Make a simple test call
            chatModel.call(Prompt(TEST_PROMPT))

            logger.info("Azure OpenAI API key validated successfully")
            ValidationResult.success()
        } catch (e: Exception) {
            logger.warn("Azure OpenAI API key validation failed: ${e.message}")
            ValidationResult.failure("API key validation failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Validate an Anthropic API key by making a test API call.
     *
     * @param apiKey The Anthropic API key to validate
     * @return ValidationResult with success/failure and error message
     */
    fun validateAnthropicKey(apiKey: String): ValidationResult {
        // Basic format checks first
        if (apiKey.isBlank()) {
            return ValidationResult.failure("API key cannot be blank")
        }

        if (apiKey.length < MIN_KEY_LENGTH) {
            return ValidationResult.failure("API key appears to be too short")
        }

        if (!apiKey.startsWith("sk-ant-")) {
            logger.warn("Anthropic API key does not start with 'sk-ant-' - may be invalid")
        }

        // Test the API key by making a real API call
        return try {
            val anthropicApi = AnthropicApi.builder()
                .apiKey(apiKey)
                .build()

            val anthropicChatOptions = AnthropicChatOptions.builder()
                .model(defaultAnthropicModel)
                .maxTokens(5)  // Minimal response to minimize cost
                .build()

            val chatModel = AnthropicChatModel(anthropicApi, anthropicChatOptions, null, retryTemplate, observationRegistry)

            // Make a simple test call
            chatModel.call(Prompt(TEST_PROMPT))

            logger.info("Anthropic API key validated successfully")
            ValidationResult.success()
        } catch (e: Exception) {
            logger.warn("Anthropic API key validation failed: ${e.message}")
            ValidationResult.failure("API key validation failed: ${e.message ?: "Unknown error"}")
        }
    }
}
