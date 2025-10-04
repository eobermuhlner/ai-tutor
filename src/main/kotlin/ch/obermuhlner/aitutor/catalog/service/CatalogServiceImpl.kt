package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

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

        // Parse suggested tutor IDs from JSON
        val suggestedIds = course.suggestedTutorIdsJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<List<UUID>>() {})
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        // If no suggested tutors, return all tutors for the language
        if (suggestedIds.isEmpty()) {
            return getTutorsForLanguage(course.languageCode)
        }

        // Return tutors in suggested order
        return suggestedIds.mapNotNull { getTutorById(it) }
    }
}
