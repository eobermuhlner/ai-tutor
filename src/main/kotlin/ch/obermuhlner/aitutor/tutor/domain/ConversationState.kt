package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationState(
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
    @field:JsonPropertyDescription("The current topic of conversation, or null if no specific topic.")
    val currentTopic: String? = null,

    // Optional metadata fields for prompt context (backward compatible with default values)
    @field:JsonPropertyDescription("Explanation for why this phase was selected (for prompt context).")
    val phaseReason: String? = null,
    @field:JsonPropertyDescription("Topic eligibility status for hysteresis tracking.")
    val topicEligibilityStatus: String? = null,
    @field:JsonPropertyDescription("List of recently discussed topics to prevent repetition.")
    val pastTopics: List<String> = emptyList()
)