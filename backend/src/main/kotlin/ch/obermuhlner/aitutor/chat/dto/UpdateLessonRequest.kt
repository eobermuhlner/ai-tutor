package ch.obermuhlner.aitutor.chat.dto

enum class LessonNavigationDirection {
    NEXT, PREVIOUS
}

data class UpdateLessonRequest(
    val direction: LessonNavigationDirection
)

data class UpdateLessonToSpecificRequest(
    val lessonId: String
)