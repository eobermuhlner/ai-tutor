package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class ErrorType {
    @JsonPropertyDescription("Wrong tense or aspect")
    TenseAspect,
    @JsonPropertyDescription("No agreement of subject-verb, gender, number")
    Agreement,
    @JsonPropertyDescription("syntax, misplaced words/clauses")
    WordOrder,
    @JsonPropertyDescription("wrong vocabulary, false friend, register")
    Lexis,
    @JsonPropertyDescription("incorrect endings, cases, conjugations")
    Morphology,
    @JsonPropertyDescription("missing/wrong/unnecessary article/determiner")
    Articles,
    @JsonPropertyDescription("wrong form or reference")
    Pronouns,
    @JsonPropertyDescription("wrong or missing preposition")
    Prepositions,
    @JsonPropertyDescription("spelling, diacritics, capitalization, punctuation")
    Typography
}
