package ch.obermuhlner.aitutor.testharness.judge

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.testharness.ai.AiProvider
import ch.obermuhlner.aitutor.testharness.ai.AiProviderConfig
import ch.obermuhlner.aitutor.testharness.ai.AiProviderType
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.*
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JudgeServiceTest {

    private lateinit var mockAiProvider: AiProvider
    private lateinit var config: TestHarnessConfig
    private lateinit var judgeService: JudgeService

    @BeforeEach
    fun setup() {
        config = TestHarnessConfig(
            apiBaseUrl = "http://localhost:8080",
            judgeModel = "gpt-4o",
            judgeTemperature = 0.3,
            judgeProvider = "openai",
            judgeApiKey = "test-key",
            scenarioFilter = "all",
            passThreshold = 70.0
        )

        // Mock the AiProvider through factory
        mockAiProvider = mockk()
        mockkObject(ch.obermuhlner.aitutor.testharness.ai.AiProviderFactory)
        every { ch.obermuhlner.aitutor.testharness.ai.AiProviderFactory.create(any()) } returns mockAiProvider

        judgeService = JudgeService(config)
    }

    @Test
    fun `evaluate should parse valid JSON response correctly`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        val jsonResponse = """
        {
          "errorDetectionScore": 85.0,
          "errorDetectionFeedback": "Good error detection",
          "phaseAppropriatenessScore": 90.0,
          "phaseAppropriatenessFeedback": "Appropriate phase selection",
          "correctionQualityScore": 80.0,
          "correctionQualityFeedback": "Clear corrections",
          "encouragementBalanceScore": 88.0,
          "encouragementBalanceFeedback": "Good balance",
          "topicManagementScore": 75.0,
          "topicManagementFeedback": "Decent topic flow",
          "vocabularyTeachingScore": 82.0,
          "vocabularyTeachingFeedback": "Effective vocabulary teaching",
          "overallPedagogicalScore": 83.3,
          "overallFeedback": "Strong overall performance",
          "strengths": ["error detection", "encouragement"],
          "improvements": ["topic variety", "pacing"]
        }
        """.trimIndent()

        every { mockAiProvider.chat(any(), any(), any()) } returns jsonResponse

        val result = judgeService.evaluate(scenario, transcript, metrics)

        assertEquals(85.0, result.errorDetectionScore)
        assertEquals("Good error detection", result.errorDetectionFeedback)
        assertEquals(90.0, result.phaseAppropriatenessScore)
        assertEquals(80.0, result.correctionQualityScore)
        assertEquals(88.0, result.encouragementBalanceScore)
        assertEquals(75.0, result.topicManagementScore)
        assertEquals(82.0, result.vocabularyTeachingScore)
        assertEquals(83.3, result.overallPedagogicalScore)
        assertEquals(2, result.strengths.size)
        assertEquals(2, result.improvements.size)
    }

    @Test
    fun `evaluate should extract JSON from markdown code blocks`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        val markdownResponse = """
        Here is my evaluation:

        ```json
        {
          "errorDetectionScore": 75.0,
          "errorDetectionFeedback": "Acceptable",
          "phaseAppropriatenessScore": 70.0,
          "phaseAppropriatenessFeedback": "OK",
          "correctionQualityScore": 72.0,
          "correctionQualityFeedback": "Good",
          "encouragementBalanceScore": 78.0,
          "encouragementBalanceFeedback": "Nice",
          "topicManagementScore": 65.0,
          "topicManagementFeedback": "Fair",
          "vocabularyTeachingScore": 70.0,
          "vocabularyTeachingFeedback": "Adequate",
          "overallPedagogicalScore": 71.7,
          "overallFeedback": "Good work",
          "strengths": ["encouragement"],
          "improvements": ["topics"]
        }
        ```
        """.trimIndent()

        every { mockAiProvider.chat(any(), any(), any()) } returns markdownResponse

        val result = judgeService.evaluate(scenario, transcript, metrics)

        assertEquals(75.0, result.errorDetectionScore)
        assertEquals(71.7, result.overallPedagogicalScore)
    }

    @Test
    fun `evaluate should repair missing commas after string values`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        // JSON with missing commas after string values (the regex pattern repairs this)
        val brokenJsonResponse = """
        {
          "errorDetectionScore": 80.0,
          "errorDetectionFeedback": "Good detection"

          "phaseAppropriatenessScore": 85.0,
          "phaseAppropriatenessFeedback": "Appropriate phase"

          "correctionQualityScore": 78.0,
          "correctionQualityFeedback": "Clear feedback"

          "encouragementBalanceScore": 82.0,
          "encouragementBalanceFeedback": "Balanced approach"

          "topicManagementScore": 75.0,
          "topicManagementFeedback": "Good flow"

          "vocabularyTeachingScore": 80.0,
          "vocabularyTeachingFeedback": "Effective teaching"

          "overallPedagogicalScore": 80.0,
          "overallFeedback": "Great performance"

          "strengths": ["error detection"],
          "improvements": ["pacing"]
        }
        """.trimIndent()

        every { mockAiProvider.chat(any(), any(), any()) } returns brokenJsonResponse

        val result = judgeService.evaluate(scenario, transcript, metrics)

        // Should successfully parse with repaired commas
        assertEquals(80.0, result.errorDetectionScore)
        assertEquals("Good detection", result.errorDetectionFeedback)
        assertEquals(85.0, result.phaseAppropriatenessScore)
        assertEquals("Appropriate phase", result.phaseAppropriatenessFeedback)
    }

    @Test
    fun `evaluate should handle completely malformed JSON gracefully`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        every { mockAiProvider.chat(any(), any(), any()) } returns "This is not JSON at all"

        val result = judgeService.evaluate(scenario, transcript, metrics)

        // Should return default evaluation with 0 scores
        assertEquals(0.0, result.errorDetectionScore)
        assertEquals(0.0, result.phaseAppropriatenessScore)
        assertEquals(0.0, result.overallPedagogicalScore)
        assertTrue(result.errorDetectionFeedback.contains("Failed to parse"))
        assertTrue(result.improvements.contains("Fix judge response parsing"))
    }

    @Test
    fun `evaluate should call AI provider with correct parameters`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        val validResponse = """
        {
          "errorDetectionScore": 80.0,
          "errorDetectionFeedback": "Good",
          "phaseAppropriatenessScore": 80.0,
          "phaseAppropriatenessFeedback": "Good",
          "correctionQualityScore": 80.0,
          "correctionQualityFeedback": "Good",
          "encouragementBalanceScore": 80.0,
          "encouragementBalanceFeedback": "Good",
          "topicManagementScore": 80.0,
          "topicManagementFeedback": "Good",
          "vocabularyTeachingScore": 80.0,
          "vocabularyTeachingFeedback": "Good",
          "overallPedagogicalScore": 80.0,
          "overallFeedback": "Good",
          "strengths": ["test"],
          "improvements": ["test"]
        }
        """.trimIndent()

        every { mockAiProvider.chat(any(), "gpt-4o", 0.3) } returns validResponse

        judgeService.evaluate(scenario, transcript, metrics)

        verify { mockAiProvider.chat(any(), "gpt-4o", 0.3) }
    }

    @Test
    fun `evaluate should include scenario details in prompt`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        val promptCapture = slot<String>()
        every { mockAiProvider.chat(capture(promptCapture), any(), any()) } returns "{}"

        try {
            judgeService.evaluate(scenario, transcript, metrics)
        } catch (e: Exception) {
            // Ignore parse errors, we just want to check the prompt
        }

        val capturedPrompt = promptCapture.captured
        assertTrue(capturedPrompt.contains(scenario.name))
        assertTrue(capturedPrompt.contains(scenario.description))
        assertTrue(capturedPrompt.contains(scenario.learnerPersona.name))
    }

    @Test
    fun `evaluate should include transcript in prompt`() {
        val scenario = createTestScenario()
        val transcript = listOf(
            ConversationTurn(
                turnIndex = 1,
                learnerMessage = "Test learner message",
                tutorResponse = TutorResponse(
                    content = "Test tutor response",
                    currentPhase = "Correction",
                    currentTopic = "Test topic"
                )
            )
        )
        val metrics = createMetrics()

        val promptCapture = slot<String>()
        every { mockAiProvider.chat(capture(promptCapture), any(), any()) } returns "{}"

        try {
            judgeService.evaluate(scenario, transcript, metrics)
        } catch (e: Exception) {
            // Ignore parse errors
        }

        val capturedPrompt = promptCapture.captured
        assertTrue(capturedPrompt.contains("Test learner message"))
        assertTrue(capturedPrompt.contains("Test tutor response"))
    }

    @Test
    fun `evaluate should handle partial JSON responses`() {
        val scenario = createTestScenario()
        val transcript = createTranscript()
        val metrics = createMetrics()

        // JSON missing some optional fields but with required fields
        val partialResponse = """
        {
          "errorDetectionScore": 75.0,
          "errorDetectionFeedback": "OK",
          "phaseAppropriatenessScore": 70.0,
          "phaseAppropriatenessFeedback": "OK",
          "correctionQualityScore": 72.0,
          "correctionQualityFeedback": "OK",
          "encouragementBalanceScore": 78.0,
          "encouragementBalanceFeedback": "OK",
          "topicManagementScore": 65.0,
          "topicManagementFeedback": "OK",
          "vocabularyTeachingScore": 70.0,
          "vocabularyTeachingFeedback": "OK",
          "overallPedagogicalScore": 71.7,
          "overallFeedback": "OK",
          "strengths": [],
          "improvements": []
        }
        """.trimIndent()

        every { mockAiProvider.chat(any(), any(), any()) } returns partialResponse

        val result = judgeService.evaluate(scenario, transcript, metrics)

        assertEquals(75.0, result.errorDetectionScore)
        assertEquals(71.7, result.overallPedagogicalScore)
        assertTrue(result.strengths.isEmpty())
        assertTrue(result.improvements.isEmpty())
    }

    private fun createTestScenario() = TestScenario(
        id = "test-scenario",
        name = "Test Scenario",
        description = "A test scenario for unit testing",
        learnerPersona = LearnerPersona(
            name = "Test Learner",
            cefrLevel = CEFRLevel.A2,
            sourceLanguage = "en",
            targetLanguage = "es",
            commonErrors = listOf("Agreement"),
            learningGoals = listOf("Basic conversation")
        ),
        tutorConfig = TutorConfig(),
        conversationScript = emptyList(),
        expectedOutcomes = ExpectedOutcomes(),
        evaluationFocus = listOf(EvaluationFocus.ERROR_DETECTION)
    )

    private fun createTranscript() = listOf(
        ConversationTurn(
            turnIndex = 1,
            learnerMessage = "Yo tiene un gato",
            tutorResponse = TutorResponse(
                content = "¡Muy bien! You have a cat.",
                corrections = listOf(
                    DetectedCorrection(
                        span = "tiene",
                        errorType = "Agreement",
                        severity = "High",
                        correctedForm = "tengo",
                        explanation = "Subject-verb agreement"
                    )
                ),
                currentPhase = "Correction",
                currentTopic = "Pets"
            ),
            intentionalErrors = listOf(
                IntentionalError(
                    span = "tiene",
                    errorType = "Agreement",
                    expectedSeverity = "High",
                    correctForm = "tengo",
                    reasoning = "Wrong verb form"
                )
            )
        )
    )

    private fun createMetrics() = TechnicalMetrics(
        totalMessages = 10,
        totalCorrections = 5,
        correctionsDetected = 4,
        correctionsMissed = 1,
        falsePositives = 0,
        phaseTransitions = emptyList(),
        topicChanges = 2,
        vocabularyItemsIntroduced = 3,
        averageResponseTimeMs = 1500
    )
}
