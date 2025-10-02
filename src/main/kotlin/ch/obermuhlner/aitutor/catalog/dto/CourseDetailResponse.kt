package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import java.time.Instant
import java.util.UUID

data class CourseDetailResponse(
    val id: UUID,
    val languageCode: String,
    val name: String,                // Localized
    val shortDescription: String,    // Localized
    val description: String,         // Localized
    val category: CourseCategory,
    val targetAudience: String,      // Localized
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,
    val suggestedTutors: List<TutorResponse>,
    val defaultPhase: ConversationPhase,
    val topicSequence: List<String>?,
    val learningGoals: List<String>,  // Localized list
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
