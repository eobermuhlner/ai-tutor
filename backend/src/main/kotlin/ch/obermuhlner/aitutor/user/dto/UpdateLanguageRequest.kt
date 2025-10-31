package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel

data class UpdateLanguageRequest(
    val cefrLevel: CEFRLevel
)
