package ch.obermuhlner.aitutor.testharness.report

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.testharness.ai.AiProviderType
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

class ReportGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generateReport should create markdown file`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val results = listOf(createScenarioResult())

        val reportPath = generator.generateReport(results)

        assertTrue(File(reportPath).exists())
        assertTrue(reportPath.endsWith(".md"))
    }

    @Test
    fun `generateReport should include overall score in markdown`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val results = listOf(createScenarioResult(overallScore = 85.5))

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("Overall Score"))
        assertTrue(content.contains("85"))
    }

    @Test
    fun `generateReport should include summary table`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val results = listOf(createScenarioResult())

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("## Summary"))
        assertTrue(content.contains("| Scenario | Overall Score"))
        assertTrue(content.contains("Error Detection"))
    }

    @Test
    fun `generateReport should include technical metrics`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val results = listOf(createScenarioResult())

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("## Technical Metrics"))
        assertTrue(content.contains("Messages"))
        assertTrue(content.contains("Corrections"))
    }

    @Test
    fun `generateReport should include detailed results for each scenario`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val scenarioName = "Test Scenario ABC"
        val results = listOf(createScenarioResult(scenarioName = scenarioName))

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("## Detailed Results"))
        assertTrue(content.contains(scenarioName))
    }

    @Test
    fun `generateReport should include judge evaluation feedback`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val feedback = "Excellent error detection performance"
        val results = listOf(
            createScenarioResult(
                judgeEvaluation = createJudgeEvaluation(errorDetectionFeedback = feedback)
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains(feedback))
        assertTrue(content.contains("**Error Detection"))
    }

    @Test
    fun `generateReport should include strengths and improvements`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val results = listOf(
            createScenarioResult(
                judgeEvaluation = createJudgeEvaluation(
                    strengths = listOf("Great error detection", "Good pacing"),
                    improvements = listOf("More vocabulary", "Better topics")
                )
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("**Strengths:**"))
        assertTrue(content.contains("Great error detection"))
        assertTrue(content.contains("**Improvements:**"))
        assertTrue(content.contains("More vocabulary"))
    }

    @Test
    fun `generateReport should include conversation transcript`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)
        val learnerMessage = "Test learner message content"
        val tutorResponse = "Test tutor response content"

        val results = listOf(
            createScenarioResult(
                transcript = listOf(
                    ConversationTurn(
                        turnIndex = 1,
                        learnerMessage = learnerMessage,
                        tutorResponse = TutorResponse(
                            content = tutorResponse,
                            currentPhase = "Correction",
                            currentTopic = "Test Topic"
                        )
                    )
                )
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("#### Conversation Transcript"))
        assertTrue(content.contains(learnerMessage))
        assertTrue(content.contains(tutorResponse))
    }

    @Test
    fun `generateReport should include intentional errors in transcript`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)

        val results = listOf(
            createScenarioResult(
                transcript = listOf(
                    ConversationTurn(
                        turnIndex = 1,
                        learnerMessage = "Yo tiene un gato",
                        tutorResponse = TutorResponse(
                            content = "Response",
                            currentPhase = "Correction",
                            currentTopic = null
                        ),
                        intentionalErrors = listOf(
                            IntentionalError(
                                span = "tiene",
                                errorType = "Agreement",
                                expectedSeverity = "High",
                                correctForm = "tengo",
                                reasoning = "Wrong form"
                            )
                        )
                    )
                )
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("Intentional errors"))
        assertTrue(content.contains("tiene"))
    }

    @Test
    fun `generateReport should handle multiple scenarios`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)

        val results = listOf(
            createScenarioResult(scenarioName = "Scenario 1"),
            createScenarioResult(scenarioName = "Scenario 2"),
            createScenarioResult(scenarioName = "Scenario 3")
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("Scenario 1"))
        assertTrue(content.contains("Scenario 2"))
        assertTrue(content.contains("Scenario 3"))
    }

    @Test
    fun `generateReport should calculate average score correctly`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)

        val results = listOf(
            createScenarioResult(overallScore = 80.0),
            createScenarioResult(overallScore = 90.0),
            createScenarioResult(overallScore = 70.0)
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("80"))  // Average of 80, 90, 70
    }

    @Test
    fun `generateReport should include phase transitions in metrics`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)

        val results = listOf(
            createScenarioResult(
                metrics = createMetrics(
                    phaseTransitions = listOf(
                        PhaseTransitionMetric(
                            atTurnIndex = 3,
                            fromPhase = "Correction",
                            toPhase = "Drill",
                            wasExpected = true
                        )
                    )
                )
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("Phase transitions"))
        assertTrue(content.contains("Turn 3"))
        assertTrue(content.contains("Correction"))
        assertTrue(content.contains("Drill"))
    }

    @Test
    fun `generateReport should include corrections in transcript`() {
        val config = createTestConfig()
        val generator = ReportGenerator(config)

        val results = listOf(
            createScenarioResult(
                transcript = listOf(
                    ConversationTurn(
                        turnIndex = 1,
                        learnerMessage = "Test",
                        tutorResponse = TutorResponse(
                            content = "Response",
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
                            currentTopic = null
                        )
                    )
                )
            )
        )

        val reportPath = generator.generateReport(results)
        val content = File(reportPath).readText()

        assertTrue(content.contains("Corrections:"))
        assertTrue(content.contains("tiene"))
        assertTrue(content.contains("tengo"))
    }

    private fun createTestConfig() = TestHarnessConfig(
        apiBaseUrl = "http://localhost:8080",
        judgeModel = "gpt-4o",
        judgeTemperature = 0.3,
        judgeProvider = "openai",
        judgeApiKey = "test-key",
        scenarioFilter = "all",
        passThreshold = 70.0,
        reportsOutputDir = tempDir.toString()
    )

    private fun createScenarioResult(
        scenarioName: String = "Test Scenario",
        overallScore: Double = 80.0,
        judgeEvaluation: JudgeEvaluation = createJudgeEvaluation(),
        transcript: List<ConversationTurn> = emptyList(),
        metrics: TechnicalMetrics = createMetrics()
    ) = ScenarioResult(
        scenarioId = "test-scenario",
        scenarioName = scenarioName,
        sessionId = UUID.randomUUID(),
        executionTime = Instant.now(),
        conversationTranscript = transcript,
        judgeEvaluation = judgeEvaluation,
        technicalMetrics = metrics,
        overallScore = overallScore
    )

    private fun createJudgeEvaluation(
        errorDetectionFeedback: String = "Good",
        strengths: List<String> = listOf("strength1", "strength2"),
        improvements: List<String> = listOf("improvement1")
    ) = JudgeEvaluation(
        errorDetectionScore = 80.0,
        errorDetectionFeedback = errorDetectionFeedback,
        phaseAppropriatenessScore = 85.0,
        phaseAppropriatenessFeedback = "Appropriate",
        correctionQualityScore = 78.0,
        correctionQualityFeedback = "Clear",
        encouragementBalanceScore = 82.0,
        encouragementBalanceFeedback = "Balanced",
        topicManagementScore = 75.0,
        topicManagementFeedback = "Good flow",
        vocabularyTeachingScore = 80.0,
        vocabularyTeachingFeedback = "Effective",
        overallPedagogicalScore = 80.0,
        overallFeedback = "Strong performance",
        strengths = strengths,
        improvements = improvements
    )

    private fun createMetrics(
        phaseTransitions: List<PhaseTransitionMetric> = emptyList()
    ) = TechnicalMetrics(
        totalMessages = 10,
        totalCorrections = 5,
        correctionsDetected = 4,
        correctionsMissed = 1,
        falsePositives = 0,
        phaseTransitions = phaseTransitions,
        topicChanges = 2,
        vocabularyItemsIntroduced = 3,
        averageResponseTimeMs = 1500
    )
}
