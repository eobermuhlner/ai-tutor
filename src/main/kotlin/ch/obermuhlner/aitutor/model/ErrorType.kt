package ch.obermuhlner.aitutor.model

enum class ErrorType {
    TenseAspect,      // wrong tense or aspect
    Agreement,        // subject-verb, gender, number
    WordOrder,        // syntax, misplaced words/clauses
    Lexis,            // wrong vocabulary, false friend, register
    Morphology,       // incorrect endings, cases, conjugations
    Articles,         // missing/wrong/unnecessary article/determiner
    Pronouns,         // wrong form or reference
    Prepositions,     // wrong or missing preposition
    Typography        // spelling, diacritics, capitalization, punctuation
}
