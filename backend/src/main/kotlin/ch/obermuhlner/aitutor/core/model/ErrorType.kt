package ch.obermuhlner.aitutor.core.model

import com.fasterxml.jackson.annotation.JsonPropertyDescription

enum class ErrorType {
    @JsonPropertyDescription("""
        Subject-verb agreement: person/number mismatch between subject and verb.
        CRITICAL RULE: Subject pronoun + verb with wrong person/number ending = Agreement.
        Examples: "yo tiene" → "yo tengo", "I are" → "I am", "he go" → "he goes", "yo vive" → "yo vivo", "yo trabaja" → "yo trabajo"
        NOT Morphology (word formation), NOT TenseAspect (tense choice).
        Test: Does the subject and verb disagree on person/number? → Agreement
    """)
    Agreement,

    @JsonPropertyDescription("""
        Wrong tense/aspect CHOICE for time context (not conjugation errors).
        Examples: "I go yesterday" → "I went" (wrong tense for past context), "Fui ahora" → "voy" (wrong tense for "now")
        Test: Is the tense selection wrong for the time context? → TenseAspect
        NOT for "yo vive" (present is correct tense, conjugation is wrong → Agreement)
    """)
    TenseAspect,

    @JsonPropertyDescription("""
        Word formation, derivational morphology, irregular forms.
        Use ONLY when there's no subject-verb agreement issue.
        Examples: "goed" → "went", "childs" → "children", "sheeps" → "sheep"
        NOT for regular conjugation errors with clear subjects (those are Agreement).
    """)
    Morphology,

    @JsonPropertyDescription("syntax, word order problems, misplaced words/clauses")
    WordOrder,

    @JsonPropertyDescription("wrong vocabulary, false friend, register mismatch, word choice errors")
    Lexis,

    @JsonPropertyDescription("missing/wrong/unnecessary article/determiner (a/an/the, un/una/el/la, der/die/das)")
    Articles,

    @JsonPropertyDescription("wrong pronoun form or reference (he/him, yo/me, ich/mich)")
    Pronouns,

    @JsonPropertyDescription("wrong or missing preposition (in/on/at, en/a/de, in/auf/an)")
    Prepositions,

    @JsonPropertyDescription("spelling, diacritics, capitalization, punctuation - be very selective, ignore chat-acceptable omissions")
    Typography
}
