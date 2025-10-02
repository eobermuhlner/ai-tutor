package ch.obermuhlner.aitutor.chat.dto

data class SessionWithMessagesResponse(
    val session: SessionResponse,
    val messages: List<MessageResponse>
)
