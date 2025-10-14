package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription

@JsonClassDescription("""
Correction examples for common error patterns across languages:

Example 1 - Agreement error (Spanish, subject-verb mismatch):
User: "Yo vive en Madrid"
{
  "span": "vive",
  "errorType": "Agreement",
  "severity": "Medium",
  "correctedTargetLanguage": "vivo",
  "whySourceLanguage": "With 'yo' (I), use 'vivo' (first person), not 'vive' (third person)",
  "whyTargetLanguage": "Con 'yo', usa 'vivo' (primera persona), no 'vive' (tercera persona)"
}

Example 2 - Agreement error (German, subject-verb mismatch):
User: "Ich gehen nach Hause"
{
  "span": "gehen",
  "errorType": "Agreement",
  "severity": "Medium",
  "correctedTargetLanguage": "gehe",
  "whySourceLanguage": "The verb must agree with 'ich' (I). Use 'gehe' (first person singular) instead of 'gehen' (infinitive)",
  "whyTargetLanguage": "Das Verb muss mit 'ich' übereinstimmen. Verwende 'gehe' (erste Person Singular) statt 'gehen' (Infinitiv)"
}

Example 3 - Agreement error (English, subject-verb mismatch):
User: "He go to school"
{
  "span": "go",
  "errorType": "Agreement",
  "severity": "Medium",
  "correctedTargetLanguage": "goes",
  "whySourceLanguage": "With 'he', use 'goes' (third person singular), not 'go'",
  "whyTargetLanguage": "With 'he', use 'goes' (third person singular), not 'go'"
}

Example 4 - TenseAspect error (NOT Agreement, tense choice wrong):
User: "I go to school yesterday"
{
  "span": "go",
  "errorType": "TenseAspect",
  "severity": "High",
  "correctedTargetLanguage": "went",
  "whySourceLanguage": "'Yesterday' requires past tense. Use 'went' instead of 'go'",
  "whyTargetLanguage": "'Yesterday' requires past tense. Use 'went' instead of 'go'"
}

CLASSIFICATION DECISION TREE:
1. Is there a subject-verb person/number mismatch? → Agreement
2. Is the tense choice wrong for time context? → TenseAspect
3. Is it word formation/irregular form? → Morphology
4. Other issues → use appropriate specific type
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
