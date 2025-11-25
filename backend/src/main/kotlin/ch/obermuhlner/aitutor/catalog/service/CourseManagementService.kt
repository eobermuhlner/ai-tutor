package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.auth.exception.InsufficientPermissionsException
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import ch.obermuhlner.aitutor.catalog.dto.*
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class CourseManagementService(
    private val courseTemplateRepository: CourseTemplateRepository,
    private val curriculumRuleRepository: CurriculumRuleRepository,
    private val authorizationService: AuthorizationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createCourse(request: CreateCourseRequest): CourseManagementResponse {
        authorizationService.requireEditor()
        
        val currentUser = authorizationService.getCurrentUser()
        
        val course = CourseTemplateEntity(
            languageCode = request.languageCode,
            nameJson = request.nameJson,
            shortDescriptionJson = request.shortDescriptionJson,
            descriptionJson = request.descriptionJson,
            category = request.category,
            targetAudienceJson = request.targetAudienceJson,
            startingLevel = request.startingLevel,
            targetLevel = request.targetLevel,
            estimatedWeeks = request.estimatedWeeks,
            suggestedTutorIdsJson = request.suggestedTutorIdsJson,
            defaultPhase = request.defaultPhase,
            topicSequenceJson = request.topicSequenceJson,
            learningGoalsJson = request.learningGoalsJson,
            tagsJson = request.tagsJson,
            isDraft = true,  // New courses are created as drafts by default
            lastEditedBy = currentUser.id
        )
        
        val savedCourse = courseTemplateRepository.save(course)
        logger.info("Created new course ${savedCourse.id} as draft by user ${currentUser.id}")
        
        // Create default curriculum rule for the new course
        val curriculumRule = CurriculumRuleEntity(
            courseId = savedCourse.id,
            progressionMode = "LINEAR",  // Default progression mode
            allowSkipping = false,
            requireCompletion = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        curriculumRuleRepository.save(curriculumRule)
        
        return mapToCourseResponse(savedCourse)
    }

    fun updateCourse(courseId: UUID, request: UpdateCourseRequest): CourseManagementResponse {
        authorizationService.requireEditor()
        
        val currentUser = authorizationService.getCurrentUser()
        val existingCourse = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        // Check if user can edit this course (editors can only edit their own draft courses, admins can edit any)
        if (!authorizationService.isAdmin() && 
            (!existingCourse.isDraft || existingCourse.lastEditedBy != currentUser.id)) {
            throw InsufficientPermissionsException("You do not have permission to edit this course")
        }
        
        // Update fields if provided in request
        if (request.nameJson != null) existingCourse.nameJson = request.nameJson
        if (request.shortDescriptionJson != null) existingCourse.shortDescriptionJson = request.shortDescriptionJson
        if (request.descriptionJson != null) existingCourse.descriptionJson = request.descriptionJson
        if (request.category != null) existingCourse.category = request.category
        if (request.targetAudienceJson != null) existingCourse.targetAudienceJson = request.targetAudienceJson
        if (request.startingLevel != null) existingCourse.startingLevel = request.startingLevel
        if (request.targetLevel != null) existingCourse.targetLevel = request.targetLevel
        if (request.estimatedWeeks != null) existingCourse.estimatedWeeks = request.estimatedWeeks
        if (request.suggestedTutorIdsJson != null) existingCourse.suggestedTutorIdsJson = request.suggestedTutorIdsJson
        if (request.defaultPhase != null) existingCourse.defaultPhase = request.defaultPhase
        if (request.topicSequenceJson != null) existingCourse.topicSequenceJson = request.topicSequenceJson
        if (request.learningGoalsJson != null) existingCourse.learningGoalsJson = request.learningGoalsJson
        if (request.tagsJson != null) existingCourse.tagsJson = request.tagsJson
        
        existingCourse.lastEditedBy = currentUser.id
        existingCourse.version = existingCourse.version + 1  // Increment version
        
        val updatedCourse = courseTemplateRepository.save(existingCourse)
        logger.info("Updated course ${updatedCourse.id} by user ${currentUser.id}")
        
        return mapToCourseResponse(updatedCourse)
    }

    fun publishCourse(courseId: UUID): CourseManagementResponse {
        authorizationService.requireEditor()
        
        val currentUser = authorizationService.getCurrentUser()
        val existingCourse = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        // Check if user can publish this course (editors can only publish their own draft courses, admins can publish any)
        if (!authorizationService.isAdmin() && 
            (!existingCourse.isDraft || existingCourse.lastEditedBy != currentUser.id)) {
            throw InsufficientPermissionsException("You do not have permission to publish this course")
        }
        
        existingCourse.isDraft = false
        existingCourse.publishedAt = Instant.now()
        existingCourse.lastEditedBy = currentUser.id
        existingCourse.version = existingCourse.version + 1  // Increment version
        
        val publishedCourse = courseTemplateRepository.save(existingCourse)
        logger.info("Published course ${publishedCourse.id} by user ${currentUser.id}")
        
        return mapToCourseResponse(publishedCourse)
    }

    fun unpublishCourse(courseId: UUID): CourseManagementResponse {
        authorizationService.requireEditor()
        
        val currentUser = authorizationService.getCurrentUser()
        val existingCourse = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        // Check if user can unpublish this course (editors can only unpublish their own courses, admins can unpublish any)
        if (!authorizationService.isAdmin() && existingCourse.lastEditedBy != currentUser.id) {
            throw InsufficientPermissionsException("You do not have permission to unpublish this course")
        }
        
        // Only published courses can be unpublished
        if (existingCourse.isDraft) {
            throw IllegalArgumentException("Course is already a draft")
        }
        
        existingCourse.isDraft = true
        existingCourse.publishedAt = null
        existingCourse.lastEditedBy = currentUser.id
        existingCourse.version = existingCourse.version + 1  // Increment version
        
        val unpublishedCourse = courseTemplateRepository.save(existingCourse)
        logger.info("Unpublished course ${unpublishedCourse.id} by user ${currentUser.id}")
        
        return mapToCourseResponse(unpublishedCourse)
    }

    fun deleteCourse(courseId: UUID) {
        authorizationService.requireEditor()
        
        val currentUser = authorizationService.getCurrentUser()
        val existingCourse = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        // Editors can only delete their own draft courses, admins can delete any
        if (!authorizationService.isAdmin() && 
            (!existingCourse.isDraft || existingCourse.lastEditedBy != currentUser.id)) {
            throw InsufficientPermissionsException("You do not have permission to delete this course")
        }
        
        // Delete associated curriculum rules
        curriculumRuleRepository.deleteByCourseId(courseId)
        
        courseTemplateRepository.delete(existingCourse)
        logger.info("Deleted course $courseId by user ${currentUser.id}")
    }

    fun getAllCourses(includeDrafts: Boolean): List<CourseManagementResponse> {
        val courses = if (!includeDrafts) {
            // Only return published courses to non-editors
            if (authorizationService.isEditorOrAdmin()) {
                courseTemplateRepository.findAll()
            } else {
                courseTemplateRepository.findAll().filter { !it.isDraft }
            }
        } else {
            // Only editors and admins can see all courses (including drafts)
            if (authorizationService.isEditorOrAdmin()) {
                courseTemplateRepository.findAll()
            } else {
                courseTemplateRepository.findAll().filter { !it.isDraft }
            }
        }

        return courses.map { mapToCourseResponse(it) }
    }

    fun getCourseById(courseId: UUID): CourseManagementResponse {
        val course = courseTemplateRepository.findById(courseId)
            .orElseThrow { IllegalArgumentException("Course not found: $courseId") }
        
        // Check if user can access this course (editors/admins can access all, others only published)
        if (!authorizationService.isEditorOrAdmin() && course.isDraft) {
            throw InsufficientPermissionsException("You do not have permission to access this draft course")
        }
        
        return mapToCourseResponse(course)
    }

    private fun mapToCourseResponse(course: CourseTemplateEntity): CourseManagementResponse {
        return CourseManagementResponse(
            id = course.id,
            languageCode = course.languageCode,
            nameJson = course.nameJson,
            shortDescriptionJson = course.shortDescriptionJson,
            descriptionJson = course.descriptionJson,
            category = course.category,
            targetAudienceJson = course.targetAudienceJson,
            startingLevel = course.startingLevel,
            targetLevel = course.targetLevel,
            estimatedWeeks = course.estimatedWeeks,
            suggestedTutorIdsJson = course.suggestedTutorIdsJson,
            defaultPhase = course.defaultPhase,
            topicSequenceJson = course.topicSequenceJson,
            learningGoalsJson = course.learningGoalsJson,
            isActive = course.isActive,
            displayOrder = course.displayOrder,
            tagsJson = course.tagsJson,
            createdAt = course.createdAt,
            updatedAt = course.updatedAt,
            isDraft = course.isDraft,
            publishedAt = course.publishedAt,
            lastEditedBy = course.lastEditedBy,
            version = course.version
        )
    }
}