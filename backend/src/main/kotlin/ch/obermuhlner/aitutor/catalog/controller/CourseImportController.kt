package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.dto.CourseImportRequest
import ch.obermuhlner.aitutor.catalog.dto.CourseImportResponse
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.catalog.service.FileImportService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

/**
 * REST API for importing courses from YAML and markdown files.
 * Replaces file-based seeding with API-based course uploads.
 */
@RestController
@RequestMapping("/api/v1/courses/import")
class CourseImportController(
    private val fileImportService: FileImportService,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val lessonContentRepository: LessonContentRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val authorizationService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Import complete course from curriculum.yml and lesson markdown files.
     *
     * POST /api/v1/courses/import/file
     *
     * Request: multipart/form-data
     * - curriculumFile: curriculum.yml file
     * - lessonFiles: multiple .md lesson files
     * - languageCode: language code (e.g., "de", "es", "ja")
     * - courseName: course name
     * - courseDescription: course description
     * - category: course category (optional, default: CONVERSATIONAL)
     * - startingLevel: starting CEFR level (optional, default: A1)
     * - targetLevel: target CEFR level (optional, default: B2)
     *
     * Response: CourseImportResponse with course ID and import status
     *
     * Requires: EDITOR or ADMIN role
     */
    @PostMapping("/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCourseFromFiles(
        @RequestParam("curriculumFile") curriculumFile: MultipartFile,
        @RequestParam("lessonFiles") lessonFiles: List<MultipartFile>,
        @ModelAttribute request: CourseImportRequest
    ): ResponseEntity<CourseImportResponse> {
        authorizationService.requireEditor()
        val currentUser = authorizationService.getCurrentUser()

        logger.info("User ${currentUser.id} importing course: ${request.courseName} (${request.languageCode})")

        // Validate files
        val validationErrors = fileImportService.validateImportFiles(curriculumFile, lessonFiles)
        if (validationErrors.isNotEmpty()) {
            logger.warn("Import validation failed: $validationErrors")
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                    CourseImportResponse(
                        courseId = java.util.UUID.randomUUID(),
                        courseName = request.courseName,
                        lessonsImported = 0,
                        errors = validationErrors,
                        success = false
                    )
                )
        }

        try {
            // Import course
            val result = fileImportService.importCourse(
                curriculumFile = curriculumFile,
                lessonFiles = lessonFiles,
                languageCode = request.languageCode,
                courseName = request.courseName,
                courseDescription = request.courseDescription,
                category = request.category,
                startingLevel = request.startingLevel,
                targetLevel = request.targetLevel,
                sourceType = SourceType.UPLOADED
            )

            // Save course
            val savedCourse = courseTemplateRepository.save(result.course)
            logger.info("Saved course ${savedCourse.id}")

            // Create curriculum rule
            val curriculumRule = CurriculumRuleEntity(
                courseId = savedCourse.id,
                progressionMode = result.lessons.firstOrNull()?.minimumDays?.let { "TIME_BASED" } ?: "LINEAR",
                allowSkipping = false,
                requireCompletion = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            curriculumRuleRepository.save(curriculumRule)

            // Save lessons
            lessonContentRepository.saveAll(result.lessons)
            logger.info("Saved ${result.lessons.size} lessons for course ${savedCourse.id}")

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    CourseImportResponse(
                        courseId = savedCourse.id,
                        courseName = request.courseName,
                        lessonsImported = result.lessons.size,
                        errors = result.errors,
                        success = true
                    )
                )
        } catch (e: Exception) {
            logger.error("Failed to import course: ${request.courseName}", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    CourseImportResponse(
                        courseId = java.util.UUID.randomUUID(),
                        courseName = request.courseName,
                        lessonsImported = 0,
                        errors = listOf("Import failed: ${e.message}"),
                        success = false
                    )
                )
        }
    }

    /**
     * Import lessons to existing course from markdown files.
     *
     * POST /api/v1/courses/{courseId}/lessons/import/file
     *
     * Request: multipart/form-data
     * - lessonFiles: multiple .md lesson files
     *
     * Response: Number of lessons imported
     *
     * Requires: EDITOR or ADMIN role
     */
    @PostMapping("/{courseId}/lessons/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importLessonsToExistingCourse(
        @PathVariable courseId: java.util.UUID,
        @RequestParam("lessonFiles") lessonFiles: List<MultipartFile>
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireEditor()
        val currentUser = authorizationService.getCurrentUser()

        logger.info("User ${currentUser.id} importing lessons to course $courseId")

        // Validate course exists
        val course = courseTemplateRepository.findById(courseId).orElse(null)
        if (course == null) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Course not found"))
        }

        // Check permissions
        if (!authorizationService.isAdmin() &&
            (!course.isDraft || course.lastEditedBy != currentUser.id)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not have permission to import lessons to this course"))
        }

        try {
            // Get current lesson count for display order
            val existingCount = lessonContentRepository.countByCourseId(courseId)
            val errors = mutableListOf<String>()

            // Parse and save lessons
            val newLessons = lessonFiles.mapIndexedNotNull { index, file ->
                try {
                    val parsed = fileImportService.parseLesson(file)
                    fileImportService.createLessonEntity(
                        courseId = courseId,
                        lessonParseResult = parsed,
                        displayOrder = existingCount.toInt() + index
                    )
                } catch (e: Exception) {
                    errors.add("Failed to parse ${file.originalFilename}: ${e.message}")
                    null
                }
            }

            lessonContentRepository.saveAll(newLessons)
            logger.info("Imported ${newLessons.size} lessons to course $courseId")

            return ResponseEntity.ok(
                mapOf(
                    "courseId" to courseId,
                    "lessonsImported" to newLessons.size,
                    "errors" to errors,
                    "success" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to import lessons to course $courseId", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    mapOf(
                        "error" to "Import failed: ${e.message}",
                        "success" to false
                    )
                )
        }
    }

    /**
     * Validate import files without actually importing.
     * Useful for client-side validation before upload.
     *
     * POST /api/v1/courses/import/validate
     *
     * Request: multipart/form-data
     * - curriculumFile: curriculum.yml file (optional)
     * - lessonFiles: multiple .md lesson files
     *
     * Response: Validation errors (empty if valid)
     */
    @PostMapping("/validate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun validateImportFiles(
        @RequestParam("curriculumFile", required = false) curriculumFile: MultipartFile?,
        @RequestParam("lessonFiles") lessonFiles: List<MultipartFile>
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireEditor()

        val errors = fileImportService.validateImportFiles(curriculumFile, lessonFiles)

        return ResponseEntity.ok(
            mapOf(
                "valid" to errors.isEmpty(),
                "errors" to errors
            )
        )
    }
}
