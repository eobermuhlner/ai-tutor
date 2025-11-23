package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.Correction

data class SendMessageRequest(
    val content: String,
    val corrections: List<Correction>? = null
)
