package ch.obermuhlner.aitutor.lesson.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel

data class LessonContentResponse(
    val id: String,
    val title: String,
    val weekNumber: Int?,
    val estimatedDuration: String?,
    val focusAreas: List<String>,
    val targetCEFR: CEFRLevel,
    val goals: List<String>,
    val grammarPoints: List<GrammarPointResponse>,
    val essentialVocabulary: List<VocabEntryResponse>,
    val conversationScenarios: List<ScenarioResponse>,
    val practicePatterns: List<String>,
    val commonMistakes: List<String>,
    val fullMarkdown: String
)

data class GrammarPointResponse(
    val title: String,
    val rule: String,
    val examples: List<String>,
    val patterns: List<String>
)

data class VocabEntryResponse(
    val word: String,
    val translation: String,
    val contextExample: String?
)

data class ScenarioResponse(
    val title: String,
    val dialogue: String
)
