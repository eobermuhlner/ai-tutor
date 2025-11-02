package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.LlmProvider
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingManager
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
    private val toolCallingManager: ToolCallingManager,
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
     * Generic API key validation that dispatches to the appropriate provider-specific validator.
     *
     * @param provider The LLM provider to validate for
     * @param apiKey The API key to validate (optional for Ollama)
     * @param endpoint The endpoint (required for Azure OpenAI and Ollama)
     * @return ValidationResult with success/failure and error message
     */
    fun validateApiKey(provider: LlmProvider, apiKey: String, endpoint: String? = null): ValidationResult {
        return when (provider) {
            LlmProvider.OPENAI -> validateOpenAiKey(apiKey)
            LlmProvider.AZURE_OPENAI -> {
                if (endpoint.isNullOrBlank()) {
                    ValidationResult.failure("Endpoint is required for Azure OpenAI")
                } else {
                    validateAzureOpenAiKey(apiKey, endpoint)
                }
            }
            LlmProvider.ANTHROPIC -> validateAnthropicKey(apiKey)
            LlmProvider.OLLAMA -> {
                if (endpoint.isNullOrBlank()) {
                    ValidationResult.failure("Endpoint is required for Ollama")
                } else {
                    validateOllamaEndpoint(endpoint)
                }
            }
            LlmProvider.SYSTEM_DEFAULT -> ValidationResult.failure("Cannot validate system default provider")
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

            val chatModel = OpenAiChatModel(openAiApi, openAiChatOptions, toolCallingManager, retryTemplate, observationRegistry)

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

            val chatModel = AzureOpenAiChatModel(openAIClientBuilder, azureOpenAiChatOptions, toolCallingManager, observationRegistry)

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

            val chatModel = AnthropicChatModel(anthropicApi, anthropicChatOptions, toolCallingManager, retryTemplate, observationRegistry)

            // Make a simple test call
            chatModel.call(Prompt(TEST_PROMPT))

            logger.info("Anthropic API key validated successfully")
            ValidationResult.success()
        } catch (e: Exception) {
            logger.warn("Anthropic API key validation failed: ${e.message}")
            ValidationResult.failure("API key validation failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Validate an Ollama endpoint by checking connectivity.
     *
     * @param endpoint The Ollama endpoint URL (e.g., http://localhost:11434)
     * @return ValidationResult with success/failure and error message
     */
    fun validateOllamaEndpoint(endpoint: String): ValidationResult {
        // Basic format checks
        if (endpoint.isBlank()) {
            return ValidationResult.failure("Endpoint cannot be blank")
        }

        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            return ValidationResult.failure("Endpoint must start with http:// or https://")
        }

        // Test endpoint connectivity by making a simple request to /api/tags
        // This endpoint lists available models and doesn't require authentication
        return try {
            val url = java.net.URL("$endpoint/api/tags")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000  // 5 second timeout
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 200) {
                logger.info("Ollama endpoint validated successfully: $endpoint")
                ValidationResult.success()
            } else {
                logger.warn("Ollama endpoint returned unexpected status code: $responseCode")
                ValidationResult.failure("Ollama endpoint is not responding correctly (status: $responseCode)")
            }
        } catch (e: java.net.SocketTimeoutException) {
            logger.warn("Ollama endpoint timeout: ${e.message}")
            ValidationResult.failure("Connection to Ollama endpoint timed out. Is Ollama running?")
        } catch (e: java.net.ConnectException) {
            logger.warn("Ollama endpoint connection failed: ${e.message}")
            ValidationResult.failure("Cannot connect to Ollama endpoint. Is Ollama running?")
        } catch (e: Exception) {
            logger.warn("Ollama endpoint validation failed: ${e.message}")
            ValidationResult.failure("Endpoint validation failed: ${e.message ?: "Unknown error"}")
        }
    }
}
