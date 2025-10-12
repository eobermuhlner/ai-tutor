package ch.obermuhlner.aitutor.lesson.dto

import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode

data class CourseCurriculumResponse(
    val courseId: String,
    val progressionMode: ProgressionMode,
    val lessons: List<LessonMetadataResponse>
)

data class LessonMetadataResponse(
    val id: String,
    val file: String,
    val unlockAfterDays: Int,
    val requiredTurns: Int
)
