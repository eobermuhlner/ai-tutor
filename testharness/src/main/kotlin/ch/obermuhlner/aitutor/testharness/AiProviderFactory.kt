package ch.obermuhlner.aitutor.testharness.ai

import org.slf4j.LoggerFactory

/**
 * Factory for creating AI providers based on configuration.
 */
object AiProviderFactory {
    private val logger = LoggerFactory.getLogger(AiProviderFactory::class.java)

    /**
     * Create an AI provider from configuration.
     */
    fun create(config: AiProviderConfig): AiProvider {
        logger.info("Creating AI provider: ${config.type}")

        return when (config.type) {
            AiProviderType.OPENAI -> OpenAiProvider(config)
            AiProviderType.AZURE_OPENAI -> AzureOpenAiProvider(config)
            AiProviderType.OLLAMA -> OllamaProvider(config)
        }
    }
}
