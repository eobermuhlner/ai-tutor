package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.LessonMetadata
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class LessonContentService(
    private val objectMapper: ObjectMapper,
    private val lessonContentRepository: LessonContentRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lessonCache = ConcurrentHashMap<String, LessonContent>()
    private val curriculumCache = ConcurrentHashMap<String, CourseCurriculum>()

    // YAML mapper for curriculum.yml files
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun getLesson(courseId: String, lessonId: String): LessonContent? {
        val cacheKey = "$courseId/$lessonId"
        // computeIfAbsent requires non-null value, so we need special handling
        val cached = lessonCache[cacheKey]
        if (cached != null) return cached

        // First try to find in database, but only if courseId looks like a UUID
        val courseEntity = if (isValidUUID(courseId)) {
            try {
                courseTemplateRepository.findById(UUID.fromString(courseId))
            } catch (e: Exception) {
                // If parsing fails, continue with file system lookup
                null
            }
        } else {
            null
        }
        
        if (courseEntity?.isPresent == true) {
            val course = courseEntity.get()
            // For published courses, prioritize database content
            if (!course.isDraft) {
                val lessonEntity = lessonContentRepository.findByCourseIdAndLessonId(course.id, lessonId)
                if (lessonEntity != null) {
                    val lessonContent = parseLessonFromEntity(lessonEntity)
                    lessonCache[cacheKey] = lessonContent
                    return lessonContent
                }
            }
        }
        
        // If not found in database or courseId is not a UUID, try filesystem
        val loaded = loadLessonFromFile(courseId, lessonId)
        if (loaded != null) {
            lessonCache[cacheKey] = loaded
        }
        return loaded
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun parseLessonFromEntity(lessonEntity: ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity): LessonContent {
        // Extract information from the database entity's markdown content
        // This is similar to the existing parseLesson method but adapted for database content
        val markdown = lessonEntity.content

        // Extract YAML frontmatter (between --- delimiters)
        val frontmatterRegex = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)
        val frontmatterMatch = frontmatterRegex.find(markdown)

        val frontmatter = if (frontmatterMatch != null) {
            try {
                yamlMapper.readValue<Map<String, Any>>(frontmatterMatch.groupValues[1])
            } catch (e: Exception) {
                logger.warn("Failed to parse frontmatter for lesson ${lessonEntity.lessonId}", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }

        return LessonContent(
            id = lessonEntity.lessonId,
            title = lessonEntity.title,
            weekNumber = null, // Extract from frontmatter if available
            estimatedDuration = frontmatter["estimatedDuration"] as? String,
            focusAreas = (frontmatter["focusAreas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            targetCEFR = CEFRLevel.valueOf(frontmatter["targetCEFR"] as? String ?: "A1"),
            fullMarkdown = lessonEntity.content
        )
    }

    private fun loadLessonFromFile(courseId: String, lessonId: String): LessonContent? {
        return try {
            val resource = ClassPathResource("course-content/$courseId/$lessonId.md")
            if (!resource.exists()) {
                logger.warn("Lesson file not found: $courseId/$lessonId.md")
                return null
            }
            val markdown = resource.inputStream.bufferedReader().readText()
            parseLesson(lessonId, markdown)
        } catch (e: Exception) {
            logger.error("Failed to load lesson $lessonId for course $courseId", e)
            null
        }
    }

    private fun parseLesson(lessonId: String, markdown: String): LessonContent {
        // Extract YAML frontmatter (between --- delimiters)
        val frontmatterRegex = Regex("""^---\s*\n(.*?)\n---\s*\n""", RegexOption.DOT_MATCHES_ALL)
        val frontmatterMatch = frontmatterRegex.find(markdown)

        val frontmatter = if (frontmatterMatch != null) {
            yamlMapper.readValue<Map<String, Any>>(frontmatterMatch.groupValues[1])
        } else {
            emptyMap()
        }

        return LessonContent(
            id = frontmatter["lessonId"] as? String ?: lessonId,
            title = frontmatter["title"] as? String ?: "Untitled",
            weekNumber = (frontmatter["weekNumber"] as? Number)?.toInt(),
            estimatedDuration = frontmatter["estimatedDuration"] as? String,
            focusAreas = (frontmatter["focusAreas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            targetCEFR = CEFRLevel.valueOf(frontmatter["targetCEFR"] as? String ?: "A1"),
            fullMarkdown = markdown
        )
    }


    fun getCurriculum(courseId: String): CourseCurriculum? {
        // Check cache first
        val cached = curriculumCache[courseId]
        if (cached != null) return cached

        // Try to load from database first (if courseId is a UUID)
        if (isValidUUID(courseId)) {
            try {
                val courseUuid = UUID.fromString(courseId)
                val curriculumFromDb = loadCurriculumFromDatabase(courseUuid)
                if (curriculumFromDb != null) {
                    curriculumCache[courseId] = curriculumFromDb
                    return curriculumFromDb
                }
            } catch (e: Exception) {
                logger.warn("Failed to load curriculum from database for course $courseId", e)
            }
        }

        // Fall back to file system
        val loaded = try {
            val resource = ClassPathResource("course-content/$courseId/curriculum.yml")
            if (!resource.exists()) {
                logger.warn("Curriculum file not found: $courseId/curriculum.yml")
                return null
            }
            yamlMapper.readValue(resource.inputStream, CourseCurriculum::class.java)
        } catch (e: Exception) {
            logger.error("Failed to load curriculum for course $courseId", e)
            return null
        }

        // Cache and return
        if (loaded != null) {
            curriculumCache[courseId] = loaded
        }
        return loaded
    }

    /**
     * Load curriculum from database entities.
     * Reconstructs CourseCurriculum from CurriculumRuleEntity and LessonContentEntity.
     */
    private fun loadCurriculumFromDatabase(courseId: UUID): CourseCurriculum? {
        // Get curriculum rules
        val curriculumRule = curriculumRuleRepository.findByCourseId(courseId) ?: return null

        // Get all lessons for this course, ordered by displayOrder
        val lessons = lessonContentRepository.findByCourseIdOrderByDisplayOrder(courseId)
        if (lessons.isEmpty()) {
            logger.warn("No lessons found in database for course $courseId")
            return null
        }

        // Convert to LessonMetadata
        val lessonMetadata = lessons.map { lesson ->
            LessonMetadata(
                id = lesson.lessonId,
                file = "${lesson.lessonId}.md",  // Synthetic file name for compatibility
                minimumDays = lesson.minimumDays ?: 7,
                requiredTurns = lesson.requiredTurns ?: 20
            )
        }

        // Convert progression mode
        val progressionMode = when (curriculumRule.progressionMode) {
            "TIME_BASED" -> ProgressionMode.TIME_BASED
            "COMPLETION_BASED" -> ProgressionMode.COMPLETION_BASED
            else -> ProgressionMode.TIME_BASED
        }

        logger.debug("Loaded curriculum from database for course $courseId with ${lessonMetadata.size} lessons")

        return CourseCurriculum(
            courseId = courseId.toString(),
            progressionMode = progressionMode,
            lessons = lessonMetadata
        )
    }
}
