package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseTemplateRepository : JpaRepository<CourseTemplateEntity, UUID> {
    fun findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<CourseTemplateEntity>
    fun findByCategoryAndIsActiveTrue(category: CourseCategory): List<CourseTemplateEntity>
    fun findByStartingLevelLessThanEqualAndIsActiveTrue(level: CEFRLevel): List<CourseTemplateEntity>
}
