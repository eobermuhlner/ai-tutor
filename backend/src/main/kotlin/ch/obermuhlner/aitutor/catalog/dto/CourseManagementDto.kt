package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import java.time.Instant
import java.util.UUID

// Course Management DTOs
data class CreateCourseRequest(
    val languageCode: String,
    val nameJson: String,
    val shortDescriptionJson: String,
    val descriptionJson: String,
    val category: CourseCategory,
    val targetAudienceJson: String,
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int? = null,
    val suggestedTutorIdsJson: String? = null,
    val defaultPhase: ConversationPhase = ConversationPhase.Auto,
    val topicSequenceJson: String? = null,
    val learningGoalsJson: String,
    val tagsJson: String? = null
)

data class UpdateCourseRequest(
    val nameJson: String? = null,
    val shortDescriptionJson: String? = null,
    val descriptionJson: String? = null,
    val category: CourseCategory? = null,
    val targetAudienceJson: String? = null,
    val startingLevel: CEFRLevel? = null,
    val targetLevel: CEFRLevel? = null,
    val estimatedWeeks: Int? = null,
    val suggestedTutorIdsJson: String? = null,
    val defaultPhase: ConversationPhase? = null,
    val topicSequenceJson: String? = null,
    val learningGoalsJson: String? = null,
    val tagsJson: String? = null
)

data class CourseManagementResponse(
    val id: UUID,
    val languageCode: String,
    val nameJson: String,
    val shortDescriptionJson: String,
    val descriptionJson: String,
    val category: CourseCategory,
    val targetAudienceJson: String,
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int? = null,
    val suggestedTutorIdsJson: String? = null,
    val defaultPhase: ConversationPhase = ConversationPhase.Auto,
    val topicSequenceJson: String? = null,
    val learningGoalsJson: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val tagsJson: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val isDraft: Boolean = false,
    val publishedAt: Instant? = null,
    val lastEditedBy: UUID? = null,
    val version: Int = 1
)

// Lesson Management DTOs
data class LessonRequest(
    val lessonId: String,
    val title: String,
    val content: String, // markdown content
    val displayOrder: Int,
    val minimumDays: Int? = null,
    val requiredTurns: Int? = null
)

data class LessonResponse(
    val id: UUID,
    val courseId: UUID,
    val lessonId: String,
    val title: String,
    val content: String, // markdown content
    val displayOrder: Int,
    val minimumDays: Int? = null,
    val requiredTurns: Int? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ReorderLessonsRequest(
    val lessons: List<LessonOrderUpdate>
)

data class LessonOrderUpdate(
    val id: UUID,
    val displayOrder: Int
)

// Curriculum Management DTOs
data class CurriculumRequest(
    val progressionMode: String, // TIME_BASED/LINEAR/ADAPTIVE
    val allowSkipping: Boolean = false,
    val requireCompletion: Boolean = false
)

data class CurriculumResponse(
    val id: UUID,
    val courseId: UUID,
    val progressionMode: String, // TIME_BASED/LINEAR/ADAPTIVE
    val allowSkipping: Boolean = false,
    val requireCompletion: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Course Import DTOs
data class CourseImportRequest(
    val languageCode: String,
    val courseName: String,
    val courseDescription: String = "Imported course",
    val category: CourseCategory = CourseCategory.Conversational,
    val startingLevel: CEFRLevel = CEFRLevel.A1,
    val targetLevel: CEFRLevel = CEFRLevel.B2
)

data class CourseImportResponse(
    val courseId: UUID,
    val courseName: String,
    val lessonsImported: Int,
    val errors: List<String> = emptyList(),
    val success: Boolean
)

// Unified Catalog Import DTOs
data class CatalogImportResponse(
    val languagesImported: Int,
    val tutorsImported: Int,
    val coursesImported: Int,
    val lessonsImported: Int,
    val errors: List<String> = emptyList(),
    val success: Boolean
)