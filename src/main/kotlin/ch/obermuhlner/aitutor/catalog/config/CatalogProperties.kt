package ch.obermuhlner.aitutor.catalog.config

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ai-tutor.catalog")
data class CatalogProperties(
    var languages: List<LanguageConfig> = emptyList(),
    var tutors: List<TutorConfig> = emptyList(),
    var courses: List<CourseConfig> = emptyList()
)

data class TutorConfig(
    val name: String,
    val emoji: String,
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionEnglish: String,
    val personality: TutorPersonality,
    val targetLanguageCode: String,
    val displayOrder: Int
)

data class LanguageConfig(
    val code: String,
    val nameJson: String,
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val descriptionJson: String
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
