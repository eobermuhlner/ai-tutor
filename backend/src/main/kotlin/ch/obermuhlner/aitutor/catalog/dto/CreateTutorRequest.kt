package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
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
    val age: Int = 30,
    val gender: TutorGender? = null,
    val personality: TutorPersonality,
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val targetLanguageCode: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val isGlobal: Boolean? = null  // Only admins can set this to true
)

data class UpdateTutorRequest(
    val name: String? = null,
    val emoji: String? = null,
    val personaEnglish: String? = null,
    val domainEnglish: String? = null,
    val descriptionEnglish: String? = null,
    val culturalBackground: String? = null,
    val location: String? = null,
    val age: Int? = null,
    val gender: TutorGender? = null,
    val personality: TutorPersonality? = null,
    val teachingStyle: TeachingStyle? = null,
    val targetLanguageCode: String? = null,
    val isActive: Boolean? = null,
    val displayOrder: Int? = null
)
