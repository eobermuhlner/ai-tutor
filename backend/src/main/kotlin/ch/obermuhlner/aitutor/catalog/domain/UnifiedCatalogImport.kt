package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.TutorGender
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.core.model.catalog.TutorVoice
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle

/**
 * Unified catalog import format supporting languages, tutors, and courses in a single file.
 * All sections are optional to support partial imports.
 * Replaces application-seed.yml with a more flexible format.
 */
data class UnifiedCatalogImport(
    val version: String = "1.0",
    val tutorArchetypes: List<TutorArchetypeImport> = emptyList(),
    val languages: List<LanguageImport> = emptyList(),
    val tutors: List<TutorImport> = emptyList(),
    val courses: List<CourseImport> = emptyList()
)

/**
 * Reusable tutor template definition (optional).
 * Archetypes can be referenced by tutors for DRY principle.
 */
data class TutorArchetypeImport(
    val id: String,
    val emoji: String,
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionTemplateEnglish: String,  // Supports {culturalNotes} placeholder
    val personality: TutorPersonality,
    val teachingStyle: TeachingStyle = TeachingStyle.Reactive,
    val displayOrder: Int = 0,
    val voiceId: TutorVoice? = null
)

/**
 * Language definition with multilingual support.
 */
data class LanguageImport(
    val code: String,  // BCP 47 language code (e.g., "de-DE", "ja-JP")
    val name: Map<String, String>,  // {en: "German (Germany)", de: "Deutsch (Deutschland)"}
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val description: Map<String, String>,  // Multilingual descriptions
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

/**
 * Tutor definition supporting both archetype references and direct definitions.
 * Either use archetypeId + culturalNotes OR define all properties inline.
 */
data class TutorImport(
    val name: String,
    val targetLanguage: String,  // BCP 47 language code
    val emoji: String? = null,

    // Option 1: Reference archetype (DRY approach)
    val archetypeId: String? = null,
    val culturalNotes: String? = null,  // Replaces {culturalNotes} in archetype template

    // Option 2: Direct definition (standalone approach)
    val persona: Map<String, String>? = null,
    val domain: Map<String, String>? = null,
    val description: Map<String, String>? = null,
    val culturalBackground: Map<String, String>? = null,
    val personality: TutorPersonality? = null,
    val teachingStyle: TeachingStyle? = null,
    val voiceId: TutorVoice? = null,

    // Common fields
    val location: String? = null,
    val gender: TutorGender? = null,
    val age: Int = 30,
    val isGlobal: Boolean = true,
    val displayOrder: Int = 0
)

/**
 * Course definition with optional embedded curriculum.
 */
data class CourseImport(
    val languageCode: String,
    val name: Map<String, String>,
    val shortDescription: Map<String, String>,
    val description: Map<String, String>,
    val category: CourseCategory,
    val targetAudience: Map<String, String>,
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int? = null,
    val learningGoals: Map<String, List<String>>,  // {en: ["Goal 1", "Goal 2"]}
    val suggestedTutors: List<String>? = null,  // List of tutor names to resolve
    val tags: List<String>? = null,
    val displayOrder: Int = 0,
    val requiresCurriculum: Boolean = true,
    val curriculum: CurriculumImport? = null  // Optional embedded curriculum
)

/**
 * Embedded curriculum definition.
 */
data class CurriculumImport(
    val progressionMode: ProgressionMode,
    val lessons: List<LessonImport>
)

/**
 * Lesson definition supporting both embedded content and file references.
 */
data class LessonImport(
    val id: String,
    val title: String? = null,
    val requiredTurns: Int = 0,

    // Option 1: Embedded markdown content (single-file import)
    val content: String? = null,

    // Option 2: File reference (keeps lessons readable)
    val file: String? = null
) {
    init {
        require(content != null || file != null) {
            "Lesson $id must have either 'content' or 'file' specified"
        }
    }
}
