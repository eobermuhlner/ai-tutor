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
import ch.obermuhlner.aitutor.catalog.service.FileImportService
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@ConditionalOnProperty(name = ["ai-tutor.catalog.use-seeding"], havingValue = "true", matchIfMissing = true)
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val languageRepository: LanguageRepository,
    private val lessonContentRepository: LessonContentRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val catalogProperties: CatalogProperties,
    private val objectMapper: ObjectMapper,
    private val unifiedCatalogImportService: UnifiedCatalogImportService,
    private val fileImportService: FileImportService
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

            // Import catalog entities (courses, tutors, languages) but not lessons as they may come from file system
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

            // Save languages, tutors, and courses first
            result.languages.forEach { language ->
                languageRepository.save(language)
            }
            tutorProfileRepository.saveAll(result.tutors)
            val savedCourses = courseTemplateRepository.saveAll(result.courses)
            // Save lessons from embedded curriculum if any
            lessonContentRepository.saveAll(result.lessons)

            // Create curriculum rules for embedded lessons
            val coursesWithEmbeddedLessons = result.lessons.groupBy { it.courseId }
            coursesWithEmbeddedLessons.forEach { (courseId, lessons) ->
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

            // Now try to migrate file-based lessons for seeded courses that don't have embedded curriculum
            migrateFileBasedLessons(savedCourses)

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
                isDraft = false,  // Seed data is published
                publishedAt = Instant.now(),
                sourceType = SourceType.SEEDED  // Mark as seeded from configuration
            )
        }

        courseTemplateRepository.saveAll(courseEntities)
        return courseEntities
    }

    /**
     * Migrate file-based lessons to database for seeded courses that don't have embedded lessons.
     */
    private fun migrateFileBasedLessons(courses: List<CourseTemplateEntity>) {
        logger.info("Migrating file-based lessons for ${courses.size} seeded courses...")

        val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
        val yamlMapper = com.fasterxml.jackson.databind.ObjectMapper(com.fasterxml.jackson.dataformat.yaml.YAMLFactory()).registerKotlinModule()

        courses.forEach { course ->
            try {
                // Generate course slug (same logic as in CourseMigrationMain)
                val courseSlug = generateCourseSlug(course)
                val curriculumResource = org.springframework.core.io.ClassPathResource("course-content/$courseSlug/curriculum.yml")

                // Check if curriculum file exists for this course
                if (curriculumResource.exists()) {
                    logger.debug("Found curriculum file for course $courseSlug, checking for lessons...")

                    // Check if this course already has lessons in the database (embedded in seed data)
                    val existingLessons = lessonContentRepository.findByCourseId(course.id)
                    if (existingLessons.isNotEmpty()) {
                        logger.debug("Course ${course.id} already has ${existingLessons.size} embedded lessons in database, skipping file migration")
                        return@forEach
                    }

                    // Parse curriculum file
                    val curriculum = curriculumResource.inputStream.use {
                        yamlMapper.readValue(it, ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum::class.java)
                    }

                    // Load lesson files
                    val lessonResources = try {
                        org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                            .getResources("classpath:course-content/$courseSlug/*.md")
                    } catch (e: Exception) {
                        logger.warn("No lesson files found for $courseSlug: ${e.message}")
                        emptyArray<org.springframework.core.io.Resource>()
                    }

                    logger.debug("Found ${lessonResources.size} lesson files for $courseSlug")

                    // Parse each lesson
                    val lessonsParsed = mutableMapOf<String, ch.obermuhlner.aitutor.catalog.service.FileImportService.LessonParseResult>()
                    lessonResources.forEach { lessonResource ->
                        try {
                            val markdown = lessonResource.inputStream.bufferedReader().readText()
                            val fileName = lessonResource.filename ?: "unknown.md"
                            val parsed = fileImportService.parseLessonFromString(markdown, fileName)
                            lessonsParsed[parsed.lessonId] = parsed
                        } catch (e: Exception) {
                            logger.warn("Failed to parse lesson ${lessonResource.filename}: ${e.message}")
                        }
                    }

                    // Create lesson entities from file-based lessons
                    val lessons = curriculum.lessons.mapIndexedNotNull { index, metadata ->
                        val parsed = lessonsParsed[metadata.id]
                        if (parsed == null) {
                            logger.warn("Missing lesson file for ${metadata.id} in course $courseSlug")
                            null
                        } else {
                            fileImportService.createLessonEntity(
                                courseId = course.id,
                                lessonParseResult = parsed,
                                displayOrder = index,
                                minimumDays = metadata.minimumDays,
                                requiredTurns = metadata.requiredTurns
                            )
                        }
                    }.filterNotNull()

                    if (lessons.isNotEmpty()) {
                        // Save lessons to database
                        lessonContentRepository.saveAll(lessons)

                        // Create curriculum rule if doesn't exist
                        val existingRule = curriculumRuleRepository.findByCourseId(course.id)
                        if (existingRule == null) {
                            val progressionMode = when (curriculum.progressionMode) {
                                ch.obermuhlner.aitutor.lesson.domain.ProgressionMode.TIME_BASED -> "TIME_BASED"
                                ch.obermuhlner.aitutor.lesson.domain.ProgressionMode.COMPLETION_BASED -> "COMPLETION_BASED"
                            }

                            val rule = ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity(
                                courseId = course.id,
                                progressionMode = progressionMode,
                                allowSkipping = false,
                                requireCompletion = true,
                                createdAt = Instant.now(),
                                updatedAt = Instant.now()
                            )
                            curriculumRuleRepository.save(rule)
                        }

                        logger.info("Migrated ${lessons.size} lessons for course $courseSlug (ID: ${course.id}) to database")
                    } else {
                        logger.debug("No lessons to migrate for course $courseSlug")
                    }
                } else {
                    logger.debug("No curriculum file found for $courseSlug, skipping lesson migration")
                }
            } catch (e: Exception) {
                logger.error("Failed to migrate lessons for course ${course.nameJson} (${course.id})", e)
            }
        }

        logger.info("File-based lesson migration completed")
    }

    /**
     * Generate course slug from language code and course name (same logic as CourseMigrationMain)
     */
    private fun generateCourseSlug(course: CourseTemplateEntity): String {
        // Extract English name from JSON
        val nameEnglish = try {
            val nameMap = objectMapper.readValue(course.nameJson, Map::class.java)
            nameMap["en"] as? String ?: "unknown"
        } catch (e: Exception) {
            logger.warn("Could not parse course name JSON for course ${course.id}: ${e.message}")
            "unknown"
        }

        // Generate slug: languageCode-courseName (lowercase, spaces to dashes)
        val languageOnly = course.languageCode.lowercase().substringBefore("-")
        val courseNameSlug = nameEnglish.lowercase().replace(" ", "-").replace("[^a-z0-9-]".toRegex(), "")
        return "$languageOnly-$courseNameSlug"
    }
}
