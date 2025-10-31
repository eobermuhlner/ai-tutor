package ch.obermuhlner.aitutor.lesson.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel

data class LessonContent(
    val id: String,
    val title: String,
    val weekNumber: Int?,
    val estimatedDuration: String?,
    val focusAreas: List<String>,
    val targetCEFR: CEFRLevel,
    val goals: List<String>,
    val grammarPoints: List<GrammarPoint>,
    val essentialVocabulary: List<VocabEntry>,
    val conversationScenarios: List<Scenario>,
    val practicePatterns: List<String>,
    val commonMistakes: List<String>,
    val fullMarkdown: String  // Complete markdown for preview
)

data class GrammarPoint(
    val title: String,
    val rule: String,
    val examples: List<String>,
    val patterns: List<String>
)

data class VocabEntry(
    val word: String,
    val translation: String,
    val contextExample: String? = null
)

data class Scenario(
    val title: String,
    val dialogue: String
)
