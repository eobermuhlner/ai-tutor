package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.Difficulty

data class LanguageResponse(
    val code: String,
    val name: String,           // Localized
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val description: String,    // Localized
    val courseCount: Int = 0    // Populated by controller
)
