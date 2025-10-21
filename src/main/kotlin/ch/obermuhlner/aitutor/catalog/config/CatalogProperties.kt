package ch.obermuhlner.aitutor.catalog.config

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ai-tutor.catalog")
data class CatalogProperties(
    var languages: List<LanguageConfig> = emptyList(),
    var tutorArchetypes: List<TutorArchetypeConfig> = emptyList(),
    var courses: List<CourseConfig> = emptyList()
)

data class TutorArchetypeConfig(
    val id: String,
    val emoji: String,
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionTemplateEnglish: String,
    val personality: TutorPersonality,
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val displayOrder: Int,
    val voiceId: TutorVoice? = null
)

data class TutorVariantConfig(
    val archetypeId: String,
    val name: String,
    val culturalNotes: String,
    val displayOrderOverride: Int? = null,
    val gender: TutorGender? = null,  // Override archetype gender if needed
    val age: Int = 30  // Tutor age (default 30)
)

data class LanguageConfig(
    val code: String,
    val nameJson: String,
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val descriptionJson: String,
    val tutorVariants: List<TutorVariantConfig> = emptyList()
)

data class CourseConfig(
    val languageCode: String,
    val nameEnglish: String,
    val shortDescriptionEnglish: String,
    val descriptionEnglish: String,
    val category: CourseCategory,
    val targetAudienceEnglish: String,
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,
    val learningGoalsEnglish: List<String>,
    val displayOrder: Int
)
