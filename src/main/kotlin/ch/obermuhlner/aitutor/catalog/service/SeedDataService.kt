package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
@Profile("dev", "default")  // Only run in dev mode
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val catalogProperties: CatalogProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun seedData() {
        // Only seed if database is empty
        if (tutorProfileRepository.count() > 0) {
            logger.info("Seed data already exists, skipping...")
            return
        }

        logger.debug("Seeding catalog data from configuration...")

        // Validate curriculum files exist before seeding
        validateCurriculumFiles()

        val tutors = seedTutors()
        val courses = seedCourses(tutors)

        logger.info("Seeded ${tutors.size} tutors and ${courses.size} courses")
    }

    private fun validateCurriculumFiles() {
        logger.info("Validating curriculum files for configured courses...")
        
        var validCourses = 0
        var invalidCourses = 0
        val missingCourses = mutableListOf<String>()
        
        for (config in catalogProperties.courses) {
            // Generate slug as per LessonProgressionService logic
            val languageOnly = config.languageCode.lowercase().substringBefore("-")
            val nameEnglish = config.nameEnglish
            val courseSlug = "$languageOnly-${nameEnglish.lowercase().replace(" ", "-")}"
            
            val curriculumResource = ClassPathResource("course-content/$courseSlug/curriculum.yml")
            if (curriculumResource.exists()) {
                logger.debug("Found curriculum file for course $courseSlug: ${curriculumResource.path}")
                validCourses++
            } else {
                logger.error("Missing curriculum file for course $courseSlug: course-content/$courseSlug/curriculum.yml")
                invalidCourses++
                missingCourses.add("$courseSlug (${config.nameEnglish} - ${config.languageCode})")
            }
        }
        
        if (invalidCourses > 0) {
            logger.error("Found $invalidCourses course(s) without corresponding curriculum files:")
            missingCourses.forEach { logger.error("  - $it") }
            logger.error("Please ensure all configured courses have curriculum files in src/main/resources/course-content/")
            throw IllegalStateException("Some courses are configured without corresponding curriculum files")
        }
        
        logger.info("Curriculum validation completed: $validCourses valid, $invalidCourses missing")
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
                    location = variant.location,
                    personality = archetype.personality,
                    teachingStyle = archetype.teachingStyle,
                    voiceId = archetype.voiceId,
                    gender = variant.gender,
                    age = variant.age,
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
