package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import ch.obermuhlner.aitutor.lesson.domain.ProgressionMode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LessonProgressionService(
    private val lessonContentService: LessonContentService,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val catalogService: CatalogService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun checkAndProgressLesson(session: ChatSessionEntity): LessonContent? {
        // Convert UUID to course slug identifier
        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null

        val currentLessonId = session.currentLessonId

        // First session message - activate first lesson
        if (currentLessonId == null) {
            return activateFirstLesson(session, curriculum)
        }

        // Check if should advance to next lesson
        val progression = calculateProgression(session, curriculum, currentLessonId)
        if (progression.shouldAdvance) {
            return advanceToNextLesson(session, curriculum, currentLessonId)
        }

        // Continue with current lesson
        return lessonContentService.getLesson(curriculum.courseId, currentLessonId)
    }

    fun forceAdvanceLesson(session: ChatSessionEntity): LessonContent? {
        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null
        val currentLessonId = session.currentLessonId ?: return null

        return advanceToNextLesson(session, curriculum, currentLessonId)
    }

    private fun activateFirstLesson(
        session: ChatSessionEntity,
        curriculum: CourseCurriculum
    ): LessonContent? {
        val firstLesson = curriculum.lessons.firstOrNull() ?: return null
        session.currentLessonId = firstLesson.id
        session.lessonStartedAt = Instant.now()
        session.lessonProgressJson = """{"turnCount": 0}"""
        chatSessionRepository.save(session)

        logger.info("Activated first lesson for session ${session.id}: ${firstLesson.id}")

        return lessonContentService.getLesson(curriculum.courseId, firstLesson.id)
    }

    private fun calculateProgression(
        session: ChatSessionEntity,
        curriculum: CourseCurriculum,
        currentLessonId: String
    ): ProgressionResult {
        val metadata = curriculum.lessons.find { it.id == currentLessonId } ?: return ProgressionResult(false)

        // Check time criteria
        val daysElapsed = Duration.between(
            session.lessonStartedAt ?: Instant.now(),
            Instant.now()
        ).toDays()

        // Check turn count
        val turnCount = chatMessageRepository.countBySessionId(session.id)
        val progress = parseProgress(session.lessonProgressJson)

        val shouldAdvance = when (curriculum.progressionMode) {
            ProgressionMode.TIME_BASED ->
                daysElapsed >= metadata.unlockAfterDays && turnCount >= metadata.requiredTurns
            ProgressionMode.COMPLETION_BASED ->
                turnCount >= metadata.requiredTurns && progress.goalsCompleted
        }

        return ProgressionResult(shouldAdvance)
    }

    private fun advanceToNextLesson(
        session: ChatSessionEntity,
        curriculum: CourseCurriculum,
        currentLessonId: String
    ): LessonContent? {
        val currentIndex = curriculum.lessons.indexOfFirst { it.id == currentLessonId }
        val nextLesson = curriculum.lessons.getOrNull(currentIndex + 1) ?: return null

        session.currentLessonId = nextLesson.id
        session.lessonStartedAt = Instant.now()
        session.lessonProgressJson = """{"turnCount": 0}"""
        chatSessionRepository.save(session)

        logger.info("Advanced session ${session.id} to lesson ${nextLesson.id}")

        return lessonContentService.getLesson(curriculum.courseId, nextLesson.id)
    }

    private fun parseProgress(json: String?): LessonProgress {
        if (json == null) return LessonProgress(0, false)
        return try {
            val map = objectMapper.readValue<Map<String, Any>>(json)
            LessonProgress(
                turnCount = (map["turnCount"] as? Number)?.toInt() ?: 0,
                goalsCompleted = (map["goalsCompleted"] as? Boolean) ?: false
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse lesson progress JSON: $json", e)
            LessonProgress(0, false)
        }
    }

    // Helper: Map UUID to course slug for file system lookup
    private fun getCourseSlug(courseTemplateId: UUID?): String? {
        if (courseTemplateId == null) return null

        val course = catalogService.getCourseById(courseTemplateId) ?: return null

        // Parse English name from JSON
        val nameEnglish = try {
            val nameMap = objectMapper.readValue<Map<String, String>>(course.nameJson)
            nameMap["en"] ?: "unknown"
        } catch (e: Exception) {
            logger.warn("Failed to parse course name JSON: ${course.nameJson}", e)
            "unknown"
        }

        // Generate slug from language code (ISO part only) and course name
        // Example: "es-ES" + "Conversational Spanish" -> "es-conversational-spanish"
        // Extract language part (before hyphen) to match filesystem structure
        val languageOnly = course.languageCode.lowercase().substringBefore("-")
        return "$languageOnly-${nameEnglish.lowercase().replace(" ", "-")}"
    }
}

data class ProgressionResult(val shouldAdvance: Boolean)
data class LessonProgress(val turnCount: Int, val goalsCompleted: Boolean)
