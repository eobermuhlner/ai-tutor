package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import java.time.Instant
import java.util.UUID

data class UserLanguageProficiencyResponse(
    val id: UUID,
    val userId: UUID,
    val languageCode: String,
    val proficiencyType: LanguageProficiencyType,
    val cefrLevel: CEFRLevel?,
    val isNative: Boolean,
    val isPrimary: Boolean,
    val selfAssessed: Boolean,
    val lastAssessedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
