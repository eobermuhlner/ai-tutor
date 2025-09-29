package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationState(
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
)