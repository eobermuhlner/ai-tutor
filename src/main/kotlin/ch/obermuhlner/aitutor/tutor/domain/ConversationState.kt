package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationState(
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("The current topic of conversation, or null if no specific topic.")
    val currentTopic: String? = null,

    // Optional metadata fields for prompt context (backward compatible with default values)
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Explanation for why this phase was selected (for prompt context).")
    val phaseReason: String? = null,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Topic eligibility status for hysteresis tracking.")
    val topicEligibilityStatus: String? = null,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("List of recently discussed topics to prevent repetition.")
    val pastTopics: List<String> = emptyList(),

    // Vocabulary review mode
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Whether vocabulary review mode is enabled for this session. When true, the tutor should naturally integrate due vocabulary review into conversation.")
    val vocabularyReviewMode: Boolean = false,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Count of vocabulary items due for review (for context).")
    val dueVocabularyCount: Long? = null,

    // Lesson switching control
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Request to change lesson: 'next' (advance to next lesson), 'previous' (go back one lesson), null (remain on current lesson, default). Set when learner explicitly requests lesson change or when tutor determines learner is ready for advancement based on demonstrated mastery.")
    val requestedLessonAction: String? = null,

    // Tutor-initiated conversation context
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Context for tutor-initiated messages: 'welcome' (first message in a new course), 'reengage' (restarting conversation after inactivity), or null (normal user-initiated message).")
    val initiationContext: String? = null
)