package ch.obermuhlner.aitutor.chat.dto

data class TopicHistoryResponse(
    val currentTopic: String?,
    val pastTopics: List<String>
)
