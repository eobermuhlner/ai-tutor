package ch.obermuhlner.aitutor.conversation.dto

import org.springframework.ai.chat.messages.Message

data class AiChatRequest(
    val messages: List<Message>
)
