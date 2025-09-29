package ch.obermuhlner.aitutor.service

import ch.obermuhlner.aitutor.model.ConversationResponse
import org.springframework.ai.chat.messages.Message

interface AiChatService {
    data class AiChatRequest(
        val messages: List<Message>
    )

    data class AiChatResponse(
        val reply: String,
        val conversationResponse: ConversationResponse
    )

    fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse?
}