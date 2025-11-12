package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.config.CatalogProperties
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@Profile("dev", "default")  // Only run in dev mode
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val languageRepository: LanguageRepository,
    private val lessonContentRepository: LessonContentRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val catalogProperties: CatalogProperties,
    private val objectMapper: ObjectMapper,
    private val unifiedCatalogImportService: UnifiedCatalogImportService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    @Transactional
    fun seedData() {
        // Only seed if database is empty
        if (tutorProfileRepository.count() > 0) {
            logger.info("Seed data already exists, skipping...")
            return
        }

        logger.info("Seeding catalog data...")

        // Try to load unified catalog format first (catalog-seed.yml)
        val catalogResource = ClassPathResource("catalog-seed.yml")
        if (catalogResource.exists()) {
            logger.info("Loading seed data from unified catalog format: catalog-seed.yml")
            seedFromUnifiedFormat(catalogResource)
        } else {
            // Fallback to legacy format (application-seed.yml via CatalogProperties)
            logger.info("catalog-seed.yml not found, falling back to legacy application-seed.yml format")
            seedFromLegacyFormat()
        }
    }

    /**
     * Seed from unified catalog format (catalog-seed.yml).
     */
    private fun seedFromUnifiedFormat(catalogResource: ClassPathResource) {
        try {
            val catalog = catalogResource.inputStream.use { inputStream ->
                unifiedCatalogImportService.parseCatalogFromString(
                    inputStream.bufferedReader().readText()
                )
            }

            logger.info("Parsed unified catalog: ${catalog.languages.size} languages, " +
                    "${catalog.tutors.size} tutors, ${catalog.courses.size} courses")

            // Import all entities
            val result = unifiedCatalogImportService.importCatalog(
                catalogFile = object : org.springframework.web.multipart.MultipartFile {
                    override fun getName() = "catalog-seed.yml"
                    override fun getOriginalFilename() = "catalog-seed.yml"
                    override fun getContentType() = "application/x-yaml"
                    override fun isEmpty() = false
                    override fun getSize() = catalogResource.contentLength()
                    override fun getBytes() = catalogResource.inputStream.readAllBytes()
                    override fun getInputStream() = catalogResource.inputStream
                    override fun transferTo(dest: java.io.File) = throw UnsupportedOperationException()
                    override fun transferTo(dest: java.nio.file.Path) = throw UnsupportedOperationException()
                },
                lessonFiles = emptyList(),  // Lessons loaded from filesystem
                sourceType = SourceType.SEEDED
            )

            // Save all entities
            result.languages.forEach { language ->
                languageRepository.save(language)
            }
            tutorProfileRepository.saveAll(result.tutors)
            courseTemplateRepository.saveAll(result.courses)
            lessonContentRepository.saveAll(result.lessons)

            // Create curriculum rules
            val coursesWithLessons = result.lessons.groupBy { it.courseId }
            coursesWithLessons.forEach { (courseId, lessons) ->
                val hasTimeBased = lessons.any { lesson ->
                    val minDays = lesson.minimumDays
                    minDays != null && minDays > 0
                }
                val rule = CurriculumRuleEntity(
                    courseId = courseId,
                    progressionMode = if (hasTimeBased) "TIME_BASED" else "LINEAR",
                    allowSkipping = false,
                    requireCompletion = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                curriculumRuleRepository.save(rule)
            }

            logger.info("Seeded from unified format: ${result.languages.size} languages, " +
                    "${result.tutors.size} tutors, ${result.courses.size} courses, " +
                    "${result.lessons.size} lessons")

            if (result.errors.isNotEmpty()) {
                logger.warn("Seed import completed with ${result.errors.size} errors:")
                result.errors.forEach { logger.warn("  - $it") }
            }
        } catch (e: Exception) {
            logger.error("Failed to seed from unified format, falling back to legacy format", e)
            seedFromLegacyFormat()
        }
    }

    /**
     * Seed from legacy format (application-seed.yml via CatalogProperties).
     */
    private fun seedFromLegacyFormat() {
        logger.info("Seeding catalog data from legacy configuration...")

        // Validate curriculum files exist before seeding
        validateCurriculumFiles()

        val tutors = seedTutors()
        val courses = seedCourses(tutors)

        logger.info("Seeded ${tutors.size} tutors and ${courses.size} courses from legacy format")
    }

    private fun validateCurriculumFiles() {
        logger.info("Validating curriculum files for configured courses...")
        
        var validCourses = 0
        var invalidCourses = 0
        val missingCourses = mutableListOf<String>()
        
        for (config in catalogProperties.courses) {
            // Skip validation for courses that don't require curriculum
            if (!config.requiresCurriculum) {
                logger.debug("Skipping curriculum validation for ${config.nameEnglish} (${config.languageCode}) - requiresCurriculum=false")
                validCourses++
                continue
            }

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
                    displayOrder = variant.displayOrderOverride ?: archetype.displayOrder,
                    isGlobal = true,  // Seed tutors are global (visible to all users)
                    createdByUserId = null,  // No specific owner for seed tutors
                    sourceType = SourceType.SEEDED  // Mark as seeded from configuration
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
                tagsJson = null,
                sourceType = SourceType.SEEDED  // Mark as seeded from configuration
            )
        }

        courseTemplateRepository.saveAll(courseEntities)
        return courseEntities
    }
}
