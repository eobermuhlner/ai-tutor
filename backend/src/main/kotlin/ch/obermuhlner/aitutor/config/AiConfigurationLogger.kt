package ch.obermuhlner.aitutor.config

import ch.obermuhlner.aitutor.conversation.service.ActiveProviderDetectionService
import ch.obermuhlner.aitutor.user.domain.LlmProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Logs AI model configuration at application startup for visibility and debugging.
 *
 * Displays the active LLM provider, model name, provider-specific settings (endpoints),
 * and configuration flags like strict JSON schema enforcement.
 */
@Component
class AiConfigurationLogger(
    private val environment: Environment,
    private val activeProviderDetection: ActiveProviderDetectionService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AiConfigurationLogger::class.java)

    override fun run(args: ApplicationArguments) {
        val provider = activeProviderDetection.getActiveProvider()
        val activeProfiles = environment.activeProfiles.joinToString(", ")

        logger.info("=".repeat(60))
        logger.info("AI Configuration:")
        logger.info("  Provider: {}", provider.name)

        // Log model name based on provider
        when (provider) {
            LlmProvider.OPENAI -> {
                val model = environment.getProperty("spring.ai.openai.chat.options.model")
                logger.info("  Model: {}", model ?: "not configured")
            }
            LlmProvider.AZURE_OPENAI -> {
                val model = environment.getProperty("spring.ai.azure.openai.chat.options.model")
                val endpoint = environment.getProperty("spring.ai.azure.openai.endpoint")
                logger.info("  Model: {}", model ?: "not configured")
                logger.info("  Endpoint: {}", endpoint ?: "not configured")
            }
            LlmProvider.ANTHROPIC -> {
                val model = environment.getProperty("spring.ai.anthropic.chat.options.model")
                logger.info("  Model: {}", model ?: "not configured")
            }
            LlmProvider.OLLAMA -> {
                val model = environment.getProperty("spring.ai.ollama.chat.options.model")
                val baseUrl = environment.getProperty("spring.ai.ollama.base-url")
                logger.info("  Model: {}", model ?: "not configured")
                logger.info("  Base URL: {}", baseUrl ?: "http://localhost:11434")
            }
            LlmProvider.SYSTEM_DEFAULT -> {
                logger.info("  Model: Using Spring AI autoconfiguration defaults")
            }
        }

        // Log strict schema enforcement
        val strictSchema = environment.getProperty("ai-tutor.chat.strict-schema-enforcement", "true")
        logger.info("  Strict Schema: {}", strictSchema)

        // Log active profiles
        logger.info("  Profiles: [{}]", activeProfiles)
        logger.info("=".repeat(60))
    }
}
