package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewVocabulary(
    @field:JsonProperty(required = true)
    val lemma: String,
    @field:JsonProperty(required = true)
    val context: String,
    @field:JsonProperty(required = true)
    val conceptName: String? = null,
)
