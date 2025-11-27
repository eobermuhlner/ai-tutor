package ch.obermuhlner.aitutor.lesson.domain

data class CourseCurriculum(
    val courseId: String,
    val progressionMode: ProgressionMode,
    val lessons: List<LessonMetadata>
)

data class LessonMetadata(
    val id: String,
    val file: String,
    val requiredTurns: Int
)

enum class ProgressionMode {
    COMPLETION_BASED, // Advance after meeting turn and goal criteria
    SPECIAL,
}
