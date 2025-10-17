package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationResponse(
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Current state of the conversation.")
    val conversationState: ConversationState,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Corrections of learner's errors.")
    val corrections: List<Correction>,
    @field:JsonProperty(required = true)
    val newVocabulary: List<NewVocabulary>,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Word cards to help learn new vocabulary or concepts.")
    val wordCards: List<WordCard> = emptyList(),
)
