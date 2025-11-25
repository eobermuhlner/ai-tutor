package ch.obermuhlner.aitutor.conversation.service

import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.model.ChatModel

/**
 * Interface for generating provider-specific ChatOptions with JSON schema support.
 * 
 * This allows different LLM providers to have their own specific configuration
 * for strict JSON schema enforcement while maintaining a consistent interface.
 */
interface ChatOptionsProvider {
    /**
     * Checks if this provider can handle the given ChatModel.
     */
    fun supports(chatModel: ChatModel): Boolean
    
    /**
     * Creates ChatOptions for the given JSON schema.
     * 
     * @param jsonSchema The JSON schema to enforce
     * @return Provider-specific ChatOptions configured for the schema
     */
    fun createOptions(jsonSchema: String): ChatOptions
}