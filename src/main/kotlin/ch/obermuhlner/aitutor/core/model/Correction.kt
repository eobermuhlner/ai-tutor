package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class Correction(
    @field:JsonPropertyDescription("Exact string of the learner's error.")
    val span: String,
    val errorType: ErrorType,
    @field:JsonPropertyDescription("Corrected form of the exact string of the learner's error in {targetLanguage}.")
    val correctedTargetLanguage: String,
    @field:JsonPropertyDescription("Simple explanation of the error in {sourceLanguage}, appropriate for the estimatedCEFRLevel of the learner.")
    val whySourceLanguage: String,
    @field:JsonPropertyDescription("Simple explanation of the error in {targetLanguage}, appropriate for the estimatedCEFRLevel of the learner.")
    val whyTargetLanguage: String,
)
