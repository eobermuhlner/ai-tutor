package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonClassDescription("""
Example for an error correction of the user message "Ich gehen nach Hause":
{
  "span": "gehen",
  "errorType": "Agreement",
  "severity": "Medium",
  "correctedTargetLanguage": "gehe",
  "whySourceLanguage": "The verb must agree with 'ich' (I). Use 'gehe' (first person singular) instead of 'gehen' (infinitive)",
  "whyTargetLanguage": "Das Verb muss mit 'ich' übereinstimmen. Verwende 'gehe' (erste Person Singular) statt 'gehen' (Infinitiv)"
}
""")
data class Correction(
    @field:JsonPropertyDescription("Exact string of the learner's error.")
    val span: String,
    val errorType: ErrorType,
    @field:JsonPropertyDescription("Severity of the error based on comprehension impact. Consider chat context: missing accents, capitalization, or end punctuation in casual chat should be Low or ignored.")
    val severity: ErrorSeverity,
    @field:JsonPropertyDescription("Corrected form of the exact string of the learner's error in {targetLanguage}.")
    val correctedTargetLanguage: String,
    @field:JsonPropertyDescription("Simple explanation of the error in {sourceLanguage}, appropriate for the estimatedCEFRLevel of the learner.")
    val whySourceLanguage: String,
    @field:JsonPropertyDescription("Simple explanation of the error in {targetLanguage}, appropriate for the estimatedCEFRLevel of the learner.")
    val whyTargetLanguage: String,
)
