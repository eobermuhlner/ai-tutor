package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.CharacterCard
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import ch.obermuhlner.aitutor.core.model.WordCard
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

/**
 * Response from LLM containing conversational state and pedagogical data.
 * Corrections are now handled separately via CorrectionResponse to reduce LLM context.
 */
data class ConversationResponse(
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Current state of the conversation.")
    val conversationState: ConversationState,
    @field:JsonProperty(required = true)
    val newVocabulary: List<NewVocabulary>,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Word cards to help learn new vocabulary or concepts. Also useful to teach single characters in other writing systems (cyrillic, hiragana, katagana, kanji, hangul, hanzi, ...)")
    val wordCards: List<WordCard> = emptyList(),
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Character cards to help learn individual characters/symbols (e.g., hiragana, cyrillic, kanji).")
    val characterCards: List<CharacterCard> = emptyList(),
)
