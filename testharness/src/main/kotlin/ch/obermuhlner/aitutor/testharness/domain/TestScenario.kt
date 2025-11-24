package ch.obermuhlner.aitutor.testharness.domain

data class EvaluationResult(
    val rating: Int, // 1-10 scale
    val feedback: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val pedagogicalScore: Int, // 1-10 scale
    val accuracyScore: Int, // 1-10 scale
    val engagementScore: Int // 1-10 scale
)

data class TestScenario(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val level: String,
    val topic: String,
    val objective: String,
    val learnerPersona: LearnerPersona,
    val tutorConfig: TutorConfig?,
    val conversationScript: List<ConversationMessage>,
    val expectedOutcomes: ExpectedOutcomes?,
    val evaluationFocus: List<String>
)

data class LearnerPersona(
    val name: String,
    val level: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val learningGoals: List<String>,
    val commonErrors: List<String>
)

data class TutorConfig(
    val tutorName: String,
    val initialPhase: String,
    val teachingStyle: String
)

data class ConversationMessage(
    val content: String,
    val notes: String?
)

data class ExpectedOutcomes(
    val phaseTransitions: List<PhaseTransition>
)

data class PhaseTransition(
    val afterMessageIndex: Int,
    val toPhase: String,
    val reason: String
)