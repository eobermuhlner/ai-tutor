package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse

interface AiChatService {
    fun call(
        request: AiChatRequest,
        onReplyText: (String) -> Unit
    ): AiChatResponse?
}