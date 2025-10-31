package ch.obermuhlner.aitutor.vocabulary.dto

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import java.time.Instant
import java.util.UUID

data class VocabularyItemResponse(
    val id: UUID,
    val lemma: String,
    val lang: String,
    val exposures: Int,
    val lastSeenAt: Instant,
    val createdAt: Instant,
    val imageUrl: String? = null,
    val conceptName: String? = null,
    // SRS fields
    val nextReviewAt: Instant? = null,
    val reviewStage: Int = 0,
    val isDue: Boolean = false
)

fun VocabularyItemEntity.toResponse(): VocabularyItemResponse {
    val now = Instant.now()
    return VocabularyItemResponse(
        id = id,
        lemma = lemma,
        lang = lang,
        exposures = exposures,
        lastSeenAt = lastSeenAt,
        createdAt = createdAt ?: lastSeenAt,
        imageUrl = conceptName?.let { "/api/v1/images/concept/$it/data" },
        conceptName = conceptName,
        nextReviewAt = nextReviewAt,
        reviewStage = reviewStage,
        isDue = nextReviewAt?.let { it <= now } ?: false
    )
}
