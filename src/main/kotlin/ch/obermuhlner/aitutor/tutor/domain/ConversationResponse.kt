package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationResponse(
    @field:JsonPropertyDescription("Current state of the conversation.")
    val conversationState: ConversationState,
    @field:JsonPropertyDescription("Corrections of learner's errors.")
    val corrections: List<Correction>,
    val newVocabulary: List<NewVocabulary>,
    @field:JsonPropertyDescription("Word cards to help learn new vocabulary or concepts.")
    val wordCards: List<WordCard> = emptyList(),
)
