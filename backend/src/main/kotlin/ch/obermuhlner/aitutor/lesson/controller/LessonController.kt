package ch.obermuhlner.aitutor.lesson.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.lesson.dto.CourseCurriculumResponse
import ch.obermuhlner.aitutor.lesson.dto.GrammarPointResponse
import ch.obermuhlner.aitutor.lesson.dto.LessonContentResponse
import ch.obermuhlner.aitutor.lesson.dto.LessonMetadataResponse
import ch.obermuhlner.aitutor.lesson.dto.ScenarioResponse
import ch.obermuhlner.aitutor.lesson.dto.VocabEntryResponse
import ch.obermuhlner.aitutor.lesson.service.LessonContentService
import ch.obermuhlner.aitutor.lesson.service.LessonProgressionService
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/lessons")
class LessonController(
    private val lessonContentService: LessonContentService,
    private val lessonProgressionService: LessonProgressionService,
    private val chatSessionRepository: ChatSessionRepository,
    private val authorizationService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/courses/{courseId}/curriculum")
    fun getCourseCurriculum(
        @PathVariable courseId: String
    ): ResponseEntity<CourseCurriculumResponse> {
        logger.debug("Get curriculum request: courseId=$courseId")

        val curriculum = lessonContentService.getCurriculum(courseId)
            ?: return ResponseEntity.notFound().build()

        val response = CourseCurriculumResponse(
            courseId = curriculum.courseId,
            progressionMode = curriculum.progressionMode,
            lessons = curriculum.lessons.map { metadata ->
                LessonMetadataResponse(
                    id = metadata.id,
                    file = metadata.file,
                    minimumDays = metadata.minimumDays,
                    requiredTurns = metadata.requiredTurns
                )
            }
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    fun getLesson(
        @PathVariable courseId: String,
        @PathVariable lessonId: String
    ): ResponseEntity<LessonContentResponse> {
        logger.debug("Get lesson request: courseId=$courseId, lessonId=$lessonId")

        val lesson = lessonContentService.getLesson(courseId, lessonId)
            ?: return ResponseEntity.notFound().build()

        val response = toLessonContentResponse(lesson)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/sessions/{sessionId}/current")
    fun getCurrentLesson(
        @PathVariable sessionId: UUID
    ): ResponseEntity<LessonContentResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        logger.debug("Get current lesson request: sessionId=$sessionId, userId=$currentUserId")

        val session = chatSessionRepository.findById(sessionId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Verify ownership
        if (session.userId != currentUserId) {
            return ResponseEntity.status(403).build()
        }

        // Check if session is course-based
        if (session.courseTemplateId == null) {
            return ResponseEntity.badRequest().build()
        }

        val lesson = lessonProgressionService.checkAndProgressLesson(sessionId)
            ?: return ResponseEntity.notFound().build()

        val response = toLessonContentResponse(lesson)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/sessions/{sessionId}/advance")
    fun advanceLesson(
        @PathVariable sessionId: UUID
    ): ResponseEntity<LessonContentResponse> {
        val currentUserId = authorizationService.getCurrentUserId()
        logger.debug("Advance lesson request: sessionId=$sessionId, userId=$currentUserId")

        val session = chatSessionRepository.findById(sessionId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Verify ownership
        if (session.userId != currentUserId) {
            return ResponseEntity.status(403).build()
        }

        // Check if session is course-based
        if (session.courseTemplateId == null) {
            return ResponseEntity.badRequest().build()
        }

        val nextLesson = lessonProgressionService.forceAdvanceLesson(sessionId)
            ?: return ResponseEntity.notFound().build()

        val response = toLessonContentResponse(nextLesson)
        return ResponseEntity.ok(response)
    }

    private fun toLessonContentResponse(lesson: ch.obermuhlner.aitutor.lesson.domain.LessonContent): LessonContentResponse {
        return LessonContentResponse(
            id = lesson.id,
            title = lesson.title,
            weekNumber = lesson.weekNumber,
            estimatedDuration = lesson.estimatedDuration,
            focusAreas = lesson.focusAreas,
            targetCEFR = lesson.targetCEFR,
            goals = lesson.goals,
            grammarPoints = lesson.grammarPoints.map { grammar ->
                GrammarPointResponse(
                    title = grammar.title,
                    rule = grammar.rule,
                    examples = grammar.examples,
                    patterns = grammar.patterns
                )
            },
            essentialVocabulary = lesson.essentialVocabulary.map { vocab ->
                VocabEntryResponse(
                    word = vocab.word,
                    translation = vocab.translation,
                    contextExample = vocab.contextExample
                )
            },
            conversationScenarios = lesson.conversationScenarios.map { scenario ->
                ScenarioResponse(
                    title = scenario.title,
                    dialogue = scenario.dialogue
                )
            },
            practicePatterns = lesson.practicePatterns,
            commonMistakes = lesson.commonMistakes,
            fullMarkdown = lesson.fullMarkdown
        )
    }
}
