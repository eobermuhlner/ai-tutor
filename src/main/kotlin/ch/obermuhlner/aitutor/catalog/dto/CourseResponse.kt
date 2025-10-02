package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import java.util.UUID

data class CourseResponse(
    val id: UUID,
    val languageCode: String,
    val name: String,                // Localized
    val shortDescription: String,    // Localized
    val category: CourseCategory,
    val targetAudience: String,      // Localized
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,
    val displayOrder: Int
)
