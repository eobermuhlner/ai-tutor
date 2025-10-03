package ch.obermuhlner.aitutor.core.model

data class NewVocabulary(
    val lemma: String,
    val context: String,
    val conceptName: String? = null,
)
