package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("dev", "default")  // Only run in dev mode
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val catalogProperties: CatalogProperties,
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun seedData() {
        // Only seed if database is empty
        if (tutorProfileRepository.count() > 0) {
            println("Seed data already exists, skipping...")
            return
        }

        println("Seeding catalog data from configuration...")

        val tutors = seedTutors()
        val courses = seedCourses(tutors)

        println("Seeded ${tutors.size} tutors and ${courses.size} courses")
    }

    private fun seedTutors(): Map<String, List<TutorProfileEntity>> {
        // Create archetype lookup map for efficient access
        val archetypeMap = catalogProperties.tutorArchetypes.associateBy { it.id }

        // Cross-join: iterate through languages and their tutor variants
        val tutorEntities = catalogProperties.languages.flatMap { language ->
            language.tutorVariants.map { variant ->
                val archetype = archetypeMap[variant.archetypeId]
                    ?: throw IllegalStateException("Archetype '${variant.archetypeId}' not found for tutor variant '${variant.name}'")

                // Template interpolation: replace {culturalNotes} placeholder
                val description = archetype.descriptionTemplateEnglish
                    .replace("{culturalNotes}", variant.culturalNotes)

                TutorProfileEntity(
                    name = variant.name,
                    emoji = archetype.emoji,
                    personaEnglish = archetype.personaEnglish,
                    domainEnglish = archetype.domainEnglish,
                    descriptionEnglish = description,
                    personaJson = """{"en": "${archetype.personaEnglish}"}""",
                    domainJson = """{"en": "${archetype.domainEnglish}"}""",
                    descriptionJson = """{"en": "$description"}""",
                    culturalBackgroundJson = """{"en": "${variant.culturalNotes}"}""",
                    personality = archetype.personality,
                    targetLanguageCode = language.code,
                    isActive = true,
                    displayOrder = variant.displayOrderOverride ?: archetype.displayOrder
                )
            }
        }

        tutorProfileRepository.saveAll(tutorEntities)

        return tutorEntities.groupBy { it.targetLanguageCode }
    }

    private fun seedCourses(tutorsByLanguage: Map<String, List<TutorProfileEntity>>): List<CourseTemplateEntity> {
        val courseEntities = catalogProperties.courses.map { config ->
            val tutorsForLanguage = tutorsByLanguage[config.languageCode] ?: emptyList()
            val suggestedTutorIds = tutorsForLanguage.map { it.id }

            CourseTemplateEntity(
                languageCode = config.languageCode,
                nameJson = """{"en": "${config.nameEnglish}"}""",
                shortDescriptionJson = """{"en": "${config.shortDescriptionEnglish}"}""",
                descriptionJson = """{"en": "${config.descriptionEnglish}"}""",
                category = config.category,
                targetAudienceJson = """{"en": "${config.targetAudienceEnglish}"}""",
                startingLevel = config.startingLevel,
                targetLevel = config.targetLevel,
                estimatedWeeks = config.estimatedWeeks,
                suggestedTutorIdsJson = objectMapper.writeValueAsString(suggestedTutorIds),
                defaultPhase = ConversationPhase.Auto,
                topicSequenceJson = null,
                learningGoalsJson = """{"en": ${objectMapper.writeValueAsString(config.learningGoalsEnglish)}}""",
                isActive = true,
                displayOrder = config.displayOrder,
                tagsJson = null
            )
        }

        courseTemplateRepository.saveAll(courseEntities)
        return courseEntities
    }
}
