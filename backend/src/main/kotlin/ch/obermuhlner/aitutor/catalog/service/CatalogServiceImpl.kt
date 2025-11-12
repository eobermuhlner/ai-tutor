package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.LanguageEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.catalog.dto.UpdateTutorRequest
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.LanguageRepository
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
    private val languageRepository: LanguageRepository,
    private val objectMapper: ObjectMapper
) : CatalogService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getAvailableLanguages(): List<LanguageMetadata> {
        val languages = languageRepository.findByIsActiveTrue()
        return languages.map { toLanguageMetadata(it) }
    }

    override fun getLanguageByCode(code: String): LanguageMetadata? {
        val language = languageRepository.findById(code).orElse(null)
        return if (language != null && language.isActive) {
            toLanguageMetadata(language)
        } else {
            null
        }
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

    override fun getTutorsForLanguage(targetLanguageCode: String, userId: UUID?): List<TutorProfileEntity> {
        return if (userId != null) {
            // Filter by visibility: global tutors + user's own custom tutors
            tutorProfileRepository.findVisibleTutorsForUser(targetLanguageCode, userId)
        } else {
            // No user context: return only global tutors
            tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueAndIsGlobalTrueOrderByDisplayOrder(targetLanguageCode)
        }
    }

    override fun getTutorById(tutorId: UUID, userId: UUID?): TutorProfileEntity? {
        val tutor = tutorProfileRepository.findById(tutorId).orElse(null) ?: return null

        // Check visibility: global tutors are visible to all, custom tutors only to owner
        val isVisible = tutor.isGlobal || (userId != null && tutor.createdByUserId == userId)

        return if (isVisible) tutor else null
    }

    override fun getTutorsForCourse(courseTemplateId: UUID, userId: UUID?): List<TutorProfileEntity> {
        val course = getCourseById(courseTemplateId) ?: return emptyList()

        // Get all tutors for this language (filtered by visibility)
        val allTutors = getTutorsForLanguage(course.languageCode, userId)

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

    override fun createTutor(request: CreateTutorRequest, creatorUserId: UUID, isAdminRequest: Boolean): TutorProfileEntity {
        logger.info("Creating new tutor: ${request.name} for language ${request.targetLanguageCode}")

        // Determine if tutor should be global
        // Admin can explicitly request global tutor, otherwise user-specific
        val isGlobal = if (isAdminRequest && request.isGlobal == true) {
            logger.info("Admin creating global tutor")
            true
        } else {
            logger.info("Creating user-specific tutor for user $creatorUserId")
            false
        }

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
            location = request.location,
            age = request.age,
            gender = request.gender,
            personality = request.personality,
            teachingStyle = request.teachingStyle,
            targetLanguageCode = request.targetLanguageCode,
            isActive = request.isActive,
            displayOrder = request.displayOrder,
            isGlobal = isGlobal,
            createdByUserId = creatorUserId
        )

        val saved = tutorProfileRepository.save(tutor)
        logger.info("Created tutor with ID: ${saved.id}, isGlobal: $isGlobal")
        return saved
    }

    override fun updateTutor(tutorId: UUID, request: UpdateTutorRequest, userId: UUID, isAdmin: Boolean): TutorProfileEntity? {
        logger.info("Updating tutor: $tutorId for user $userId, isAdmin: $isAdmin")

        val existingTutor = tutorProfileRepository.findById(tutorId).orElse(null) ?: return null

        // Check authorization - users can only update their own tutors unless they're admin
        if (!isAdmin && existingTutor.createdByUserId != userId) {
            logger.warn("User $userId attempted to update tutor $tutorId they don't own")
            return null
        }

        // Only update fields that are provided in the request
        if (request.name != null) existingTutor.name = request.name
        if (request.emoji != null) existingTutor.emoji = request.emoji
        if (request.personaEnglish != null) existingTutor.personaEnglish = request.personaEnglish
        if (request.domainEnglish != null) existingTutor.domainEnglish = request.domainEnglish
        if (request.descriptionEnglish != null) existingTutor.descriptionEnglish = request.descriptionEnglish

        // Update multilingual JSON fields if persona/description/domain are updated
        if (request.personaEnglish != null) {
            existingTutor.personaJson = objectMapper.writeValueAsString(mapOf<String, String>("en" to request.personaEnglish))
        }
        if (request.domainEnglish != null) {
            existingTutor.domainJson = objectMapper.writeValueAsString(mapOf<String, String>("en" to request.domainEnglish))
        }
        if (request.descriptionEnglish != null) {
            existingTutor.descriptionJson = objectMapper.writeValueAsString(mapOf<String, String>("en" to request.descriptionEnglish))
        }
        if (request.culturalBackground != null) {
            existingTutor.culturalBackgroundJson = objectMapper.writeValueAsString(mapOf<String, String>("en" to request.culturalBackground))
        } else {
            // If culturalBackground is null in the request, clear it
            existingTutor.culturalBackgroundJson = null
        }

        if (request.location != null) existingTutor.location = request.location
        if (request.age != null) existingTutor.age = request.age
        if (request.gender != null) existingTutor.gender = request.gender
        if (request.personality != null) existingTutor.personality = request.personality
        if (request.teachingStyle != null) existingTutor.teachingStyle = request.teachingStyle
        if (request.targetLanguageCode != null) existingTutor.targetLanguageCode = request.targetLanguageCode
        if (request.isActive != null) existingTutor.isActive = request.isActive
        if (request.displayOrder != null) existingTutor.displayOrder = request.displayOrder

        val updatedTutor = tutorProfileRepository.save(existingTutor)
        logger.info("Updated tutor with ID: ${updatedTutor.id}")
        return updatedTutor
    }

    private fun toLanguageMetadata(language: LanguageEntity): LanguageMetadata {
        return LanguageMetadata(
            code = language.code,
            nameJson = language.nameJson,
            flagEmoji = language.flagEmoji,
            nativeName = language.nativeName,
            difficulty = language.difficulty,
            descriptionJson = language.descriptionJson
        )
    }
}