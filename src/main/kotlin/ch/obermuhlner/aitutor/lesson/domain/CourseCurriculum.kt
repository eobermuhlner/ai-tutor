package ch.obermuhlner.aitutor.lesson.domain

data class CourseCurriculum(
    val courseId: String,
    val progressionMode: ProgressionMode,
    val lessons: List<LessonMetadata>
)

data class LessonMetadata(
    val id: String,
    val file: String,
    val unlockAfterDays: Int,
    val requiredTurns: Int
)

enum class ProgressionMode {
    TIME_BASED,      // Advance after N days
    COMPLETION_BASED // Advance after meeting criteria
}
