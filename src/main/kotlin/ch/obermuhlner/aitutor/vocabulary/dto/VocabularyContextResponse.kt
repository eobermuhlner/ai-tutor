package ch.obermuhlner.aitutor.vocabulary.dto

import java.util.UUID

data class VocabularyContextResponse(
    val context: String,
    val turnId: UUID?
)
