package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CreateCourseRequest
import ch.obermuhlner.aitutor.catalog.dto.CourseManagementResponse
import ch.obermuhlner.aitutor.catalog.dto.UpdateCourseRequest
import ch.obermuhlner.aitutor.catalog.service.CourseManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Course Management", description = "Endpoints for managing courses (editor role required)")
class CourseManagementController(
    private val courseManagementService: CourseManagementService
) {

    @PostMapping
    @Operation(summary = "Create new course", description = "Creates a new course as a draft. Requires editor role.")
    fun createCourse(@RequestBody request: CreateCourseRequest): ResponseEntity<CourseManagementResponse> {
        val course = courseManagementService.createCourse(request)
        return ResponseEntity.ok(course)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course", description = "Updates course metadata. Requires editor role.")
    fun updateCourse(@PathVariable id: UUID, @RequestBody request: UpdateCourseRequest): ResponseEntity<CourseManagementResponse> {
        val course = courseManagementService.updateCourse(id, request)
        return ResponseEntity.ok(course)
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a draft course", description = "Publishes a draft course, making it visible to all users. Requires editor role.")
    fun publishCourse(@PathVariable id: UUID): ResponseEntity<CourseManagementResponse> {
        val course = courseManagementService.publishCourse(id)
        return ResponseEntity.ok(course)
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a course", description = "Reverts a published course back to draft. Requires editor role.")
    fun unpublishCourse(@PathVariable id: UUID): ResponseEntity<CourseManagementResponse> {
        val course = courseManagementService.unpublishCourse(id)
        return ResponseEntity.ok(course)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course", description = "Deletes a course. Editors can only delete their own draft courses, admins can delete any. Requires editor role.")
    fun deleteCourse(@PathVariable id: UUID): ResponseEntity<Unit> {
        courseManagementService.deleteCourse(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @Operation(summary = "List all courses", description = "Lists all courses. Non-editors only see published courses. With includeDrafts=true, editors see all courses.")  
    fun getAllCourses(
        @RequestParam(defaultValue = "false", required = false) includeDrafts: Boolean
    ): ResponseEntity<List<CourseManagementResponse>> {
        val courses = courseManagementService.getAllCourses(includeDrafts)
        return ResponseEntity.ok(courses)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID", description = "Gets a specific course. Draft courses are only accessible to editors. Requires editor role for draft courses.")
    fun getCourseById(@PathVariable id: UUID): ResponseEntity<CourseManagementResponse> {
        val course = courseManagementService.getCourseById(id)
        return ResponseEntity.ok(course)
    }
}