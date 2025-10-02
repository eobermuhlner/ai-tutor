package ch.obermuhlner.aitutor.chat.dto

data class SessionProgressResponse(
    val messageCount: Int,
    val vocabularyCount: Int,
    val daysActive: Long
)
