package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CatalogServiceImpl(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val supportedLanguages: Map<String, LanguageMetadata>,
    private val objectMapper: ObjectMapper
) : CatalogService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAvailableLanguages(): List<LanguageMetadata> {
        return supportedLanguages.values.toList()
    }

    override fun getLanguageByCode(code: String): LanguageMetadata? {
        return supportedLanguages[code]
    }

    override fun getCoursesForLanguage(languageCode: String, userLevel: CEFRLevel?): List<CourseTemplateEntity> {
        logger.debug("Fetching courses for language: $languageCode, userLevel: $userLevel")
        val courses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode)

        // Filter by user level if provided
        val filtered = if (userLevel != null) {
            courses.filter { it.startingLevel.ordinal <= userLevel.ordinal }
        } else {
            courses
        }
        logger.debug("Found ${filtered.size} courses for $languageCode")
        return filtered
    }

    override fun getCourseById(courseId: UUID): CourseTemplateEntity? {
        return courseTemplateRepository.findById(courseId).orElse(null)
    }

    override fun getCoursesByCategory(category: CourseCategory): List<CourseTemplateEntity> {
        return courseTemplateRepository.findByCategoryAndIsActiveTrue(category)
    }

    override fun searchCourses(query: String, languageCode: String?): List<CourseTemplateEntity> {
        // Simple search implementation - can be enhanced with full-text search
        val allCourses = if (languageCode != null) {
            courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode)
        } else {
            courseTemplateRepository.findAll()
        }

        return allCourses.filter { course ->
            course.nameJson.contains(query, ignoreCase = true) ||
            course.descriptionJson.contains(query, ignoreCase = true) ||
            course.category.name.contains(query, ignoreCase = true)
        }
    }

    override fun getTutorsForLanguage(targetLanguageCode: String): List<TutorProfileEntity> {
        return tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(targetLanguageCode)
    }

    override fun getTutorById(tutorId: UUID): TutorProfileEntity? {
        return tutorProfileRepository.findById(tutorId).orElse(null)
    }

    override fun getTutorsForCourse(courseTemplateId: UUID): List<TutorProfileEntity> {
        val course = getCourseById(courseTemplateId) ?: return emptyList()

        // Get all tutors for this language
        val allTutors = getTutorsForLanguage(course.languageCode)

        // Parse suggested tutor IDs from JSON
        val suggestedIds = course.suggestedTutorIdsJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<List<UUID>>() {})
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        // If no suggested tutors, return all tutors
        if (suggestedIds.isEmpty()) {
            return allTutors
        }

        // Return suggested tutors first (in order), then remaining tutors
        val suggestedTutors = suggestedIds.mapNotNull { id -> allTutors.find { it.id == id } }
        val remainingTutors = allTutors.filter { it.id !in suggestedIds }

        return suggestedTutors + remainingTutors
    }

    override fun createTutor(request: CreateTutorRequest): TutorProfileEntity {
        logger.info("Creating new tutor: ${request.name} for language ${request.targetLanguageCode}")

        // Create JSON with English as default locale
        val personaJson = objectMapper.writeValueAsString(mapOf("en" to request.personaEnglish))
        val domainJson = objectMapper.writeValueAsString(mapOf("en" to request.domainEnglish))
        val descriptionJson = objectMapper.writeValueAsString(mapOf("en" to request.descriptionEnglish))
        val culturalBackgroundJson = request.culturalBackground?.let {
            objectMapper.writeValueAsString(mapOf("en" to it))
        }

        val tutor = TutorProfileEntity(
            name = request.name,
            emoji = request.emoji,
            personaEnglish = request.personaEnglish,
            domainEnglish = request.domainEnglish,
            descriptionEnglish = request.descriptionEnglish,
            personaJson = personaJson,
            domainJson = domainJson,
            descriptionJson = descriptionJson,
            culturalBackgroundJson = culturalBackgroundJson,
            personality = request.personality,
            teachingStyle = request.teachingStyle,
            targetLanguageCode = request.targetLanguageCode,
            isActive = request.isActive,
            displayOrder = request.displayOrder
        )

        val saved = tutorProfileRepository.save(tutor)
        logger.info("Created tutor with ID: ${saved.id}")
        return saved
    }
}
