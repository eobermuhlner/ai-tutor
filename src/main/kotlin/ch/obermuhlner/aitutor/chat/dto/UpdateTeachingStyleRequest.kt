package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle

data class UpdateTeachingStyleRequest(
    val teachingStyle: TeachingStyle
)
