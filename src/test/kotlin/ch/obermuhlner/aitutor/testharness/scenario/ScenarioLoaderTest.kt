package ch.obermuhlner.aitutor.testharness.scenario

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScenarioLoaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should load valid scenario from YAML file`() {
        // Given
        val yamlContent = """
            id: test-scenario
            name: Test Scenario
            description: A test scenario
            learnerPersona:
              name: TestLearner
              cefrLevel: B1
              sourceLanguage: en
              targetLanguage: es
              commonErrors:
                - Grammar errors
              learningGoals:
                - Improve fluency
            tutorConfig:
              tutorName: TestTutor
              initialPhase: Correction
              teachingStyle: Guided
            conversationScript:
              - content: "Hola"
                intentionalErrors: []
            expectedOutcomes:
              minimumCorrectionsDetected: 0
            evaluationFocus:
              - ERROR_DETECTION
        """.trimIndent()

        val scenarioFile = File(tempDir, "test-scenario.yml")
        scenarioFile.writeText(yamlContent)

        val loader = ScenarioLoader(tempDir.absolutePath)

        // When
        val scenarios = loader.loadScenarios("all")

        // Then
        assertEquals(1, scenarios.size)
        val scenario = scenarios.first()
        assertEquals("test-scenario", scenario.id)
        assertEquals("Test Scenario", scenario.name)
        assertEquals("TestLearner", scenario.learnerPersona.name)
        assertEquals(CEFRLevel.B1, scenario.learnerPersona.cefrLevel)
        assertEquals("TestTutor", scenario.tutorConfig.tutorName)
        assertEquals(ConversationPhase.Correction, scenario.tutorConfig.initialPhase)
        assertEquals(1, scenario.conversationScript.size)
    }

    @Test
    fun `should filter scenarios by ID`() {
        // Given
        val scenario1 = File(tempDir, "beginner-test.yml")
        scenario1.writeText("""
            id: beginner-test
            name: Beginner Test
            description: Test
            learnerPersona:
              name: TestLearner
              cefrLevel: A1
              sourceLanguage: en
              targetLanguage: es
            tutorConfig:
              tutorName: TestTutor
              initialPhase: Auto
              teachingStyle: Guided
            conversationScript:
              - content: "Test"
            expectedOutcomes:
              minimumCorrectionsDetected: 0
            evaluationFocus:
              - ERROR_DETECTION
        """.trimIndent())

        val scenario2 = File(tempDir, "advanced-test.yml")
        scenario2.writeText("""
            id: advanced-test
            name: Advanced Test
            description: Test
            learnerPersona:
              name: TestLearner
              cefrLevel: C1
              sourceLanguage: en
              targetLanguage: fr
            tutorConfig:
              tutorName: TestTutor
              initialPhase: Free
              teachingStyle: Reactive
            conversationScript:
              - content: "Test"
            expectedOutcomes:
              minimumCorrectionsDetected: 0
            evaluationFocus:
              - PHASE_APPROPRIATENESS
        """.trimIndent())

        val loader = ScenarioLoader(tempDir.absolutePath)

        // When
        val filtered = loader.loadScenarios("beginner")

        // Then
        assertEquals(1, filtered.size)
        assertEquals("beginner-test", filtered.first().id)
    }

    @Test
    fun `should return empty list for non-existent directory`() {
        // Given
        val loader = ScenarioLoader("non-existent-directory")

        // When
        val scenarios = loader.loadScenarios("all")

        // Then
        assertTrue(scenarios.isEmpty())
    }

    @Test
    fun `should load scenario by specific ID`() {
        // Given
        val scenarioFile = File(tempDir, "specific-test.yml")
        scenarioFile.writeText("""
            id: specific-scenario
            name: Specific Scenario
            description: Test
            learnerPersona:
              name: TestLearner
              cefrLevel: B2
              sourceLanguage: en
              targetLanguage: de
            tutorConfig:
              tutorName: TestTutor
              initialPhase: Correction
              teachingStyle: Guided
            conversationScript:
              - content: "Test"
            expectedOutcomes:
              minimumCorrectionsDetected: 0
            evaluationFocus:
              - ERROR_DETECTION
        """.trimIndent())

        val loader = ScenarioLoader(tempDir.absolutePath)

        // When
        val scenario = loader.loadScenario("specific-scenario")

        // Then
        assertNotNull(scenario)
        assertEquals("specific-scenario", scenario.id)
        assertEquals("Specific Scenario", scenario.name)
    }
}
