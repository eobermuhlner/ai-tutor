package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import java.time.Instant
import java.util.UUID

data class TutorDetailResponse(
    val id: UUID,
    val name: String,
    val emoji: String,
    val persona: String,              // Localized
    val domain: String,               // Localized
    val personality: TutorPersonality,
    val description: String,          // Localized
    val targetLanguageCode: String,
    val culturalBackground: String?,  // Localized
    val createdAt: Instant,
    val updatedAt: Instant
)
