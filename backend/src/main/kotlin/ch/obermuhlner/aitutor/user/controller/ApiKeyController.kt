package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.core.util.ApiKeyEncryptionService
import ch.obermuhlner.aitutor.user.domain.LlmProvider
import ch.obermuhlner.aitutor.user.dto.ApiKeyConfigurationResponse
import ch.obermuhlner.aitutor.user.dto.UpdateAnthropicKeyRequest
import ch.obermuhlner.aitutor.user.dto.UpdateAzureOpenAiKeyRequest
import ch.obermuhlner.aitutor.user.dto.UpdateOpenAiKeyRequest
import ch.obermuhlner.aitutor.user.dto.UpdatePreferredProviderRequest
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.ApiKeyValidationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST controller for managing user API keys (Bring Your Own Key feature).
 *
 * Endpoints:
 * - GET /api/v1/users/me/api-keys - Get API key configuration status
 * - PUT /api/v1/users/me/api-keys/openai - Set OpenAI API key
 * - PUT /api/v1/users/me/api-keys/azure-openai - Set Azure OpenAI API key and endpoint
 * - PUT /api/v1/users/me/api-keys/anthropic - Set Anthropic API key
 * - DELETE /api/v1/users/me/api-keys/{provider} - Remove API key for a provider
 * - PUT /api/v1/users/me/preferred-provider - Set preferred LLM provider
 */
@RestController
@RequestMapping("/api/v1/users/me/api-keys")
@Tag(name = "API Keys", description = "User API key management (BYOK)")
class ApiKeyController(
    private val userRepository: UserRepository,
    private val encryptionService: ApiKeyEncryptionService,
    private val validationService: ApiKeyValidationService
) {

    @GetMapping
    @Operation(summary = "Get API key configuration", description = "Returns which providers are configured (does not expose actual keys)")
    fun getApiKeyConfiguration(authentication: Authentication): ResponseEntity<ApiKeyConfigurationResponse> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val response = ApiKeyConfigurationResponse(
            openaiConfigured = !user.openaiApiKeyEncrypted.isNullOrBlank(),
            azureOpenaiConfigured = !user.azureOpenaiApiKeyEncrypted.isNullOrBlank() &&
                    !user.azureOpenaiEndpoint.isNullOrBlank(),
            anthropicConfigured = !user.anthropicApiKeyEncrypted.isNullOrBlank(),
            preferredProvider = user.preferredProvider,
            azureOpenaiEndpoint = user.azureOpenaiEndpoint
        )

        return ResponseEntity.ok(response)
    }

    @PutMapping("/openai")
    @Operation(summary = "Set OpenAI API key", description = "Validates and stores encrypted OpenAI API key")
    fun setOpenAiKey(
        @Valid @RequestBody request: UpdateOpenAiKeyRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Validate API key
        val validationResult = validationService.validateOpenAiKey(request.apiKey)
        if (!validationResult.isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (validationResult.errorMessage ?: "Invalid API key")))
        }

        // Encrypt and save
        user.openaiApiKeyEncrypted = encryptionService.encrypt(request.apiKey)
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "OpenAI API key saved successfully"))
    }

    @PutMapping("/azure-openai")
    @Operation(summary = "Set Azure OpenAI API key", description = "Validates and stores encrypted Azure OpenAI API key and endpoint")
    fun setAzureOpenAiKey(
        @Valid @RequestBody request: UpdateAzureOpenAiKeyRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Validate API key and endpoint
        val validationResult = validationService.validateAzureOpenAiKey(request.apiKey, request.endpoint)
        if (!validationResult.isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (validationResult.errorMessage ?: "Invalid API key or endpoint")))
        }

        // Encrypt and save
        user.azureOpenaiApiKeyEncrypted = encryptionService.encrypt(request.apiKey)
        user.azureOpenaiEndpoint = request.endpoint
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Azure OpenAI API key and endpoint saved successfully"))
    }

    @PutMapping("/anthropic")
    @Operation(summary = "Set Anthropic API key", description = "Validates and stores encrypted Anthropic API key")
    fun setAnthropicKey(
        @Valid @RequestBody request: UpdateAnthropicKeyRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Validate API key
        val validationResult = validationService.validateAnthropicKey(request.apiKey)
        if (!validationResult.isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (validationResult.errorMessage ?: "Invalid API key")))
        }

        // Encrypt and save
        user.anthropicApiKeyEncrypted = encryptionService.encrypt(request.apiKey)
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Anthropic API key saved successfully"))
    }

    @DeleteMapping("/{provider}")
    @Operation(summary = "Remove API key", description = "Removes the API key for the specified provider")
    fun removeApiKey(
        @PathVariable provider: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val llmProvider = try {
            LlmProvider.valueOf(provider.uppercase().replace("-", "_"))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Invalid provider: $provider"))
        }

        when (llmProvider) {
            LlmProvider.OPENAI -> {
                user.openaiApiKeyEncrypted = null
                if (user.preferredProvider == LlmProvider.OPENAI) {
                    user.preferredProvider = null
                }
            }
            LlmProvider.AZURE_OPENAI -> {
                user.azureOpenaiApiKeyEncrypted = null
                user.azureOpenaiEndpoint = null
                if (user.preferredProvider == LlmProvider.AZURE_OPENAI) {
                    user.preferredProvider = null
                }
            }
            LlmProvider.ANTHROPIC -> {
                user.anthropicApiKeyEncrypted = null
                if (user.preferredProvider == LlmProvider.ANTHROPIC) {
                    user.preferredProvider = null
                }
            }
            LlmProvider.SYSTEM_DEFAULT -> {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Cannot remove system default provider"))
            }
        }

        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "$provider API key removed successfully"))
    }

    @PutMapping("/preferred-provider")
    @Operation(summary = "Set preferred provider", description = "Sets the user's preferred LLM provider")
    fun setPreferredProvider(
        @Valid @RequestBody request: UpdatePreferredProviderRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = UUID.fromString(authentication.name)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Verify the user has configured the provider (except SYSTEM_DEFAULT)
        if (request.provider != LlmProvider.SYSTEM_DEFAULT) {
            val isConfigured = when (request.provider) {
                LlmProvider.OPENAI -> !user.openaiApiKeyEncrypted.isNullOrBlank()
                LlmProvider.AZURE_OPENAI ->
                    !user.azureOpenaiApiKeyEncrypted.isNullOrBlank() &&
                            !user.azureOpenaiEndpoint.isNullOrBlank()
                LlmProvider.ANTHROPIC -> !user.anthropicApiKeyEncrypted.isNullOrBlank()
                else -> false
            }

            if (!isConfigured) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Provider ${request.provider} is not configured. Please add an API key first."))
            }
        }

        user.preferredProvider = request.provider
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Preferred provider set to ${request.provider}"))
    }
}
