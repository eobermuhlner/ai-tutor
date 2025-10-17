package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender

data class Tutor(
    val name: String,
    val persona: String = "patient coach",
    val domain: String = "general conversation, grammar, typography",
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val gender: TutorGender? = null
)

enum class TeachingStyle {
    Reactive,   // Follows learner's conversational lead
    Guided,     // Strategic prompts and discovery questions
    Directive   // Explicit instruction and structured lessons
}