package ch.obermuhlner.aitutor.assessment.dto

import java.time.Instant

data class SkillBreakdownResponse(
    val overall: String,       // Overall CEFR level (e.g., "B1")
    val grammar: String,       // Grammar skill level
    val vocabulary: String,    // Vocabulary skill level
    val fluency: String,       // Fluency skill level
    val comprehension: String, // Comprehension skill level
    val lastAssessedAt: Instant?,
    val assessmentCount: Int
)
