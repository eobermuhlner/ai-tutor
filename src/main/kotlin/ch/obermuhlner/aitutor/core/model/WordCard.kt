package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class WordCard(
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Title/word in the learner's source language (e.g., English).")
    val titleSourceLanguage: String,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Title/word in the target language being learned.")
    val titleTargetLanguage: String,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Description or explanation in the learner's source language.")
    val descriptionSourceLanguage: String,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Description or explanation in the target language being learned.")
    val descriptionTargetLanguage: String,
    @field:JsonProperty(required = true)
    @field:JsonPropertyDescription("Simple English concept identifier for image lookup (e.g., 'apple', 'running', 'coffee-cup').")
    val conceptName: String? = null
)
