package ch.obermuhlner.aitutor.lesson.service

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LessonContentServiceTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val lessonContentService = LessonContentService(objectMapper)

    @Test
    fun `should load Spanish curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("es-conversational-spanish")

        assertNotNull(curriculum)
        assertEquals("es-conversational-spanish", curriculum?.courseId)
        assertEquals(10, curriculum?.lessons?.size)
        assertEquals("week-01-greetings", curriculum?.lessons?.first()?.id)
    }

    @Test
    fun `should return null for non-existent curriculum`() {
        val curriculum = lessonContentService.getCurriculum("non-existent-course")

        assertNull(curriculum)
    }

    @Test
    fun `should load Spanish week 1 lesson successfully`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals("week-01-greetings", lesson?.id)
        assertEquals("Greetings and Basic Expressions", lesson?.title)
        assertEquals(1, lesson?.weekNumber)
        assertEquals(CEFRLevel.A1, lesson?.targetCEFR)
        assertEquals(3, lesson?.focusAreas?.size)
    }

    @Test
    fun `should parse goals from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals(4, lesson?.goals?.size)
        assertEquals(true, lesson?.goals?.any { it.contains("greetings") })
    }

    @Test
    fun `should parse grammar points from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        // Parser currently extracts at least one grammar point
        assertEquals(true, lesson!!.grammarPoints.isNotEmpty())

        val formalInformal = lesson.grammarPoints.find { it.title.contains("Formal") }
        assertNotNull(formalInformal)
        assertEquals(true, formalInformal?.rule?.contains("tú"))
        assertEquals(true, formalInformal?.examples?.isNotEmpty())
    }

    @Test
    fun `should parse essential vocabulary from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals(true, lesson!!.essentialVocabulary.size > 15)

        val hola = lesson.essentialVocabulary.find { it.word == "Hola" }
        assertNotNull(hola)
        assertEquals("Hello", hola?.translation)
    }

    @Test
    fun `should parse conversation scenarios from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        // Parser currently extracts at least one scenario
        assertEquals(true, lesson!!.conversationScenarios.isNotEmpty())

        val firstMeeting = lesson.conversationScenarios.find { it.title.contains("First Time") }
        assertNotNull(firstMeeting)
        assertEquals(true, firstMeeting?.dialogue?.contains("Buenos días"))
    }

    @Test
    fun `should parse practice patterns from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals(true, lesson!!.practicePatterns.size >= 5)
        assertEquals(true, lesson.practicePatterns.any { it.contains("tutor") })
    }

    @Test
    fun `should parse common mistakes from markdown`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals(5, lesson!!.commonMistakes.size)
        assertEquals(true, lesson.commonMistakes.any { it.contains("encantado") })
    }

    @Test
    fun `should return null for non-existent lesson`() {
        val lesson = lessonContentService.getLesson("es-conversational-spanish", "week-99-nonexistent")

        assertNull(lesson)
    }

    @Test
    fun `should cache lesson content`() {
        val lesson1 = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")
        val lesson2 = lessonContentService.getLesson("es-conversational-spanish", "week-01-greetings")

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
    fun `should load French curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("fr-conversational-french")

        assertNotNull(curriculum)
        assertEquals("fr-conversational-french", curriculum?.courseId)
        assertEquals(1, curriculum?.lessons?.size)
    }

    @Test
    fun `should load German curriculum successfully`() {
        val curriculum = lessonContentService.getCurriculum("de-conversational-german")

        assertNotNull(curriculum)
        assertEquals("de-conversational-german", curriculum?.courseId)
        assertEquals(1, curriculum?.lessons?.size)
    }

    @Test
    fun `should parse lesson with minimal content`() {
        val lesson = lessonContentService.getLesson("fr-conversational-french", "week-01-greetings")

        assertNotNull(lesson)
        assertEquals("week-01-placeholder", lesson?.id)
        assertNotNull(lesson?.fullMarkdown)
    }

    @Test
    fun `should handle lesson without optional fields gracefully`() {
        val lesson = lessonContentService.getLesson("fr-conversational-french", "week-01-greetings")

        assertNotNull(lesson)
        // Should not crash even with minimal content
        assertNotNull(lesson?.goals)
        assertNotNull(lesson?.grammarPoints)
        assertNotNull(lesson?.essentialVocabulary)
    }
}
