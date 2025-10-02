package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType

data class AddLanguageRequest(
    val languageCode: String,
    val type: LanguageProficiencyType,
    val cefrLevel: CEFRLevel? = null,
    val isNative: Boolean = false
)
