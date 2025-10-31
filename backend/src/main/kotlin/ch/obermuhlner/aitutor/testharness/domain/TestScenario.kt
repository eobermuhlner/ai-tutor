package ch.obermuhlner.aitutor.testharness.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase

/**
 * A test scenario defining a learner persona and conversation script.
 */
data class TestScenario(
    val id: String,
    val name: String,
    val description: String,
    val learnerPersona: LearnerPersona,
    val tutorConfig: TutorConfig,
    val conversationScript: List<LearnerMessage>,
    val expectedOutcomes: ExpectedOutcomes,
    val evaluationFocus: List<EvaluationFocus>
)

/**
 * Learner persona with proficiency and characteristics.
 */
data class LearnerPersona(
    val name: String,
    val cefrLevel: CEFRLevel,
    val sourceLanguage: String,
    val targetLanguage: String,
    val commonErrors: List<String> = emptyList(),
    val learningGoals: List<String> = emptyList()
)

/**
 * Tutor configuration for the test session.
 */
data class TutorConfig(
    val tutorName: String = "TestTutor",
    val initialPhase: ConversationPhase = ConversationPhase.Auto,
    val teachingStyle: String = "Guided"
)

/**
 * A single learner message in the conversation script.
 */
data class LearnerMessage(
    val content: String,
    val intentionalErrors: List<IntentionalError> = emptyList(),
    val notes: String? = null
)

/**
 * An intentional error embedded in a learner message.
 */
data class IntentionalError(
    val span: String,
    val errorType: String,
    val expectedSeverity: String,
    val correctForm: String,
    val reasoning: String
)

/**
 * Expected outcomes for the scenario.
 */
data class ExpectedOutcomes(
    val phaseTransitions: List<ExpectedPhaseTransition> = emptyList(),
    val minimumCorrectionsDetected: Int = 0,
    val topicChanges: Int? = null,
    val vocabularyItems: Int? = null,
    val shouldTriggerDrillPhase: Boolean? = null,
    val shouldMaintainFreePhase: Boolean? = null
)

/**
 * Expected phase transition during the conversation.
 */
data class ExpectedPhaseTransition(
    val afterMessageIndex: Int,
    val toPhase: ConversationPhase,
    val reason: String
)

/**
 * Focus areas for LLM judge evaluation.
 */
enum class EvaluationFocus {
    ERROR_DETECTION,
    PHASE_APPROPRIATENESS,
    CORRECTION_QUALITY,
    ENCOURAGEMENT_BALANCE,
    TOPIC_MANAGEMENT,
    VOCABULARY_TEACHING,
    COMPREHENSIBILITY,
    FOSSILIZATION_DETECTION
}
