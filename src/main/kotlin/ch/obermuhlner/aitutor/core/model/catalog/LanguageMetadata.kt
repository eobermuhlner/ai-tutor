package ch.obermuhlner.aitutor.core.model.catalog

/**
 * Language metadata stored in configuration (not database).
 * Used by LanguageConfig to provide language information.
 */
data class LanguageMetadata(
    val code: String,              // ISO 639-1 (e.g., "es", "fr")
    val nameJson: String,          // JSON map: {"en": "Spanish", "es": "Español", "de": "Spanisch"}
    val flagEmoji: String,         // Unicode flag emoji
    val nativeName: String,        // Native language name (e.g., "Español")
    val difficulty: Difficulty,
    val descriptionJson: String    // JSON map of descriptions
)
