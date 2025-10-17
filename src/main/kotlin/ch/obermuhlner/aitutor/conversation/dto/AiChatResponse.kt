package ch.obermuhlner.aitutor.conversation.dto

import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse
import com.fasterxml.jackson.annotation.JsonProperty

data class AiChatResponse(
    @field:JsonProperty(required = true)
    val reply: String,
    @field:JsonProperty(required = true)
    val conversationResponse: ConversationResponse
)
