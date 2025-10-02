package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationState(
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
)