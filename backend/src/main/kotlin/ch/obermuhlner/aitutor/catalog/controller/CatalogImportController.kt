package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.SourceType
import ch.obermuhlner.aitutor.catalog.dto.CatalogImportResponse
import ch.obermuhlner.aitutor.catalog.repository.*
import ch.obermuhlner.aitutor.catalog.service.UnifiedCatalogImportService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * REST API for importing catalog data from unified YAML format.
 * Supports importing languages, tutors, and courses together.
 */
@RestController
@RequestMapping("/api/v1/catalog/import")
class CatalogImportController(
    private val unifiedCatalogImportService: UnifiedCatalogImportService,
    private val languageRepository: LanguageRepository,
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val lessonContentRepository: LessonContentRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val authorizationService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Import complete catalog from unified YAML format.
     *
     * POST /api/v1/catalog/import
     *
     * Request: multipart/form-data
     * - catalogFile: catalog.yml file (unified format)
     * - lessonFiles: optional .md lesson files for file-referenced lessons
     *
     * Response: CatalogImportResponse with import statistics
     *
     * Requires: ADMIN role
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Transactional
    fun importCatalog(
        @RequestParam("catalogFile") catalogFile: MultipartFile,
        @RequestParam("lessonFiles", required = false) lessonFiles: List<MultipartFile>?
    ): ResponseEntity<CatalogImportResponse> {
        authorizationService.requireAdmin()
        val currentUser = authorizationService.getCurrentUser()

        logger.info("User ${currentUser.id} importing catalog from unified format")

        // Validate catalog file
        val validationErrors = unifiedCatalogImportService.validateCatalog(
            catalogFile,
            lessonFiles ?: emptyList()
        )

        if (validationErrors.isNotEmpty()) {
            logger.warn("Catalog validation failed: $validationErrors")
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                    CatalogImportResponse(
                        languagesImported = 0,
                        tutorsImported = 0,
                        coursesImported = 0,
                        lessonsImported = 0,
                        errors = validationErrors,
                        success = false
                    )
                )
        }

        try {
            // Import catalog
            val result = unifiedCatalogImportService.importCatalog(
                catalogFile = catalogFile,
                lessonFiles = lessonFiles ?: emptyList(),
                sourceType = SourceType.UPLOADED
            )

            // Save languages (upsert by code)
            result.languages.forEach { language ->
                val existing = languageRepository.findById(language.code).orElse(null)
                if (existing != null) {
                    // Update existing language
                    existing.nameJson = language.nameJson
                    existing.flagEmoji = language.flagEmoji
                    existing.nativeName = language.nativeName
                    existing.difficulty = language.difficulty
                    existing.descriptionJson = language.descriptionJson
                    existing.isActive = language.isActive
                    existing.displayOrder = language.displayOrder
                    existing.updatedAt = java.time.Instant.now()
                    languageRepository.save(existing)
                    logger.info("Updated existing language: ${language.code}")
                } else {
                    // Create new language
                    languageRepository.save(language)
                    logger.info("Created new language: ${language.code}")
                }
            }

            // Save tutors
            tutorProfileRepository.saveAll(result.tutors)
            logger.info("Saved ${result.tutors.size} tutors")

            // Save courses and lessons
            courseTemplateRepository.saveAll(result.courses)
            logger.info("Saved ${result.courses.size} courses")

            lessonContentRepository.saveAll(result.lessons)
            logger.info("Saved ${result.lessons.size} lessons")

            // Create curriculum rules for courses with lessons
            val coursesWithLessons = result.lessons.groupBy { it.courseId }
            coursesWithLessons.forEach { (courseId, lessons) ->
                val existingRule = curriculumRuleRepository.findByCourseId(courseId)
                if (existingRule == null) {
                    val hasTimeBased = lessons.any { lesson ->
                        val minDays = lesson.minimumDays
                        minDays != null && minDays > 0
                    }
                    val rule = ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity(
                        courseId = courseId,
                        progressionMode = if (hasTimeBased) "TIME_BASED" else "LINEAR",
                        allowSkipping = false,
                        requireCompletion = true,
                        createdAt = java.time.Instant.now(),
                        updatedAt = java.time.Instant.now()
                    )
                    curriculumRuleRepository.save(rule)
                }
            }

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    CatalogImportResponse(
                        languagesImported = result.languages.size,
                        tutorsImported = result.tutors.size,
                        coursesImported = result.courses.size,
                        lessonsImported = result.lessons.size,
                        errors = result.errors,
                        success = true
                    )
                )
        } catch (e: Exception) {
            logger.error("Failed to import catalog", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    CatalogImportResponse(
                        languagesImported = 0,
                        tutorsImported = 0,
                        coursesImported = 0,
                        lessonsImported = 0,
                        errors = listOf("Import failed: ${e.message}"),
                        success = false
                    )
                )
        }
    }

    /**
     * Validate unified catalog file without importing.
     *
     * POST /api/v1/catalog/import/validate
     *
     * Request: multipart/form-data
     * - catalogFile: catalog.yml file
     * - lessonFiles: optional .md lesson files
     *
     * Response: Validation errors (empty if valid)
     *
     * Requires: ADMIN role
     */
    @PostMapping("/validate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun validateCatalog(
        @RequestParam("catalogFile") catalogFile: MultipartFile,
        @RequestParam("lessonFiles", required = false) lessonFiles: List<MultipartFile>?
    ): ResponseEntity<Map<String, Any>> {
        authorizationService.requireAdmin()

        val errors = unifiedCatalogImportService.validateCatalog(
            catalogFile,
            lessonFiles ?: emptyList()
        )

        return ResponseEntity.ok(
            mapOf(
                "valid" to errors.isEmpty(),
                "errors" to errors
            )
        )
    }
}
