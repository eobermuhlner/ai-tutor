package ch.obermuhlner.aitutor.migration

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.catalog.service.FileImportService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Instant
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * One-time migration script to convert course-content files to database entities.
 *
 * Usage:
 * ```
 * ./gradlew :backend:migrateCoursesFromFiles
 * ```
 *
 * This script:
 * 1. Scans course-content/ directory for curriculum.yml files
 * 2. Parses all curriculum and lesson markdown files
 * 3. Creates database entities with sourceType = SEEDED
 * 4. Is idempotent - can be run multiple times safely (skips existing courses)
 * 5. Generates migration report with statistics
 */
@SpringBootApplication
@ConditionalOnProperty(name = ["ai-tutor.catalog.use-seeding"], havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = ["ch.obermuhlner.aitutor"])
@EntityScan(basePackages = ["ch.obermuhlner.aitutor"])
@ComponentScan(basePackages = ["ch.obermuhlner.aitutor"])
class CourseMigrationApplication {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun migrationRunner(
        fileImportService: FileImportService,
        courseTemplateRepository: CourseTemplateRepository,
        lessonContentRepository: LessonContentRepository,
        curriculumRuleRepository: CurriculumRuleRepository,
        objectMapper: ObjectMapper
    ): CommandLineRunner {
        return CommandLineRunner {
            logger.info("=".repeat(80))
            logger.info("Starting Course Migration: File System → Database")
            logger.info("=".repeat(80))

            val startTime = System.currentTimeMillis()
            val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
            val resolver = PathMatchingResourcePatternResolver()

            // Find all curriculum.yml files
            val curriculumFiles: Array<Resource> = try {
                resolver.getResources("classpath:course-content/*/curriculum.yml")
            } catch (e: Exception) {
                logger.error("Failed to scan course-content directory", e)
                emptyArray()
            }

            logger.info("Found ${curriculumFiles.size} curriculum files to migrate")

            val report = MigrationReport()

            curriculumFiles.forEach { curriculumResource ->
                try {
                    // Extract course slug from path: course-content/de-conversational-german/curriculum.yml
                    // For JAR resources, uri.path is null, so we use uri.toString() instead
                    val uriString = curriculumResource.uri.toString()
                    val pathParts = uriString.split("/")
                    val courseSlug = pathParts[pathParts.indexOf("course-content") + 1]

                    logger.info("-".repeat(80))
                    logger.info("Processing course: $courseSlug")

                    // Check if course already migrated (by matching slug in nameJson)
                    val existing = courseTemplateRepository.findAll()
                        .find { it.sourceType == SourceType.SEEDED && extractSlugFromCourse(it) == courseSlug }

                    if (existing != null) {
                        logger.info("Course $courseSlug already migrated (ID: ${existing.id}). Skipping.")
                        report.skipped++
                        return@forEach
                    }

                    // Parse curriculum
                    val curriculum = curriculumResource.inputStream.use {
                        yamlMapper.readValue(it, CourseCurriculum::class.java)
                    }

                    // Parse all lesson files
                    val lessonResources = try {
                        resolver.getResources("classpath:course-content/$courseSlug/*.md")
                    } catch (e: Exception) {
                        logger.warn("No lesson files found for $courseSlug")
                        emptyArray<Resource>()
                    }

                    logger.info("Found ${lessonResources.size} lesson files")

                    // Parse lessons
                    val lessonsParsed = mutableMapOf<String, FileImportService.LessonParseResult>()
                    lessonResources.forEach { lessonResource ->
                        try {
                            val markdown = lessonResource.inputStream.bufferedReader().readText()
                            val fileName = lessonResource.filename ?: "unknown.md"
                            val parsed = fileImportService.parseLessonFromString(markdown, fileName)
                            lessonsParsed[parsed.lessonId] = parsed
                        } catch (e: Exception) {
                            logger.warn("Failed to parse lesson ${lessonResource.filename}: ${e.message}")
                            report.lessonErrors++
                        }
                    }

                    // Create course entity
                    val (languageCode, category, startingLevel, targetLevel) = inferCourseMetadata(courseSlug)
                    val courseName = courseSlug
                        .substringAfter("-")
                        .split("-")
                        .joinToString(" ") { it.capitalize() }

                    val course = CourseTemplateEntity(
                        languageCode = languageCode,
                        nameJson = """{"en": "$courseName"}""",
                        shortDescriptionJson = """{"en": "Migrated from course-content"}""",
                        descriptionJson = """{"en": "This course was migrated from the legacy file-based system."}""",
                        category = category,
                        targetAudienceJson = """{"en": "Language learners"}""",
                        startingLevel = startingLevel,
                        targetLevel = targetLevel,
                        estimatedWeeks = curriculum.lessons.size,
                        suggestedTutorIdsJson = null,
                        defaultPhase = ConversationPhase.Auto,
                        topicSequenceJson = null,
                        learningGoalsJson = """{"en": ["Complete all lessons", "Build conversational fluency"]}""",
                        isActive = true,
                        displayOrder = 0,
                        tagsJson = null,
                        isDraft = false,  // Migrated courses are published
                        publishedAt = Instant.now(),
                        sourceType = SourceType.SEEDED
                    )

                    val savedCourse = courseTemplateRepository.save(course)
                    logger.info("Created course entity: ${savedCourse.id}")

                    // Create curriculum rule
                    val progressionMode = when (curriculum.progressionMode) {
                        ProgressionMode.TIME_BASED -> "TIME_BASED"
                        ProgressionMode.COMPLETION_BASED -> "COMPLETION_BASED"
                    }

                    val curriculumRule = CurriculumRuleEntity(
                        courseId = savedCourse.id,
                        progressionMode = progressionMode,
                        allowSkipping = false,
                        requireCompletion = true,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                    curriculumRuleRepository.save(curriculumRule)

                    // Create lesson entities
                    val lessons = curriculum.lessons.mapIndexedNotNull { index, metadata ->
                        val parsed = lessonsParsed[metadata.id]
                        if (parsed == null) {
                            logger.warn("Missing lesson file for ${metadata.id}")
                            report.lessonErrors++
                            null
                        } else {
                            fileImportService.createLessonEntity(
                                courseId = savedCourse.id,
                                lessonParseResult = parsed,
                                displayOrder = index,
                                minimumDays = metadata.minimumDays,
                                requiredTurns = metadata.requiredTurns
                            )
                        }
                    }

                    lessonContentRepository.saveAll(lessons)
                    logger.info("Created ${lessons.size} lesson entities")

                    report.coursesCreated++
                    report.lessonsCreated += lessons.size

                } catch (e: Exception) {
                    logger.error("Failed to migrate course from ${curriculumResource.uri}", e)
                    report.courseErrors++
                }
            }

            val elapsedTime = System.currentTimeMillis() - startTime
            logger.info("=".repeat(80))
            logger.info("Migration Complete")
            logger.info("=".repeat(80))
            logger.info(report.summary(elapsedTime))
            logger.info("=".repeat(80))
        }
    }

    private fun extractSlugFromCourse(course: CourseTemplateEntity): String {
        // Extract English name from JSON
        val nameEnglish = try {
            val nameMap = ObjectMapper().readValue(course.nameJson, Map::class.java)
            nameMap["en"] as? String ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

        // Generate slug from language code and name
        val languageOnly = course.languageCode.lowercase().substringBefore("-")
        return "$languageOnly-${nameEnglish.lowercase().replace(" ", "-")}"
    }

    private fun inferCourseMetadata(courseSlug: String): CourseMetadata {
        // Extract language code (first part before dash)
        val languageCode = courseSlug.substringBefore("-")

        // Infer category from course name
        val category = when {
            courseSlug.contains("conversational") -> CourseCategory.Conversational
            courseSlug.contains("grammar") -> CourseCategory.Grammar
            courseSlug.contains("travel") -> CourseCategory.Travel
            courseSlug.contains("business") -> CourseCategory.Business
            else -> CourseCategory.Conversational
        }

        // Default levels (can be customized per course if needed)
        val startingLevel = CEFRLevel.A1
        val targetLevel = when {
            courseSlug.contains("advanced") -> CEFRLevel.C1
            courseSlug.contains("intermediate") -> CEFRLevel.B2
            else -> CEFRLevel.B1
        }

        return CourseMetadata(languageCode, category, startingLevel, targetLevel)
    }

    data class CourseMetadata(
        val languageCode: String,
        val category: CourseCategory,
        val startingLevel: CEFRLevel,
        val targetLevel: CEFRLevel
    )

    data class MigrationReport(
        var coursesCreated: Int = 0,
        var skipped: Int = 0,
        var courseErrors: Int = 0,
        var lessonsCreated: Int = 0,
        var lessonErrors: Int = 0
    ) {
        fun summary(elapsedMs: Long): String {
            return """
                Migration Summary:
                  Courses created:     $coursesCreated
                  Courses skipped:     $skipped
                  Course errors:       $courseErrors
                  Lessons created:     $lessonsCreated
                  Lesson errors:       $lessonErrors
                  Total time:          ${elapsedMs}ms

                Status: ${if (courseErrors == 0 && lessonErrors == 0) "SUCCESS" else "COMPLETED WITH ERRORS"}
            """.trimIndent()
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(CourseMigrationApplication::class.java, *args)
}
