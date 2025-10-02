package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.util.*

@DataJpaTest
class CourseTemplateRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var courseTemplateRepository: CourseTemplateRepository

    @Test
    fun `should find courses by language code and active status ordered by display order`() {
        // Given
        val course1 = createCourseTemplate("es", "Travel Spanish", CourseCategory.Travel, 1, true)
        val course2 = createCourseTemplate("es", "Business Spanish", CourseCategory.Business, 0, true)
        val course3 = createCourseTemplate("fr", "French Basics", CourseCategory.General, 0, true)
        val course4 = createCourseTemplate("es", "Inactive Course", CourseCategory.General, 2, false)

        entityManager.persist(course1)
        entityManager.persist(course2)
        entityManager.persist(course3)
        entityManager.persist(course4)
        entityManager.flush()

        // When
        val result = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("es")

        // Then
        assertEquals(2, result.size)
        assertEquals("Business Spanish", result[0].nameJson)
        assertEquals("Travel Spanish", result[1].nameJson)
    }

    @Test
    fun `should find courses by category and active status`() {
        // Given
        val travelCourse1 = createCourseTemplate("es", "Spanish Travel", CourseCategory.Travel, 0, true)
        val travelCourse2 = createCourseTemplate("fr", "French Travel", CourseCategory.Travel, 1, true)
        val businessCourse = createCourseTemplate("es", "Spanish Business", CourseCategory.Business, 0, true)

        entityManager.persist(travelCourse1)
        entityManager.persist(travelCourse2)
        entityManager.persist(businessCourse)
        entityManager.flush()

        // When
        val result = courseTemplateRepository.findByCategoryAndIsActiveTrue(CourseCategory.Travel)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == CourseCategory.Travel })
    }

    @Test
    fun `should find courses with starting level less than or equal to given level`() {
        // Given
        val beginnerCourse = createCourseTemplate("es", "Beginner", CourseCategory.General, 0, true, CEFRLevel.A1)
        val intermediateCourse = createCourseTemplate("es", "Intermediate", CourseCategory.General, 1, true, CEFRLevel.B1)
        val advancedCourse = createCourseTemplate("es", "Advanced", CourseCategory.General, 2, true, CEFRLevel.C1)

        entityManager.persist(beginnerCourse)
        entityManager.persist(intermediateCourse)
        entityManager.persist(advancedCourse)
        entityManager.flush()

        // When
        val result = courseTemplateRepository.findByStartingLevelLessThanEqualAndIsActiveTrue(CEFRLevel.B1)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.nameJson == "Beginner" })
        assertTrue(result.any { it.nameJson == "Intermediate" })
        assertFalse(result.any { it.nameJson == "Advanced" })
    }

    @Test
    fun `should return empty list when no courses match criteria`() {
        // Given
        val spanishCourse = createCourseTemplate("es", "Spanish", CourseCategory.General, 0, true)
        entityManager.persist(spanishCourse)
        entityManager.flush()

        // When
        val result = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder("fr")

        // Then
        assertTrue(result.isEmpty())
    }

    private fun createCourseTemplate(
        languageCode: String,
        name: String,
        category: CourseCategory,
        displayOrder: Int,
        isActive: Boolean,
        startingLevel: CEFRLevel = CEFRLevel.A1
    ): CourseTemplateEntity {
        return CourseTemplateEntity(
            id = UUID.randomUUID(),
            languageCode = languageCode,
            nameJson = name,
            shortDescriptionJson = """{"en": "Short description"}""",
            descriptionJson = """{"en": "Full description"}""",
            category = category,
            targetAudienceJson = """{"en": "Beginners"}""",
            startingLevel = startingLevel,
            targetLevel = CEFRLevel.B2,
            estimatedWeeks = 8,
            suggestedTutorIdsJson = null,
            defaultPhase = ConversationPhase.Auto,
            topicSequenceJson = null,
            learningGoalsJson = """{"en": ["Goal 1", "Goal 2"]}""",
            isActive = isActive,
            displayOrder = displayOrder,
            tagsJson = null
        )
    }
}
