package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.dto.CurriculumRequest
import ch.obermuhlner.aitutor.catalog.dto.CurriculumResponse
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/courses/{courseId}/curriculum")
@Tag(name = "Curriculum Management", description = "Endpoints for managing curriculum rules (editor role required)")
class CurriculumController(
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    @Operation(summary = "Get curriculum rules", description = "Gets the curriculum rules for a course. Requires editor role.")
    fun getCurriculum(@PathVariable courseId: UUID): ResponseEntity<CurriculumResponse> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        val curriculumRule = curriculumRuleRepository.findByCourseId(courseId)
            ?: throw IllegalArgumentException("No curriculum rules found for course: $courseId")
        
        return ResponseEntity.ok(mapToCurriculumResponse(curriculumRule))
    }

    @PutMapping
    @Operation(summary = "Update curriculum settings", description = "Updates the curriculum settings for a course. Requires editor role.")
    fun updateCurriculum(@PathVariable courseId: UUID, @RequestBody request: CurriculumRequest): ResponseEntity<CurriculumResponse> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        val existingRule = curriculumRuleRepository.findByCourseId(courseId)
        
        val curriculumRule = if (existingRule != null) {
            // Update existing rule
            existingRule.progressionMode = request.progressionMode
            existingRule.allowSkipping = request.allowSkipping
            existingRule.requireCompletion = request.requireCompletion
            existingRule.updatedAt = Instant.now()
            curriculumRuleRepository.save(existingRule)
        } else {
            // Create new rule
            val newRule = CurriculumRuleEntity(
                courseId = courseId,
                progressionMode = request.progressionMode,
                allowSkipping = request.allowSkipping,
                requireCompletion = request.requireCompletion,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            curriculumRuleRepository.save(newRule)
        }
        
        return ResponseEntity.ok(mapToCurriculumResponse(curriculumRule))
    }

    private fun validateCourseOwnership(courseId: UUID) {
        val course = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        val currentUser = authorizationService.getCurrentUser()
        
        // Editors can only edit their own draft courses, admins can edit any
        if (!authorizationService.isAdmin() && 
            (!course.isDraft || course.lastEditedBy != currentUser.id)) {
            throw IllegalArgumentException("You do not have permission to edit this course")
        }
    }

    private fun mapToCurriculumResponse(rule: CurriculumRuleEntity): CurriculumResponse {
        return CurriculumResponse(
            id = rule.id,
            courseId = rule.courseId,
            progressionMode = rule.progressionMode,
            allowSkipping = rule.allowSkipping,
            requireCompletion = rule.requireCompletion,
            createdAt = rule.createdAt,
            updatedAt = rule.updatedAt
        )
    }
}