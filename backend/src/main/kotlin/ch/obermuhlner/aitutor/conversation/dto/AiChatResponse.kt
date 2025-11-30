package ch.obermuhlner.aitutor.conversation.dto

import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class AiChatResponse(
    @field:JsonProperty(required = true)
    val reply: String,
    @field:JsonProperty(required = true)
    val conversationResponse: ConversationResponse,
    @field:JsonIgnore  // Exclude from JSON schema - populated from response metadata
    val tokenUsage: TokenUsage? = null
)

/**
 * Token usage information from AI providers.
 * This is NOT part of the AI-generated response JSON,
 * but extracted from the response metadata.
 */
data class TokenUsage(
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long
)
