package ch.obermuhlner.aitutor.chat.dto

data class SessionWithProgressResponse(
    val session: SessionResponse,
    val progress: SessionProgressResponse
)
