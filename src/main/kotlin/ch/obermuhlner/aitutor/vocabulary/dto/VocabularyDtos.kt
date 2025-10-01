package ch.obermuhlner.aitutor.vocabulary.dto

import java.time.Instant
import java.util.UUID

data class VocabularyItemResponse(
    val id: UUID,
    val lemma: String,
    val lang: String,
    val exposures: Int,
    val lastSeenAt: Instant,
    val createdAt: Instant
)

data class VocabularyContextResponse(
    val context: String,
    val turnId: UUID?
)

data class VocabularyItemWithContextsResponse(
    val id: UUID,
    val lemma: String,
    val lang: String,
    val exposures: Int,
    val lastSeenAt: Instant,
    val createdAt: Instant,
    val contexts: List<VocabularyContextResponse>
)
