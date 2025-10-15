package ch.obermuhlner.aitutor.testharness.executor

import ch.obermuhlner.aitutor.chat.dto.*
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.testharness.client.ApiClient
import ch.obermuhlner.aitutor.testharness.config.TestHarnessConfig
import ch.obermuhlner.aitutor.testharness.domain.*
import ch.obermuhlner.aitutor.testharness.judge.JudgeService
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import ch.obermuhlner.aitutor.tutor.domain.TeachingStyle
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TestExecutorTest {

    private lateinit var mockApiClient: ApiClient
    private lateinit var mockJudgeService: JudgeService
    private lateinit var config: TestHarnessConfig
    private lateinit var testExecutor: TestExecutor

    @BeforeEach
    fun setup() {
        config = TestHarnessConfig(
            apiBaseUrl = "http://localhost:8080",
            apiUsername = "test-user",
            apiPassword = "test-pass",
            judgeModel = "gpt-4o",
            judgeTemperature = 0.3,
            judgeProvider = "openai",
            judgeApiKey = "test-key",
            scenarioFilter = "all",
            passThreshold = 70.0,
            delayBetweenRequestsMs = 0 // No delay for tests
        )

        // Mock dependencies
        mockApiClient = mockk()
        mockJudgeService = mockk()

        // Replace the real dependencies with mocks using reflection
        testExecutor = TestExecutor(config)

        // Use reflection to inject mocks (necessary since ApiClient and JudgeService are created in constructor)
        val apiClientField = TestExecutor::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(testExecutor, mockApiClient)

        val judgeServiceField = TestExecutor::class.java.getDeclaredField("judgeService")
        judgeServiceField.isAccessible = true
        judgeServiceField.set(testExecutor, mockJudgeService)
    }

    @Test
    fun `executeScenarios should execute multiple scenarios successfully`() {
        val scenario1 = createTestScenario(id = "scenario-1", name = "Test 1")
        val scenario2 = createTestScenario(id = "scenario-2", name = "Test 2")
        val userId = UUID.randomUUID()

        // Mock API client calls
        every { mockApiClient.login(any(), any()) } returns mockk()
        every { mockApiClient.getCurrentUserId() } returns userId
        every { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) } returns createMockSession()
        every { mockApiClient.sendMessage(any(), any()) } returns createMockMessageResponse()
        every { mockApiClient.getSessionWithMessages(any()) } returns createMockSessionWithMessages()
        every { mockApiClient.deleteSession(any()) } just Runs

        // Mock judge service
        every { mockJudgeService.evaluate(any(), any(), any()) } returns createMockJudgeEvaluation()

        val results = testExecutor.executeScenarios(listOf(scenario1, scenario2))

        assertEquals(2, results.size)
        assertEquals("scenario-1", results[0].scenarioId)
        assertEquals("scenario-2", results[1].scenarioId)

        verify(exactly = 1) { mockApiClient.login(config.apiUsername, config.apiPassword) }
        verify(exactly = 1) { mockApiClient.getCurrentUserId() }
        verify(exactly = 2) { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 2) { mockApiClient.deleteSession(any()) }
    }

    @Test
    fun `executeScenarios should handle scenario execution failures gracefully`() {
        val scenario = createTestScenario(id = "failing-scenario")
        val userId = UUID.randomUUID()

        every { mockApiClient.login(any(), any()) } returns mockk()
        every { mockApiClient.getCurrentUserId() } returns userId
        every { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) } throws
            RuntimeException("Session creation failed")

        val results = testExecutor.executeScenarios(listOf(scenario))

        assertEquals(1, results.size)
        assertEquals(0.0, results[0].overallScore)
        assertTrue(results[0].judgeEvaluation.overallFeedback.contains("Session creation failed"))
        assertEquals(0, results[0].conversationTranscript.size)
    }

    @Test
    fun `executeScenario should track conversation turns correctly`() {
        val scenario = createTestScenario(
            conversationScript = listOf(
                LearnerMessage("Hola, me llamo Juan", emptyList()),
                LearnerMessage("Tengo veinte años", emptyList())
            )
        )
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()

        every { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) } returns
            createMockSession(sessionId)
        every { mockApiClient.sendMessage(any(), any()) } returns createMockMessageResponse()
        every { mockApiClient.getSessionWithMessages(any()) } returns createMockSessionWithMessages()
        every { mockApiClient.deleteSession(any()) } just Runs
        every { mockJudgeService.evaluate(any(), any(), any()) } returns createMockJudgeEvaluation()

        // Use reflection to call private method
        val executeScenarioMethod = TestExecutor::class.java.getDeclaredMethod(
            "executeScenario",
            TestScenario::class.java,
            UUID::class.java
        )
        executeScenarioMethod.isAccessible = true
        val result = executeScenarioMethod.invoke(testExecutor, scenario, userId) as ScenarioResult

        assertEquals(2, result.conversationTranscript.size)
        assertEquals("Hola, me llamo Juan", result.conversationTranscript[0].learnerMessage)
        assertEquals("Tengo veinte años", result.conversationTranscript[1].learnerMessage)
        verify(exactly = 2) { mockApiClient.sendMessage(sessionId, any()) }
    }

    @Test
    fun `executeScenario should calculate technical metrics correctly`() {
        val intentionalError = IntentionalError(
            span = "tiene",
            errorType = "Agreement",
            expectedSeverity = "High",
            correctForm = "tengo",
            reasoning = "Wrong verb form"
        )

        val scenario = createTestScenario(
            conversationScript = listOf(
                LearnerMessage("Yo tiene un gato", listOf(intentionalError))
            )
        )
        val userId = UUID.randomUUID()

        every { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) } returns createMockSession()
        every { mockApiClient.sendMessage(any(), any()) } returns createMockMessageResponse(
            corrections = listOf(
                ch.obermuhlner.aitutor.core.model.Correction(
                    span = "tiene",
                    errorType = ErrorType.Agreement,
                    severity = ErrorSeverity.High,
                    correctedTargetLanguage = "tengo",
                    whySourceLanguage = "Subject-verb agreement error",
                    whyTargetLanguage = "Error de concordancia sujeto-verbo"
                )
            )
        )
        every { mockApiClient.getSessionWithMessages(any()) } returns createMockSessionWithMessages()
        every { mockApiClient.deleteSession(any()) } just Runs
        every { mockJudgeService.evaluate(any(), any(), any()) } answers {
            val metrics = it.invocation.args[2] as TechnicalMetrics
            // Verify metrics calculation
            assertEquals(1, metrics.totalCorrections)
            assertEquals(1, metrics.correctionsDetected)
            assertEquals(0, metrics.correctionsMissed)
            assertEquals(0, metrics.falsePositives)
            createMockJudgeEvaluation()
        }

        val executeScenarioMethod = TestExecutor::class.java.getDeclaredMethod(
            "executeScenario",
            TestScenario::class.java,
            UUID::class.java
        )
        executeScenarioMethod.isAccessible = true
        val result = executeScenarioMethod.invoke(testExecutor, scenario, userId) as ScenarioResult

        assertEquals(1, result.technicalMetrics.totalCorrections)
        assertEquals(1, result.technicalMetrics.correctionsDetected)
    }

    @Test
    fun `calculateMetrics should detect missed corrections`() {
        val intentionalError1 = IntentionalError("tiene", "Agreement", "High", "tengo", "Wrong")
        val intentionalError2 = IntentionalError("casa", "Gender", "Medium", "case", "Wrong gender")

        val scenario = createTestScenario(
            conversationScript = listOf(
                LearnerMessage("Test message", listOf(intentionalError1, intentionalError2))
            )
        )

        val turns = listOf(
            ConversationTurn(
                turnIndex = 1,
                learnerMessage = "Test message",
                tutorResponse = TutorResponse(
                    content = "Response",
                    corrections = listOf(
                        DetectedCorrection("tiene", "Agreement", "High", "tengo", "Explanation")
                        // Missing detection of "casa" error
                    ),
                    currentPhase = "Correction",
                    currentTopic = "Test"
                ),
                intentionalErrors = listOf(intentionalError1, intentionalError2)
            )
        )

        val sessionWithMessages = createMockSessionWithMessages()

        val calculateMetricsMethod = TestExecutor::class.java.getDeclaredMethod(
            "calculateMetrics",
            TestScenario::class.java,
            List::class.java,
            SessionWithMessagesResponse::class.java
        )
        calculateMetricsMethod.isAccessible = true
        val metrics = calculateMetricsMethod.invoke(testExecutor, scenario, turns, sessionWithMessages) as TechnicalMetrics

        assertEquals(2, metrics.totalCorrections)
        assertEquals(1, metrics.correctionsDetected)
        assertEquals(1, metrics.correctionsMissed)
        assertEquals(0, metrics.falsePositives)
    }

    @Test
    fun `calculateMetrics should detect false positives`() {
        val scenario = createTestScenario(
            conversationScript = listOf(
                LearnerMessage("Correct sentence", emptyList())
            )
        )

        val turns = listOf(
            ConversationTurn(
                turnIndex = 1,
                learnerMessage = "Correct sentence",
                tutorResponse = TutorResponse(
                    content = "Response",
                    corrections = listOf(
                        DetectedCorrection("something", "Grammar", "Low", "corrected", "False positive")
                    ),
                    currentPhase = "Correction",
                    currentTopic = "Test"
                ),
                intentionalErrors = emptyList()
            )
        )

        val sessionWithMessages = createMockSessionWithMessages()

        val calculateMetricsMethod = TestExecutor::class.java.getDeclaredMethod(
            "calculateMetrics",
            TestScenario::class.java,
            List::class.java,
            SessionWithMessagesResponse::class.java
        )
        calculateMetricsMethod.isAccessible = true
        val metrics = calculateMetricsMethod.invoke(testExecutor, scenario, turns, sessionWithMessages) as TechnicalMetrics

        assertEquals(0, metrics.totalCorrections)
        assertEquals(0, metrics.correctionsDetected)
        assertEquals(0, metrics.correctionsMissed)
        assertEquals(1, metrics.falsePositives)
    }

    @Test
    fun `calculateMetrics should track phase transitions`() {
        val scenario = createTestScenario(
            tutorConfig = TutorConfig(initialPhase = ConversationPhase.Free)
        )

        val turns = listOf(
            ConversationTurn(
                turnIndex = 1,
                learnerMessage = "Turn 1",
                tutorResponse = TutorResponse(
                    content = "Response 1",
                    currentPhase = "Free",
                    currentTopic = "Topic 1"
                ),
                intentionalErrors = emptyList()
            ),
            ConversationTurn(
                turnIndex = 2,
                learnerMessage = "Turn 2",
                tutorResponse = TutorResponse(
                    content = "Response 2",
                    currentPhase = "Correction",  // Phase changed
                    currentTopic = "Topic 1"
                ),
                intentionalErrors = emptyList()
            )
        )

        val sessionWithMessages = createMockSessionWithMessages()

        val calculateMetricsMethod = TestExecutor::class.java.getDeclaredMethod(
            "calculateMetrics",
            TestScenario::class.java,
            List::class.java,
            SessionWithMessagesResponse::class.java
        )
        calculateMetricsMethod.isAccessible = true
        val metrics = calculateMetricsMethod.invoke(testExecutor, scenario, turns, sessionWithMessages) as TechnicalMetrics

        assertEquals(1, metrics.phaseTransitions.size)
        assertEquals(2, metrics.phaseTransitions[0].atTurnIndex)
        assertEquals("Free", metrics.phaseTransitions[0].fromPhase)
        assertEquals("Correction", metrics.phaseTransitions[0].toPhase)
    }

    @Test
    fun `calculateMetrics should count topic changes and vocabulary`() {
        val scenario = createTestScenario()

        val turns = listOf(
            ConversationTurn(
                turnIndex = 1,
                learnerMessage = "Turn 1",
                tutorResponse = TutorResponse(
                    content = "Response 1",
                    currentPhase = "Free",
                    currentTopic = "Food",
                    newVocabulary = listOf("pan", "agua")
                ),
                intentionalErrors = emptyList()
            ),
            ConversationTurn(
                turnIndex = 2,
                learnerMessage = "Turn 2",
                tutorResponse = TutorResponse(
                    content = "Response 2",
                    currentPhase = "Free",
                    currentTopic = "Travel",  // Topic changed
                    newVocabulary = listOf("avión", "pan")  // "pan" is duplicate
                ),
                intentionalErrors = emptyList()
            )
        )

        val sessionWithMessages = createMockSessionWithMessages()

        val calculateMetricsMethod = TestExecutor::class.java.getDeclaredMethod(
            "calculateMetrics",
            TestScenario::class.java,
            List::class.java,
            SessionWithMessagesResponse::class.java
        )
        calculateMetricsMethod.isAccessible = true
        val metrics = calculateMetricsMethod.invoke(testExecutor, scenario, turns, sessionWithMessages) as TechnicalMetrics

        assertEquals(1, metrics.topicChanges)  // 2 unique topics = 1 change
        assertEquals(3, metrics.vocabularyItemsIntroduced)  // "pan", "agua", "avión" (distinct)
    }

    @Test
    fun `createFailedResult should return valid result with error details`() {
        val scenario = createTestScenario(id = "failed-scenario")
        val error = RuntimeException("Database connection failed")

        val createFailedResultMethod = TestExecutor::class.java.getDeclaredMethod(
            "createFailedResult",
            TestScenario::class.java,
            Exception::class.java
        )
        createFailedResultMethod.isAccessible = true
        val result = createFailedResultMethod.invoke(testExecutor, scenario, error) as ScenarioResult

        assertEquals("failed-scenario", result.scenarioId)
        assertEquals(0.0, result.overallScore)
        assertEquals(0, result.conversationTranscript.size)
        assertTrue(result.judgeEvaluation.overallFeedback.contains("Database connection failed"))
        assertTrue(result.judgeEvaluation.improvements.any { it.contains("Database connection failed") })
        assertEquals(0, result.technicalMetrics.totalMessages)
    }

    @Test
    fun `executeScenario should use effectivePhase for phase tracking`() {
        val scenario = createTestScenario(
            conversationScript = listOf(
                LearnerMessage("Test message", emptyList())
            )
        )
        val userId = UUID.randomUUID()

        every { mockApiClient.createSession(any(), any(), any(), any(), any(), any(), any()) } returns createMockSession()
        every { mockApiClient.sendMessage(any(), any()) } returns createMockMessageResponse()
        every { mockApiClient.getSessionWithMessages(any()) } returns createMockSessionWithMessages(
            effectivePhase = ConversationPhase.Correction
        )
        every { mockApiClient.deleteSession(any()) } just Runs
        every { mockJudgeService.evaluate(any(), any(), any()) } returns createMockJudgeEvaluation()

        val executeScenarioMethod = TestExecutor::class.java.getDeclaredMethod(
            "executeScenario",
            TestScenario::class.java,
            UUID::class.java
        )
        executeScenarioMethod.isAccessible = true
        val result = executeScenarioMethod.invoke(testExecutor, scenario, userId) as ScenarioResult

        assertEquals("Correction", result.conversationTranscript[0].tutorResponse.currentPhase)
    }

    // Helper methods

    private fun createTestScenario(
        id: String = "test-scenario",
        name: String = "Test Scenario",
        conversationScript: List<LearnerMessage> = listOf(
            LearnerMessage("Test message", emptyList())
        ),
        tutorConfig: TutorConfig = TutorConfig(initialPhase = ConversationPhase.Correction)
    ) = TestScenario(
        id = id,
        name = name,
        description = "Test description",
        learnerPersona = LearnerPersona(
            name = "Test Learner",
            cefrLevel = CEFRLevel.A2,
            sourceLanguage = "en",
            targetLanguage = "es",
            commonErrors = listOf("Agreement"),
            learningGoals = listOf("Basic conversation")
        ),
        tutorConfig = tutorConfig,
        conversationScript = conversationScript,
        expectedOutcomes = ExpectedOutcomes(),
        evaluationFocus = listOf(EvaluationFocus.ERROR_DETECTION)
    )

    private fun createMockSession(id: UUID = UUID.randomUUID()) = SessionResponse(
        id = id,
        userId = UUID.randomUUID(),
        tutorName = "Test Tutor",
        tutorPersona = "Test persona",
        tutorDomain = "general conversation",
        tutorTeachingStyle = TeachingStyle.Guided,
        sourceLanguageCode = "en",
        targetLanguageCode = "es",
        conversationPhase = ConversationPhase.Correction,
        effectivePhase = ConversationPhase.Correction,
        estimatedCEFRLevel = CEFRLevel.A2,
        currentTopic = "Test topic",
        createdAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now()
    )

    private fun createMockMessageResponse(
        corrections: List<ch.obermuhlner.aitutor.core.model.Correction> = emptyList()
    ) = MessageResponse(
        id = UUID.randomUUID(),
        role = "assistant",
        content = "Test response",
        corrections = corrections,
        newVocabulary = emptyList(),
        wordCards = emptyList(),
        createdAt = java.time.Instant.now()
    )

    private fun createMockSessionWithMessages(
        effectivePhase: ConversationPhase = ConversationPhase.Correction
    ) = SessionWithMessagesResponse(
        session = SessionResponse(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            tutorName = "Test Tutor",
            tutorPersona = "Test persona",
            tutorDomain = "general conversation",
            tutorTeachingStyle = TeachingStyle.Guided,
            sourceLanguageCode = "en",
            targetLanguageCode = "es",
            conversationPhase = ConversationPhase.Auto,
            effectivePhase = effectivePhase,
            estimatedCEFRLevel = CEFRLevel.A2,
            currentTopic = "Test topic",
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        ),
        messages = emptyList()
    )

    private fun createMockJudgeEvaluation() = JudgeEvaluation(
        errorDetectionScore = 85.0,
        errorDetectionFeedback = "Good",
        phaseAppropriatenessScore = 85.0,
        phaseAppropriatenessFeedback = "Good",
        correctionQualityScore = 85.0,
        correctionQualityFeedback = "Good",
        encouragementBalanceScore = 85.0,
        encouragementBalanceFeedback = "Good",
        topicManagementScore = 85.0,
        topicManagementFeedback = "Good",
        vocabularyTeachingScore = 85.0,
        vocabularyTeachingFeedback = "Good",
        overallPedagogicalScore = 85.0,
        overallFeedback = "Excellent performance",
        strengths = listOf("Error detection"),
        improvements = emptyList()
    )
}
