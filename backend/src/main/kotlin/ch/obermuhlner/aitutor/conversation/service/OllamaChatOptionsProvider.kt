package ch.obermuhlner.aitutor.conversation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.ollama.api.OllamaChatOptions

/**
 * ChatOptionsProvider implementation for Ollama models with JSON schema format enforcement.
 */
class OllamaChatOptionsProvider(
    private val objectMapper: ObjectMapper
) : ChatOptionsProvider {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun supports(chatModel: ChatModel): Boolean {
        return chatModel.javaClass.name.contains("Ollama", ignoreCase = true)
    }

    override fun createOptions(jsonSchema: String): ChatOptions {
        logger.debug("Creating Ollama JSON schema format options")
        // Convert JSON schema string to Map for Ollama format parameter
        val schemaMap = objectMapper.readValue<Map<String, Any>>(jsonSchema)
        
        return OllamaChatOptions.builder()
            .format(schemaMap)  // Ollama-specific format parameter for JSON schema
            .temperature(0.0)   // Lower temperature for more deterministic JSON output
            .build()
    }
}