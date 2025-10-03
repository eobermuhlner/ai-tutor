package ch.obermuhlner.aitutor.vocabulary.dto

data class NewVocabularyDTO(
    val lemma: String,
    val context: String,
    val conceptName: String? = null
)
