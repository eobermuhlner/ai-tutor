package ch.obermuhlner.aitutor.conversation.service

import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Factory service for creating provider-specific ChatOptions with JSON schema support.
 * 
 * This centralized service detects the LLM provider from the ChatModel and delegates
 * to the appropriate ChatOptionsProvider implementation.
 */
@Service
class ChatOptionsFactory(
    private val chatOptionsProviders: List<ChatOptionsProvider>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Creates ChatOptions for the given ChatModel and JSON schema.
     * 
     * @param chatModel The ChatModel to get options for
     * @param jsonSchema The JSON schema to enforce
     * @return Provider-specific ChatOptions, or null if no provider supports the model
     */
    fun createOptions(chatModel: ChatModel, jsonSchema: String): ChatOptions? {
        val provider = chatOptionsProviders.find { it.supports(chatModel) }
        
        return if (provider != null) {
            logger.debug("Using {} for chat options", provider::class.simpleName)
            provider.createOptions(jsonSchema)
        } else {
            logger.warn("No ChatOptionsProvider found for model: ${chatModel.javaClass.name}")
            null
        }
    }

    /**
     * Checks if the given ChatModel is supported by any registered provider.
     */
    fun isSupported(chatModel: ChatModel): Boolean {
        return chatOptionsProviders.any { it.supports(chatModel) }
    }
}