package ch.obermuhlner.aitutor.core.model.catalog

/**
 * Gender for tutors, used for:
 * - Gendered language features (articles, adjectives, pronouns)
 * - System prompt personalization ("your teacher" vs "your tutor")
 * - Voice selection (masculine/feminine voices)
 *
 * Examples in gendered languages:
 * - Spanish: "tu profesora" (Female) vs "tu profesor" (Male)
 * - French: "ta professeure" (Female) vs "ton professeur" (Male)
 * - German: "deine Lehrerin" (Female) vs "dein Lehrer" (Male)
 */
enum class TutorGender {
    Male,
    Female,
    Neutral  // For non-binary or unspecified gender contexts
}
