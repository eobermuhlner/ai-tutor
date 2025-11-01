package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import org.springframework.ai.chat.model.ChatModel

interface AiChatService {
    fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit,
        chatModel: ChatModel? = null // Optional per-user ChatModel (null = use system default)
    ): AiChatResponse?
}