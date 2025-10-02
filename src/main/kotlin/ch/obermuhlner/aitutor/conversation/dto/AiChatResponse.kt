package ch.obermuhlner.aitutor.conversation.dto

import ch.obermuhlner.aitutor.tutor.domain.ConversationResponse

data class AiChatResponse(
    val reply: String,
    val conversationResponse: ConversationResponse
)
