package ch.obermuhlner.aitutor.lesson.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel

data class LessonContentResponse(
    val id: String,
    val title: String,
    val weekNumber: Int?,
    val estimatedDuration: String?,
    val focusAreas: List<String>,
    val targetCEFR: CEFRLevel,
    val fullMarkdown: String
)

