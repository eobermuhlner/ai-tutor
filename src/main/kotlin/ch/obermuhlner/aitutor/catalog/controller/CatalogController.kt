package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.CourseDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.CourseResponse
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.dto.LanguageResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorDetailResponse
import ch.obermuhlner.aitutor.catalog.dto.TutorResponse
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.language.service.LocalizationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService,
    private val localizationService: LocalizationService
) {
    private val objectMapper = jacksonObjectMapper()

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
                courseCount = catalogService.getCoursesForLanguage(lang.code).size
            )
        }
    }

    @GetMapping("/languages/{languageCode}/courses")
    fun listCourses(
        @PathVariable languageCode: String,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): List<CourseResponse> {
        return catalogService.getCoursesForLanguage(languageCode).map { course ->
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
        val suggestedTutors = catalogService.getTutorsForCourse(courseId).map { tutor ->
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
                age = tutor.age,
                imageUrl = "/api/v1/images/tutor/${tutor.id}/data",
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
        return catalogService.getTutorsForLanguage(languageCode).map { tutor ->
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
                age = tutor.age,
                imageUrl = "/api/v1/images/tutor/${tutor.id}/data",
                displayOrder = tutor.displayOrder
            )
        }
    }

    @GetMapping("/tutors/{tutorId}")
    fun getTutorDetail(
        @PathVariable tutorId: UUID,
        @RequestParam(required = false, defaultValue = "en") locale: String
    ): TutorDetailResponse? {
        val tutor = catalogService.getTutorById(tutorId) ?: return null
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
            age = tutor.age,
            imageUrl = "/api/v1/images/tutor/${tutor.id}/data",
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
        val tutor = catalogService.createTutor(request)
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
            age = tutor.age,
            imageUrl = "/api/v1/images/tutor/${tutor.id}/data",
            createdAt = tutor.createdAt ?: java.time.Instant.now(),
            updatedAt = tutor.updatedAt ?: java.time.Instant.now()
        )
    }
}
