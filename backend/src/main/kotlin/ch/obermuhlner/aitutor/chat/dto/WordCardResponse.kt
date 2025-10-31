package ch.obermuhlner.aitutor.chat.dto

data class WordCardResponse(
    val titleSourceLanguage: String,
    val titleTargetLanguage: String,
    val descriptionSourceLanguage: String,
    val descriptionTargetLanguage: String,
    val conceptName: String? = null,
    val imageUrl: String? = null
)
