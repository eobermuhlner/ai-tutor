package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import ch.obermuhlner.aitutor.catalog.dto.LessonRequest
import ch.obermuhlner.aitutor.catalog.dto.LessonResponse
import ch.obermuhlner.aitutor.catalog.dto.ReorderLessonsRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons")
@Tag(name = "Lesson Management", description = "Endpoints for managing lessons (editor role required)")
class LessonManagementController(
    private val lessonContentRepository: LessonContentRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    @Operation(summary = "List all lessons", description = "Lists all lessons for a course. Requires editor role.")
    fun getLessons(@PathVariable courseId: UUID): ResponseEntity<List<LessonResponse>> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        val lessons = lessonContentRepository.findByCourseId(courseId)
            .sortedBy { it.displayOrder }
            .map { mapToLessonResponse(it) }
        
        return ResponseEntity.ok(lessons)
    }

    @PostMapping
    @Operation(summary = "Create new lesson", description = "Creates a new lesson for a course. Requires editor role.")
    fun createLesson(@PathVariable courseId: UUID, @RequestBody request: LessonRequest): ResponseEntity<LessonResponse> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        // Check if lesson ID already exists
        if (lessonContentRepository.existsByCourseIdAndLessonId(courseId, request.lessonId)) {
            throw IllegalArgumentException("Lesson with ID '${request.lessonId}' already exists in course $courseId")
        }
        
        val lesson = LessonContentEntity(
            courseId = courseId,
            lessonId = request.lessonId,
            title = request.title,
            content = request.content,
            displayOrder = request.displayOrder,
            minimumDays = request.minimumDays,
            requiredTurns = request.requiredTurns,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val savedLesson = lessonContentRepository.save(lesson)
        
        return ResponseEntity.ok(mapToLessonResponse(savedLesson))
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Update lesson", description = "Updates an existing lesson. Requires editor role.")
    fun updateLesson(
        @PathVariable courseId: UUID,
        @PathVariable lessonId: String,
        @RequestBody request: LessonRequest
    ): ResponseEntity<LessonResponse> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        val existingLesson = lessonContentRepository.findByCourseIdAndLessonId(courseId, lessonId)
            ?: throw IllegalArgumentException("Lesson with ID '$lessonId' not found in course $courseId")
        
        existingLesson.title = request.title
        existingLesson.content = request.content
        existingLesson.displayOrder = request.displayOrder
        existingLesson.minimumDays = request.minimumDays
        existingLesson.requiredTurns = request.requiredTurns
        existingLesson.updatedAt = Instant.now()
        
        val updatedLesson = lessonContentRepository.save(existingLesson)
        
        return ResponseEntity.ok(mapToLessonResponse(updatedLesson))
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Delete lesson", description = "Deletes a lesson. Requires editor role.")
    fun deleteLesson(@PathVariable courseId: UUID, @PathVariable lessonId: String): ResponseEntity<Unit> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        val lesson = lessonContentRepository.findByCourseIdAndLessonId(courseId, lessonId)
            ?: throw IllegalArgumentException("Lesson with ID '$lessonId' not found in course $courseId")
        
        lessonContentRepository.delete(lesson)
        
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder lessons", description = "Updates the display order of multiple lessons. Requires editor role.")
    fun reorderLessons(@PathVariable courseId: UUID, @RequestBody request: ReorderLessonsRequest): ResponseEntity<Unit> {
        authorizationService.requireEditor()
        
        validateCourseOwnership(courseId)
        
        request.lessons.forEach { lessonOrder ->
            val lesson = lessonContentRepository.findById(lessonOrder.id)
                .orElseThrow { IllegalArgumentException("Lesson with ID '${lessonOrder.id}' not found") }
            
            // Verify the lesson belongs to the correct course
            if (lesson.courseId != courseId) {
                throw IllegalArgumentException("Lesson with ID '${lessonOrder.id}' does not belong to course $courseId")
            }
            
            lesson.displayOrder = lessonOrder.displayOrder
            lesson.updatedAt = Instant.now()
            lessonContentRepository.save(lesson)
        }
        
        return ResponseEntity.ok().build()
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

    private fun mapToLessonResponse(lesson: LessonContentEntity): LessonResponse {
        return LessonResponse(
            id = lesson.id,
            courseId = lesson.courseId,
            lessonId = lesson.lessonId,
            title = lesson.title,
            content = lesson.content,
            displayOrder = lesson.displayOrder,
            minimumDays = lesson.minimumDays,
            requiredTurns = lesson.requiredTurns,
            createdAt = lesson.createdAt,
            updatedAt = lesson.updatedAt
        )
    }
}