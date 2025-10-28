package ch.obermuhlner.aitutor.chat.dto

data class InitiateTutorMessageRequest(
    val context: String = "welcome"  // "welcome" or "reengage"
)
