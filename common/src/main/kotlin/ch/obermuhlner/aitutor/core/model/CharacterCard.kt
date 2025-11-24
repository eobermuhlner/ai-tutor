package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class CharacterCard(
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Character(s) in target language shown on front (e.g., 'あ', 'あい', 'Д', '食'). Typically 1-3 characters.")
    val character: String,

    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Pronunciation/romanization shown on back (e.g., 'a', 'ai', 'd', 'shoku').")
    val pronunciation: String,

    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Description in learner's source language (e.g., pronunciation guide, usage notes, stroke order, mnemonics, examples - whatever is most helpful for this character).")
    val description: String
)