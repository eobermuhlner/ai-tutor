package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.dto.CreateTutorRequest
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import java.util.UUID

interface CatalogService {
    // Language metadata from configuration
    fun getAvailableLanguages(): List<LanguageMetadata>
    fun getLanguageByCode(code: String): LanguageMetadata?

    // Course browsing
    fun getCoursesForLanguage(languageCode: String, userLevel: CEFRLevel? = null): List<CourseTemplateEntity>
    fun getCourseById(courseId: UUID): CourseTemplateEntity?
    fun getCoursesByCategory(category: CourseCategory): List<CourseTemplateEntity>
    fun searchCourses(query: String, languageCode: String? = null): List<CourseTemplateEntity>

    // Tutor browsing
    fun getTutorsForLanguage(targetLanguageCode: String): List<TutorProfileEntity>
    fun getTutorById(tutorId: UUID): TutorProfileEntity?
    fun getTutorsForCourse(courseTemplateId: UUID): List<TutorProfileEntity>

    // Tutor management
    fun createTutor(request: CreateTutorRequest): TutorProfileEntity
}
