package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.lesson.domain.CourseCurriculum
import ch.obermuhlner.aitutor.lesson.domain.LessonContent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LessonProgressionService(
    private val lessonContentService: LessonContentService,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val catalogService: CatalogService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Gets the current lesson for a session without checking progression.
     * Use this when you just need the lesson content for display/context.
     * Use checkAndProgressLesson() when you want to also check if lesson should advance.
     */
    fun getCurrentLesson(sessionId: UUID): LessonContent? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null
        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null

        val currentLessonId = session.currentLessonId

        // If no current lesson, return first lesson (but don't activate it)
        if (currentLessonId == null) {
            return curriculum.lessons.firstOrNull()?.let {
                lessonContentService.getLesson(curriculum.courseId, it.id)
            }
        }

        // Return current lesson
        return lessonContentService.getLesson(curriculum.courseId, currentLessonId)
    }

    @Transactional
    fun checkAndProgressLesson(sessionId: UUID): LessonContent? {
        // Reload session within transaction to avoid stale entity and lock contention
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

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

    @Transactional
    fun forceAdvanceLesson(sessionId: UUID): LessonContent? {
        // Reload session within transaction to avoid stale entity and lock contention
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null
        val currentLessonId = session.currentLessonId ?: return null

        return advanceToNextLesson(session, curriculum, currentLessonId)
    }

    @Transactional
    fun navigateToNextLesson(sessionId: UUID): LessonContent? {
        // Reload session within transaction to avoid stale entity and lock contention
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null
        val currentLessonId = session.currentLessonId ?: return null

        return advanceToNextLesson(session, curriculum, currentLessonId)
    }

    @Transactional
    fun navigateToPreviousLesson(sessionId: UUID): LessonContent? {
        // Reload session within transaction to avoid stale entity and lock contention
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null
        val currentLessonId = session.currentLessonId ?: return null

        return advanceToPreviousLesson(session, curriculum, currentLessonId)
    }

    @Transactional
    fun navigateToSpecificLesson(sessionId: UUID, targetLessonId: String): LessonContent? {
        // Reload session within transaction to avoid stale entity and lock contention
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null

        val courseSlug = getCourseSlug(session.courseTemplateId) ?: return null
        val curriculum = lessonContentService.getCurriculum(courseSlug) ?: return null

        // Check if the target lesson exists in the curriculum
        val targetLesson = curriculum.lessons.find { it.id == targetLessonId }
        if (targetLesson == null) {
            logger.warn("Target lesson $targetLessonId not found in curriculum for session $sessionId")
            return null
        }

        // Update session to target lesson
        session.currentLessonId = targetLessonId
        session.lessonStartedAt = Instant.now()
        session.lessonProgressTurnCount = 0
        session.lessonProgressGoalsCompleted = false
        chatSessionRepository.save(session)

        logger.info("Navigated session ${session.id} to specific lesson $targetLessonId")

        return lessonContentService.getLesson(curriculum.courseId, targetLessonId)
    }

    private fun activateFirstLesson(
        session: ChatSessionEntity,
        curriculum: CourseCurriculum
    ): LessonContent? {
        val firstLesson = curriculum.lessons.firstOrNull() ?: return null
        session.currentLessonId = firstLesson.id
        session.lessonStartedAt = Instant.now()
        session.lessonProgressTurnCount = 0
        session.lessonProgressGoalsCompleted = false
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

        // Get turn count and goals completed from lesson progress fields (lesson-specific, not total session messages)
        val turnCount = session.lessonProgressTurnCount
        val goalsCompleted = session.lessonProgressGoalsCompleted

        val shouldAdvance = turnCount >= metadata.requiredTurns && goalsCompleted

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
        session.lessonProgressTurnCount = 0
        session.lessonProgressGoalsCompleted = false
        chatSessionRepository.save(session)

        logger.info("Advanced session ${session.id} to lesson ${nextLesson.id}")

        return lessonContentService.getLesson(curriculum.courseId, nextLesson.id)
    }

    private fun advanceToPreviousLesson(
        session: ChatSessionEntity,
        curriculum: CourseCurriculum,
        currentLessonId: String
    ): LessonContent? {
        val currentIndex = curriculum.lessons.indexOfFirst { it.id == currentLessonId }
        val previousLesson = if (currentIndex > 0) curriculum.lessons[currentIndex - 1] else null
        if (previousLesson == null) {
            logger.info("No previous lesson available for session ${session.id}, already at first lesson")
            return null
        }

        session.currentLessonId = previousLesson.id
        session.lessonStartedAt = Instant.now()
        session.lessonProgressTurnCount = 0
        session.lessonProgressGoalsCompleted = false
        chatSessionRepository.save(session)

        logger.info("Advanced session ${session.id} to previous lesson ${previousLesson.id}")

        return lessonContentService.getLesson(curriculum.courseId, previousLesson.id)
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
