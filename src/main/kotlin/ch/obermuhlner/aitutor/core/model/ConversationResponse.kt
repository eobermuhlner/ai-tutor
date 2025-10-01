package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationResponse(
    @field:JsonPropertyDescription("Current state of the conversation.")
    val conversationState: ConversationState,
    @field:JsonPropertyDescription("Corrections of learner's errors.")
    val corrections: List<Correction>,
    val newVocabulary: List<NewVocabulary>,
)
