package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

/**
 * Service for importing courses and lessons from YAML and markdown files.
 * Supports migration from file-based content to database storage.
 */
@Service
class FileImportService(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    /**
     * Result of parsing curriculum.yml file.
     */
    data class CurriculumParseResult(
        val courseSlug: String,
        val curriculum: CourseCurriculum,
        val courseMetadata: Map<String, Any> = emptyMap()
    )

    /**
     * Result of parsing markdown lesson file.
     */
    data class LessonParseResult(
        val lessonId: String,
        val title: String,
        val frontmatter: Map<String, Any>,
        val markdown: String
    )

    /**
     * Result of importing complete course from files.
     */
    data class CourseImportResult(
        val course: CourseTemplateEntity,
        val lessons: List<LessonContentEntity>,
        val errors: List<String> = emptyList()
    )

    /**
     * Parse curriculum.yml file to extract course structure and lesson metadata.
     */
    fun parseCurriculum(file: MultipartFile): CurriculumParseResult {
        try {
            val curriculum = yamlMapper.readValue(file.inputStream, CourseCurriculum::class.java)
            logger.info("Parsed curriculum for course: ${curriculum.courseId} with ${curriculum.lessons.size} lessons")

            return CurriculumParseResult(
                courseSlug = curriculum.courseId,
                curriculum = curriculum
            )
        } catch (e: Exception) {
            logger.error("Failed to parse curriculum file: ${file.originalFilename}", e)
            throw IllegalArgumentException("Invalid curriculum.yml format: ${e.message}", e)
        }
    }

    /**
     * Parse curriculum.yml from string content.
     */
    fun parseCurriculumFromString(content: String, courseSlug: String): CurriculumParseResult {
        try {
            val curriculum = yamlMapper.readValue(content, CourseCurriculum::class.java)
            logger.info("Parsed curriculum for course: ${curriculum.courseId} with ${curriculum.lessons.size} lessons")

            return CurriculumParseResult(
                courseSlug = courseSlug,
                curriculum = curriculum
            )
        } catch (e: Exception) {
            logger.error("Failed to parse curriculum string for course $courseSlug", e)
            throw IllegalArgumentException("Invalid curriculum.yml format: ${e.message}", e)
        }
    }

    /**
     * Parse markdown lesson file to extract frontmatter and content.
     */
    fun parseLesson(file: MultipartFile): LessonParseResult {
        try {
            val markdown = file.inputStream.bufferedReader().readText()
            return parseLessonFromString(markdown, file.originalFilename ?: "unknown.md")
        } catch (e: Exception) {
            logger.error("Failed to parse lesson file: ${file.originalFilename}", e)
            throw IllegalArgumentException("Invalid lesson markdown format: ${e.message}", e)
        }
    }

    /**
     * Parse markdown lesson from string content.
     */
    fun parseLessonFromString(markdown: String, fileName: String): LessonParseResult {
        try {
            // Extract YAML frontmatter (between --- delimiters)
            val frontmatterRegex = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)
            val frontmatterMatch = frontmatterRegex.find(markdown)

            val frontmatter = if (frontmatterMatch != null) {
                try {
                    yamlMapper.readValue<Map<String, Any>>(frontmatterMatch.groupValues[1])
                } catch (e: Exception) {
                    logger.warn("Failed to parse frontmatter in $fileName", e)
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val lessonId = frontmatter["lessonId"] as? String
                ?: fileName.removeSuffix(".md")
            val title = frontmatter["title"] as? String
                ?: "Untitled Lesson"

            logger.debug("Parsed lesson: $lessonId - $title")

            return LessonParseResult(
                lessonId = lessonId,
                title = title,
                frontmatter = frontmatter,
                markdown = markdown
            )
        } catch (e: Exception) {
            logger.error("Failed to parse lesson markdown from $fileName", e)
            throw IllegalArgumentException("Invalid lesson markdown format: ${e.message}", e)
        }
    }

    /**
     * Create CourseTemplateEntity from curriculum metadata.
     * This is a helper for creating courses during import.
     */
    fun createCourseFromCurriculum(
        curriculum: CourseCurriculum,
        languageCode: String,
        courseName: String,
        courseDescription: String = "Imported course",
        category: CourseCategory = CourseCategory.Conversational,
        startingLevel: CEFRLevel = CEFRLevel.A1,
        targetLevel: CEFRLevel = CEFRLevel.B2,
        sourceType: SourceType = SourceType.UPLOADED
    ): CourseTemplateEntity {
        return CourseTemplateEntity(
            languageCode = languageCode,
            nameJson = """{"en": "$courseName"}""",
            shortDescriptionJson = """{"en": "Imported from curriculum file"}""",
            descriptionJson = """{"en": "$courseDescription"}""",
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
            isDraft = true,  // Imported courses start as drafts
            sourceType = sourceType
        )
    }

    /**
     * Create LessonContentEntity from parsed lesson and curriculum metadata.
     */
    fun createLessonEntity(
        courseId: UUID,
        lessonParseResult: LessonParseResult,
        displayOrder: Int,
        minimumDays: Int? = null,
        requiredTurns: Int? = null
    ): LessonContentEntity {
        return LessonContentEntity(
            courseId = courseId,
            lessonId = lessonParseResult.lessonId,
            title = lessonParseResult.title,
            content = lessonParseResult.markdown,
            displayOrder = displayOrder,
            minimumDays = minimumDays,
            requiredTurns = requiredTurns,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    /**
     * Import complete course from curriculum.yml and lesson markdown files.
     * This is the main import workflow.
     */
    fun importCourse(
        curriculumFile: MultipartFile,
        lessonFiles: List<MultipartFile>,
        languageCode: String,
        courseName: String,
        courseDescription: String = "Imported course",
        category: CourseCategory = CourseCategory.Conversational,
        startingLevel: CEFRLevel = CEFRLevel.A1,
        targetLevel: CEFRLevel = CEFRLevel.B2,
        sourceType: SourceType = SourceType.UPLOADED
    ): CourseImportResult {
        val errors = mutableListOf<String>()

        try {
            // 1. Parse curriculum
            val curriculumResult = parseCurriculum(curriculumFile)
            val curriculum = curriculumResult.curriculum

            // 2. Create course entity
            val course = createCourseFromCurriculum(
                curriculum = curriculum,
                languageCode = languageCode,
                courseName = courseName,
                courseDescription = courseDescription,
                category = category,
                startingLevel = startingLevel,
                targetLevel = targetLevel,
                sourceType = sourceType
            )

            // 3. Parse all lesson files
            val lessonParsed = mutableMapOf<String, LessonParseResult>()
            for (lessonFile in lessonFiles) {
                try {
                    val parsed = parseLesson(lessonFile)
                    lessonParsed[parsed.lessonId] = parsed
                } catch (e: Exception) {
                    errors.add("Failed to parse ${lessonFile.originalFilename}: ${e.message}")
                }
            }

            // 4. Create lesson entities matching curriculum order
            val lessons = curriculum.lessons.mapIndexed { index, metadata ->
                val parsed = lessonParsed[metadata.id]
                if (parsed == null) {
                    errors.add("Missing lesson file for ${metadata.id}")
                    null
                } else {
                    createLessonEntity(
                        courseId = course.id,
                        lessonParseResult = parsed,
                        displayOrder = index,
                        minimumDays = metadata.minimumDays,
                        requiredTurns = metadata.requiredTurns
                    )
                }
            }.filterNotNull()

            logger.info("Imported course ${course.id} with ${lessons.size} lessons (${errors.size} errors)")

            return CourseImportResult(
                course = course,
                lessons = lessons,
                errors = errors
            )
        } catch (e: Exception) {
            logger.error("Failed to import course", e)
            throw IllegalArgumentException("Course import failed: ${e.message}", e)
        }
    }

    /**
     * Validate course import files before processing.
     * Returns list of validation errors (empty if valid).
     */
    fun validateImportFiles(
        curriculumFile: MultipartFile?,
        lessonFiles: List<MultipartFile>
    ): List<String> {
        val errors = mutableListOf<String>()

        // Validate curriculum file
        if (curriculumFile == null || curriculumFile.isEmpty) {
            errors.add("Curriculum file is required")
        } else if (curriculumFile.originalFilename != "curriculum.yml") {
            errors.add("Curriculum file must be named 'curriculum.yml'")
        }

        // Validate lesson files
        if (lessonFiles.isEmpty()) {
            errors.add("At least one lesson markdown file is required")
        }

        lessonFiles.forEach { file ->
            if (!file.originalFilename.orEmpty().endsWith(".md")) {
                errors.add("Lesson file ${file.originalFilename} must have .md extension")
            }
        }

        return errors
    }
}
