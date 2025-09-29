package ch.obermuhlner.aitutor.core.model

data class Tutor(
    val name: String,
    val persona: String = "patient coach",
    val domain: String = "general conversation, grammar, typography",
    val sourceLanguage: String,
    val targetLanguage: String,
)