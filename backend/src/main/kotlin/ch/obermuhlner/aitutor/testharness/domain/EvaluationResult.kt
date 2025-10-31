package ch.obermuhlner.aitutor.testharness.domain

import java.time.Instant
import java.util.UUID

/**
 * Result of executing a test scenario.
 */
data class ScenarioResult(
    val scenarioId: String,
    val scenarioName: String,
    val sessionId: UUID,
    val executionTime: Instant,
    val conversationTranscript: List<ConversationTurn>,
    val judgeEvaluation: JudgeEvaluation,
    val technicalMetrics: TechnicalMetrics,
    val overallScore: Double
)

/**
 * A single turn in the conversation (learner message + tutor response).
 */
data class ConversationTurn(
    val turnIndex: Int,
    val learnerMessage: String,
    val tutorResponse: TutorResponse,
    val intentionalErrors: List<IntentionalError> = emptyList()
)

/**
 * Tutor's response to a learner message.
 */
data class TutorResponse(
    val content: String,
    val corrections: List<DetectedCorrection> = emptyList(),
    val newVocabulary: List<String> = emptyList(),
    val wordCards: List<String> = emptyList(),
    val currentPhase: String,
    val currentTopic: String?
)

/**
 * A correction detected by the tutor.
 */
data class DetectedCorrection(
    val span: String,
    val errorType: String,
    val severity: String,
    val correctedForm: String,
    val explanation: String
)

/**
 * LLM judge's evaluation of the conversation.
 */
data class JudgeEvaluation(
    val errorDetectionScore: Double,
    val errorDetectionFeedback: String,
    val phaseAppropriatenessScore: Double,
    val phaseAppropriatenessFeedback: String,
    val correctionQualityScore: Double,
    val correctionQualityFeedback: String,
    val encouragementBalanceScore: Double,
    val encouragementBalanceFeedback: String,
    val topicManagementScore: Double,
    val topicManagementFeedback: String,
    val vocabularyTeachingScore: Double,
    val vocabularyTeachingFeedback: String,
    val overallPedagogicalScore: Double,
    val overallFeedback: String,
    val strengths: List<String>,
    val improvements: List<String>
)

/**
 * Technical metrics collected during scenario execution.
 */
data class TechnicalMetrics(
    val totalMessages: Int,
    val totalCorrections: Int,
    val correctionsDetected: Int,
    val correctionsMissed: Int,
    val falsePositives: Int,
    val phaseTransitions: List<PhaseTransitionMetric>,
    val topicChanges: Int,
    val vocabularyItemsIntroduced: Int,
    val averageResponseTimeMs: Long
)

/**
 * A phase transition that occurred during the conversation.
 */
data class PhaseTransitionMetric(
    val atTurnIndex: Int,
    val fromPhase: String,
    val toPhase: String,
    val wasExpected: Boolean
)
