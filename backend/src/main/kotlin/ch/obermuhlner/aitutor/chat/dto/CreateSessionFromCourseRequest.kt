package ch.obermuhlner.aitutor.chat.dto

import java.util.UUID

data class CreateSessionFromCourseRequest(
    val courseTemplateId: UUID,
    val tutorProfileId: UUID? = null,  // If null, use suggested tutor from course
    val customName: String? = null     // Optional custom session name
)
