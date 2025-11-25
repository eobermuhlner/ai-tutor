package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CourseDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.CourseResponse
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.dto.LanguageResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorResponse
import ch.obermuhlner.aitutor.catalog.dto.UpdateTutorRequest
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.image.service.ImageService
import ch.obermuhlner.aitutor.language.service.LocalizationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService,
    private val imageService: ImageService,
    private val localizationService: LocalizationService,
    private val authorizationService: ch.obermuhlner.aitutor.auth.service.AuthorizationService,
) {
    private val objectMapper = jacksonObjectMapper()

    private fun generateTutorImageUrl(tutor: ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity): String? {
        val gender = tutor.gender ?: ch.obermuhlner.aitutor.core.model.catalog.TutorGender.Neutral
        val countryCode = tutor.targetLanguageCode.substringAfterLast("-").uppercase()
        val combinedText = "${tutor.location} ${tutor.personaEnglish}"

        return imageService.getImageUrlByPerson(
            countryCode = countryCode,
            gender = gender,
            age = tutor.age,
            text = combinedText
        )
    }

    @GetMapping("/languages")
    fun listLanguages(
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): List<LanguageResponse> {
        val languages = catalogService.getAvailableLanguages()
        return languages.map { lang ->
            val nameMap = objectMapper.readValue<Map<String, String>>(lang.nameJson)
            val descMap = objectMapper.readValue<Map<String, String>>(lang.descriptionJson)
            LanguageResponse(
                code = lang.code,
                name = nameMap[locale] ?: nameMap["en"] ?: lang.nativeName,
                flagEmoji = lang.flagEmoji,
                nativeName = lang.nativeName,
                difficulty = lang.difficulty,
                description = descMap[locale] ?: descMap["en"] ?: "",
                courseCount = catalogService.getCoursesForLanguage(lang.code).size,
                isActive = true  // All languages returned here are active (already filtered by service)
            )
        }
    }

    @GetMapping("/languages/{languageCode}/courses")
    fun listCourses(
        @PathVariable languageCode: String,
        @RequestParam(required = false, defaultValue = "en") locale: String,
        @RequestParam(required = false, defaultValue = "false") includeDrafts: Boolean
    ): List<CourseResponse> {
        val isEditorOrAdmin = try {
            authorizationService.isEditorOrAdmin()
        } catch (e: Exception) {
            false // If user is not authenticated or has no special roles, they're not an editor/admin
        }
        
        val courses = if (includeDrafts && isEditorOrAdmin) {
            // Only editors/admins can see drafts when explicitly requested
            catalogService.getCoursesForLanguage(languageCode)
        } else {
            // Everyone else only sees published courses (non-draft)
            catalogService.getCoursesForLanguage(languageCode)
                .filter { !it.isDraft }
        }
        
        return courses.map { course ->
            CourseResponse(
                id = course.id,
                languageCode = course.languageCode,
                name = localizationService.getLocalizedText(course.nameJson, locale, "Course", "en"),
                shortDescription = localizationService.getLocalizedText(course.shortDescriptionJson, locale, "Description", "en"),
                category = course.category,
                targetAudience = localizationService.getLocalizedText(course.targetAudienceJson, locale, "All levels", "en"),
                startingLevel = course.startingLevel,
                targetLevel = course.targetLevel,
                estimatedWeeks = course.estimatedWeeks,
                displayOrder = course.displayOrder
            )
        }
    }

    @GetMapping("/courses/{courseId}")
    fun getCourseDetail(
        @PathVariable courseId: UUID,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): CourseDetailResponse? {
        val course = catalogService.getCourseById(courseId) ?: return null
        
        // Check if user can access this course (editors/admins can access drafts, others only published)
        val isEditorOrAdmin = try {
            authorizationService.isEditorOrAdmin()
        } catch (e: Exception) {
            false // If user is not authenticated or has no special roles
        }
        
        if (course.isDraft && !isEditorOrAdmin) {
            return null // Non-editor users cannot access draft courses
        }
        
        // Get current user for tutor visibility filtering (null if not authenticated)
        val userId = try { authorizationService.getCurrentUserId() } catch (e: Exception) { null }
        val suggestedTutors = catalogService.getTutorsForCourse(courseId, userId).map { tutor ->
            TutorResponse(
                id = tutor.id,
                name = tutor.name,
                emoji = tutor.emoji,
                persona = localizationService.getLocalizedText(tutor.personaJson, locale, tutor.personaEnglish, "en"),
                domain = localizationService.getLocalizedText(tutor.domainJson, locale, tutor.domainEnglish, "en"),
                personality = tutor.personality,
                teachingStyle = tutor.teachingStyle,
                description = localizationService.getLocalizedText(tutor.descriptionJson, locale, tutor.descriptionEnglish, "en"),
                targetLanguageCode = tutor.targetLanguageCode,
                culturalBackground = tutor.culturalBackgroundJson?.let {
                    localizationService.getLocalizedText(it, locale, "", "en")
                },
                location = tutor.location,
                age = tutor.age,
                gender = tutor.gender,
                imageUrl = generateTutorImageUrl(tutor),
                displayOrder = tutor.displayOrder
            )
        }

        val topicSequence = course.topicSequenceJson?.let {
            objectMapper.readValue<List<String>>(it)
        }

        val learningGoalsMap = objectMapper.readValue<Map<String, List<String>>>(course.learningGoalsJson)
        val learningGoals = learningGoalsMap[locale] ?: learningGoalsMap["en"] ?: emptyList()

        val tags = course.tagsJson?.let {
            objectMapper.readValue<List<String>>(it)
        } ?: emptyList()

        return CourseDetailResponse(
            id = course.id,
            languageCode = course.languageCode,
            name = localizationService.getLocalizedText(course.nameJson, locale, "Course", "en"),
            shortDescription = localizationService.getLocalizedText(course.shortDescriptionJson, locale, "Description", "en"),
            description = localizationService.getLocalizedText(course.descriptionJson, locale, "Description", "en"),
            category = course.category,
            targetAudience = localizationService.getLocalizedText(course.targetAudienceJson, locale, "All levels", "en"),
            startingLevel = course.startingLevel,
            targetLevel = course.targetLevel,
            estimatedWeeks = course.estimatedWeeks,
            suggestedTutors = suggestedTutors,
            defaultPhase = course.defaultPhase,
            topicSequence = topicSequence,
            learningGoals = learningGoals,
            tags = tags,
            createdAt = course.createdAt ?: java.time.Instant.now(),
            updatedAt = course.updatedAt ?: java.time.Instant.now()
        )
    }

    @GetMapping("/languages/{languageCode}/tutors")
    fun listTutors(
        @PathVariable languageCode: String,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): List<TutorResponse> {
        // Get current user for tutor visibility filtering (null if not authenticated)
        val userId = try { authorizationService.getCurrentUserId() } catch (e: Exception) { null }
        return catalogService.getTutorsForLanguage(languageCode, userId).map { tutor ->
            TutorResponse(
                id = tutor.id,
                name = tutor.name,
                emoji = tutor.emoji,
                persona = localizationService.getLocalizedText(tutor.personaJson, locale, tutor.personaEnglish, "en"),
                domain = localizationService.getLocalizedText(tutor.domainJson, locale, tutor.domainEnglish, "en"),
                personality = tutor.personality,
                teachingStyle = tutor.teachingStyle,
                description = localizationService.getLocalizedText(tutor.descriptionJson, locale, tutor.descriptionEnglish, "en"),
                targetLanguageCode = tutor.targetLanguageCode,
                culturalBackground = tutor.culturalBackgroundJson?.let {
                    localizationService.getLocalizedText(it, locale, "", "en")
                },
                location = tutor.location,
                age = tutor.age,
                gender = tutor.gender,
                imageUrl = generateTutorImageUrl(tutor),
                displayOrder = tutor.displayOrder
            )
        }
    }

    @GetMapping("/tutors/{tutorId}")
    fun getTutorDetail(
        @PathVariable tutorId: UUID,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): TutorDetailResponse? {
        // Get current user for tutor visibility filtering (null if not authenticated)
        val userId = try { authorizationService.getCurrentUserId() } catch (e: Exception) { null }
        val tutor = catalogService.getTutorById(tutorId, userId) ?: return null
        return TutorDetailResponse(
            id = tutor.id,
            name = tutor.name,
            emoji = tutor.emoji,
            persona = localizationService.getLocalizedText(tutor.personaJson, locale, tutor.personaEnglish, "en"),
            domain = localizationService.getLocalizedText(tutor.domainJson, locale, tutor.domainEnglish, "en"),
            personality = tutor.personality,
            teachingStyle = tutor.teachingStyle,
            description = localizationService.getLocalizedText(tutor.descriptionJson, locale, tutor.descriptionEnglish, "en"),
            targetLanguageCode = tutor.targetLanguageCode,
            culturalBackground = tutor.culturalBackgroundJson?.let {
                localizationService.getLocalizedText(it, locale, "", "en")
            },
            location = tutor.location,
            age = tutor.age,
            gender = tutor.gender,
            imageUrl = generateTutorImageUrl(tutor),
            createdAt = tutor.createdAt ?: java.time.Instant.now(),
            updatedAt = tutor.updatedAt ?: java.time.Instant.now()
        )
    }

    @PostMapping("/tutors")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTutor(
        @RequestBody request: CreateTutorRequest,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): TutorDetailResponse {
        // Get authenticated user (required for creating tutors)
        val currentUserId = authorizationService.getCurrentUserId()
        val isAdmin = authorizationService.isAdmin()

        val tutor = catalogService.createTutor(request, currentUserId, isAdmin)
        return TutorDetailResponse(
            id = tutor.id,
            name = tutor.name,
            emoji = tutor.emoji,
            persona = localizationService.getLocalizedText(tutor.personaJson, locale, tutor.personaEnglish, "en"),
            domain = localizationService.getLocalizedText(tutor.domainJson, locale, tutor.domainEnglish, "en"),
            personality = tutor.personality,
            teachingStyle = tutor.teachingStyle,
            description = localizationService.getLocalizedText(tutor.descriptionJson, locale, tutor.descriptionEnglish, "en"),
            targetLanguageCode = tutor.targetLanguageCode,
            culturalBackground = tutor.culturalBackgroundJson?.let {
                localizationService.getLocalizedText(it, locale, "", "en")
            },
            location = tutor.location,
            age = tutor.age,
            gender = tutor.gender,
            imageUrl = generateTutorImageUrl(tutor),
            createdAt = tutor.createdAt ?: java.time.Instant.now(),
            updatedAt = tutor.updatedAt ?: java.time.Instant.now()
        )
    }

    @PutMapping("/tutors/{tutorId}")
    fun updateTutor(
        @PathVariable tutorId: UUID,
        @RequestBody request: UpdateTutorRequest,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): TutorDetailResponse {
        // Get authenticated user (required for updating tutors)
        val currentUserId = authorizationService.getCurrentUserId()
        val isAdmin = authorizationService.isAdmin()

        val tutor = catalogService.updateTutor(tutorId, request, currentUserId, isAdmin)
            ?: throw org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor not found")

        return TutorDetailResponse(
            id = tutor.id,
            name = tutor.name,
            emoji = tutor.emoji,
            persona = localizationService.getLocalizedText(tutor.personaJson, locale, tutor.personaEnglish, "en"),
            domain = localizationService.getLocalizedText(tutor.domainJson, locale, tutor.domainEnglish, "en"),
            personality = tutor.personality,
            teachingStyle = tutor.teachingStyle,
            description = localizationService.getLocalizedText(tutor.descriptionJson, locale, tutor.descriptionEnglish, "en"),
            targetLanguageCode = tutor.targetLanguageCode,
            culturalBackground = tutor.culturalBackgroundJson?.let {
                localizationService.getLocalizedText(it, locale, "", "en")
            },
            location = tutor.location,
            age = tutor.age,
            gender = tutor.gender,
            imageUrl = generateTutorImageUrl(tutor),
            createdAt = tutor.createdAt ?: java.time.Instant.now(),
            updatedAt = tutor.updatedAt ?: java.time.Instant.now()
        )
    }
}
