package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.user.domain.LlmProvider
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

/**
 * Service to detect the currently active LLM provider from Spring configuration.
 *
 * The active provider is determined by the `spring.ai.model.chat` property,
 * which is set in application-ai-*.yml profile files.
 */
@Service
class ActiveProviderDetectionService(
    private val environment: Environment
) {

    /**
     * Detects and returns the currently active LLM provider.
     *
     * @return The active LLM provider based on Spring AI configuration
     */
    fun getActiveProvider(): LlmProvider {
        val chatModelProperty = environment.getProperty("spring.ai.model.chat")

        return when (chatModelProperty?.lowercase()) {
            "openai" -> LlmProvider.OPENAI
            "azure-openai" -> LlmProvider.AZURE_OPENAI
            "anthropic" -> LlmProvider.ANTHROPIC
            "ollama" -> LlmProvider.OLLAMA
            else -> LlmProvider.SYSTEM_DEFAULT
        }
    }

    /**
     * Checks if the active provider requires an endpoint configuration.
     *
     * Azure OpenAI and Ollama require custom endpoints.
     *
     * @return true if the active provider requires an endpoint, false otherwise
     */
    fun requiresEndpoint(): Boolean {
        val provider = getActiveProvider()
        return provider == LlmProvider.AZURE_OPENAI || provider == LlmProvider.OLLAMA
    }
}
