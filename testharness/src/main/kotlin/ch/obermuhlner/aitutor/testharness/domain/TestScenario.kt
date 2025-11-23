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
    val expectedBehaviors: List<String>,
    val testSteps: List<TestStep>
)

data class LearnerPersona(
    val name: String,
    val level: String,
    val learningStyle: String,
    val goals: List<String>,
    val commonMistakes: List<String>,
    val personality: String
)

data class TestStep(
    val stepNumber: Int,
    val action: String, // "learner_speaks", "tutor_responds", etc.
    val expectedOutcome: String,
    val evaluationCriteria: List<String>
)