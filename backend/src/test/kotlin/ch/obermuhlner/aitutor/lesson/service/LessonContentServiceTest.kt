package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.LessonContentRepository
import ch.obermuhlner.aitutor.catalog.repository.CurriculumRuleRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class LessonContentServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val lessonContentRepository: LessonContentRepository = mockk()
    private val courseTemplateRepository: CourseTemplateRepository = mockk()
    private val curriculumRuleRepository: CurriculumRuleRepository = mockk()

    init {
        // By default, return null/empty from database to fallback to file-based system
        io.mockk.every { lessonContentRepository.findByCourseIdAndLessonId(any(), any()) } returns null
        io.mockk.every { courseTemplateRepository.findById(any()) } returns java.util.Optional.empty()
        io.mockk.every { courseTemplateRepository.findAll() } returns listOf()
        io.mockk.every { lessonContentRepository.findByCourseId(any()) } returns listOf()
        io.mockk.every { lessonContentRepository.findByCourseIdOrderByDisplayOrder(any()) } returns listOf()
        io.mockk.every { curriculumRuleRepository.findByCourseId(any()) } returns null
    }

    private val lessonContentService = LessonContentService(objectMapper, lessonContentRepository, courseTemplateRepository, curriculumRuleRepository)

    @Test
    fun `should load Spanish curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("es-conversational-spanish")

        assertNotNull(curriculum)
        assertEquals("es-conversational-spanish", curriculum?.courseId)
        assertEquals(10, curriculum?.lessons?.size)
        assertEquals("lesson-01-greetings", curriculum?.lessons?.first()?.id)
    }

    @Test
    fun `should return null for non-existent curriculum`() {
        val curriculum = lessonContentService.getCurriculum("non-existent-course")

        assertNull(curriculum)
    }

    @Test
    fun `should load Spanish week 1 lesson successfully`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson)
        assertEquals("lesson-01-greetings", lesson?.id)
        assertEquals("Greetings and Basic Expressions", lesson?.title)
        assertEquals(1, lesson?.lessonNumber)
        assertEquals(CEFRLevel.A1, lesson?.targetCEFR)
        assertEquals(3, lesson?.focusAreas?.size)
    }

    @Test
    fun `should load lesson with valid content`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson)
        assertNotNull(lesson?.fullMarkdown)
        assertTrue(lesson?.fullMarkdown?.trim()?.isNotEmpty() == true)
    }

    @Test
    fun `should load lesson with metadata`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson)
        assertEquals("lesson-01-greetings", lesson?.id)
        assertEquals("Greetings and Basic Expressions", lesson?.title)
        assertEquals(1, lesson?.lessonNumber)
    }

    @Test
    fun `should load lesson with proper focus areas`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson)
        assertNotNull(lesson?.focusAreas)
        assertTrue(lesson?.focusAreas?.size ?: 0 > 0)
    }

    @Test
    fun `should load lesson with proper metadata`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson)
        assertEquals("lesson-01-greetings", lesson?.id)
        assertEquals("Greetings and Basic Expressions", lesson?.title)
        assertEquals(1, lesson?.lessonNumber)
    }

    @Test
    fun `should return null for non-existent lesson`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "lesson-99-nonexistent")

        assertNull(lesson)
    }

    @Test
    fun `should cache lesson content`() {
        val lesson1 = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")
        val lesson2 = lessonContentService.getLesson("es-conversational-spanish", "lesson-01-greetings")

        assertNotNull(lesson1)
        assertNotNull(lesson2)
        // Second call should return cached instance (same reference)
        assertEquals(lesson1, lesson2)
    }

    @Test
    fun `should cache curriculum`() {
        val curriculum1 = lessonContentService.getCurriculum("es-conversational-spanish")
        val curriculum2 = lessonContentService.getCurriculum("es-conversational-spanish")

        assertNotNull(curriculum1)
        assertNotNull(curriculum2)
        assertEquals(curriculum1, curriculum2)
    }

    @Test
    @Disabled("TODO: Fix curriculum loading - test expects specific lesson count")
    fun `should load French curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("fr-conversational-french")

        assertNotNull(curriculum)
        assertEquals("fr-conversational-french", curriculum?.courseId)
        assertEquals(1, curriculum?.lessons?.size)
    }

    @Test
    @Disabled("TODO: Fix curriculum loading - test expects specific lesson count")
    fun `should load German curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("de-conversational-german")

        assertNotNull(curriculum)
        assertEquals("de-conversational-german", curriculum?.courseId)
        assertEquals(1, curriculum?.lessons?.size)
    }

    @Test
    @Disabled("TODO: Fix lesson parsing - test expects specific lesson ID")
    fun `should parse lesson with minimal content`() {
        val lesson = lessonContentService.getLesson("fr-conversational-french", "lesson-01-greetings")

        assertNotNull(lesson)
        assertEquals("lesson-01-placeholder", lesson?.id)
        assertNotNull(lesson?.fullMarkdown)
    }

    @Test
    fun `should handle lesson without optional fields gracefully`() {
        val lesson = lessonContentService.getLesson("fr-conversational-french", "lesson-01-greetings")

        assertNotNull(lesson)
        // Should not crash even with minimal content
        assertNotNull(lesson?.fullMarkdown)
    }
}
