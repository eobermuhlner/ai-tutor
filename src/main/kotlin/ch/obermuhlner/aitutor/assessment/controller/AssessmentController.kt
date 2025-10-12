package ch.obermuhlner.aitutor.assessment.controller

import ch.obermuhlner.aitutor.assessment.dto.SkillBreakdownResponse
import ch.obermuhlner.aitutor.assessment.service.CEFRAssessmentService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assessment")
class AssessmentController(
    private val cefrAssessmentService: CEFRAssessmentService,
    private val chatSessionRepository: ChatSessionRepository,
    private val authorizationService: AuthorizationService
) {

    /**
     * Get CEFR skill breakdown for a session.
     * GET /api/v1/assessment/sessions/{id}/skills
     */
    @GetMapping("/sessions/{sessionId}/skills")
    fun getSkillBreakdown(
        @PathVariable sessionId: UUID
    ): ResponseEntity<SkillBreakdownResponse> {
        val userId = authorizationService.getCurrentUserId()
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        // Validate ownership
        if (session.userId != userId) {
            return ResponseEntity.status(403).build()
        }

        return ResponseEntity.ok(
            SkillBreakdownResponse(
                overall = session.estimatedCEFRLevel.name,
                grammar = session.cefrGrammar?.name ?: "Unknown",
                vocabulary = session.cefrVocabulary?.name ?: "Unknown",
                fluency = session.cefrFluency?.name ?: "Unknown",
                comprehension = session.cefrComprehension?.name ?: "Unknown",
                lastAssessedAt = session.lastAssessmentAt,
                assessmentCount = session.totalAssessmentCount ?: 0
            )
        )
    }

    /**
     * Trigger manual reassessment (for testing/debugging).
     * POST /api/v1/assessment/sessions/{id}/reassess
     */
    @PostMapping("/sessions/{sessionId}/reassess")
    fun triggerReassessment(
        @PathVariable sessionId: UUID
    ): ResponseEntity<SkillBreakdownResponse> {
        val userId = authorizationService.getCurrentUserId()
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        // Validate ownership
        if (session.userId != userId) {
            return ResponseEntity.status(403).build()
        }

        // Force reassessment
        val assessment = cefrAssessmentService.assessWithHeuristics(session)
        cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)
        chatSessionRepository.save(session)

        return getSkillBreakdown(sessionId)
    }
}
