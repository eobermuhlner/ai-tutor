package ch.obermuhlner.aitutor.chat.dto

data class VocabularyWithImageResponse(
    val lemma: String,
    val context: String,
    val conceptName: String? = null,
    val imageUrl: String? = null
)
