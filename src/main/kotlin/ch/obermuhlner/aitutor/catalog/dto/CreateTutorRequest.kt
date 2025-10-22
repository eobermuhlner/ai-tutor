package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle

data class CreateTutorRequest(
    val name: String,
    val emoji: String,
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionEnglish: String,
    val culturalBackground: String? = null,
    val location: String? = null,
    val personality: TutorPersonality,
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val targetLanguageCode: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
