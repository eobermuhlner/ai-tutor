package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.conversation.service.ActiveProviderDetectionService
import ch.obermuhlner.aitutor.core.util.ApiKeyEncryptionService
import ch.obermuhlner.aitutor.user.domain.LlmProvider
import ch.obermuhlner.aitutor.user.dto.ApiKeyConfigurationResponse
import ch.obermuhlner.aitutor.user.dto.UpdateApiKeyRequest
import ch.obermuhlner.aitutor.user.repository.UserRepository
import ch.obermuhlner.aitutor.user.service.ApiKeyValidationService
import ch.obermuhlner.aitutor.user.service.RateLimitingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for managing user API keys (Bring Your Own Key feature).
 *
 * Simplified BYOK implementation:
 * - System-wide provider configuration (determined by application.yml)
 * - Users provide API keys for the active system provider
 * - Single generic endpoint for setting API key
 *
 * Endpoints:
 * - GET /api/v1/users/me/api-key - Get API key configuration status
 * - PUT /api/v1/users/me/api-key - Set API key for active provider
 * - DELETE /api/v1/users/me/api-key - Remove API key
 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "API Keys", description = "User API key management (BYOK)")
class ApiKeyController(
    private val authorizationService: AuthorizationService,
    private val userRepository: UserRepository,
    private val encryptionService: ApiKeyEncryptionService,
    private val validationService: ApiKeyValidationService,
    private val activeProviderDetectionService: ActiveProviderDetectionService,
    private val rateLimitingService: RateLimitingService
) {

    @GetMapping("/api-key")
    @Operation(
        summary = "Get API key configuration",
        description = "Returns API key configuration status for the active system provider (does not expose actual keys)"
    )
    fun getApiKeyConfiguration(): ResponseEntity<ApiKeyConfigurationResponse> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val activeProvider = activeProviderDetectionService.getActiveProvider()
        val requiresEndpoint = activeProviderDetectionService.requiresEndpoint()

        val response = ApiKeyConfigurationResponse(
            hasApiKey = !user.apiKeyEncrypted.isNullOrBlank(),
            requiresEndpoint = requiresEndpoint,
            endpoint = user.endpoint,
            activeProvider = activeProvider
        )

        return ResponseEntity.ok(response)
    }

    @PutMapping("/api-key")
    @Operation(
        summary = "Set API key",
        description = "Validates and stores encrypted API key for the active system provider. For Ollama, only endpoint is required (no API key)."
    )
    fun setApiKey(
        @Valid @RequestBody request: UpdateApiKeyRequest
    ): ResponseEntity<Map<String, String>> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val activeProvider = activeProviderDetectionService.getActiveProvider()
        val requiresEndpoint = activeProviderDetectionService.requiresEndpoint()

        // Validate endpoint requirement
        if (requiresEndpoint && request.endpoint.isNullOrBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Endpoint is required for ${activeProvider}"))
        }

        // For Ollama, API key is optional (self-hosted, no authentication)
        // For other providers, API key is required
        if (activeProvider != LlmProvider.OLLAMA && request.apiKey.isBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "API key is required for ${activeProvider}"))
        }

        // Validate configuration based on provider
        val validationResult = if (activeProvider == LlmProvider.OLLAMA) {
            // For Ollama, only validate endpoint (API key is optional)
            validationService.validateApiKey(
                activeProvider,
                "",  // Empty API key for Ollama
                request.endpoint
            )
        } else {
            // For other providers, validate API key (and endpoint if applicable)
            validationService.validateApiKey(
                activeProvider,
                request.apiKey,
                request.endpoint
            )
        }

        if (!validationResult.isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (validationResult.errorMessage ?: "Invalid configuration")))
        }

        // Determine the appropriate subscription plan based on API key presence
        val newPlan = if (request.apiKey.isNotBlank()) {
            // If API key is provided, switch to FREE_BYOK plan
            ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE_BYOK
        } else {
            // If no API key provided, stay at or revert to FREE plan
            ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE
        }

        // Encrypt and save (only encrypt if API key is provided)
        user.apiKeyEncrypted = if (request.apiKey.isNotBlank()) {
            encryptionService.encrypt(request.apiKey)
        } else {
            null
        }
        user.endpoint = request.endpoint
        user.subscriptionPlan = newPlan
        userRepository.save(user)

        // Reset rate limit buckets to apply new limits immediately
        rateLimitingService.resetRateLimit(userId)

        return ResponseEntity.ok(mapOf("message" to "Configuration saved successfully for provider: $activeProvider and subscription plan updated to: $newPlan"))
    }

    @DeleteMapping("/api-key")
    @Operation(summary = "Remove API key", description = "Removes the user's API key and reverts subscription plan to FREE")
    fun removeApiKey(): ResponseEntity<Map<String, String>> {
        val userId = authorizationService.getCurrentUserId()
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        user.apiKeyEncrypted = null
        user.endpoint = null
        // When API key is removed, revert to FREE plan
        user.subscriptionPlan = ch.obermuhlner.aitutor.user.domain.SubscriptionPlan.FREE
        userRepository.save(user)

        // Reset rate limit buckets to apply new limits immediately
        rateLimitingService.resetRateLimit(userId)

        return ResponseEntity.ok(mapOf("message" to "API key removed successfully and subscription plan reverted to FREE"))
    }
}
