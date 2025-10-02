package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
import java.time.Instant
import java.util.UUID

data class MessageResponse(
    val id: UUID,
    val role: String,
    val content: String,
    val corrections: List<Correction>?,
    val newVocabulary: List<NewVocabulary>?,
    val wordCards: List<WordCard>?,
    val createdAt: Instant
)
