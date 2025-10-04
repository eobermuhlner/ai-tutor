package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import java.util.UUID

interface UserLanguageService {
    fun addLanguage(
        userId: UUID,
        languageCode: String,
        type: LanguageProficiencyType,
        cefrLevel: CEFRLevel? = null,
        isNative: Boolean = false
    ): UserLanguageProficiencyEntity

    fun updateLanguage(userId: UUID, languageCode: String, cefrLevel: CEFRLevel): UserLanguageProficiencyEntity
    fun getUserLanguages(userId: UUID): List<UserLanguageProficiencyEntity>
    fun getNativeLanguages(userId: UUID): List<UserLanguageProficiencyEntity>
    fun getLearningLanguages(userId: UUID): List<UserLanguageProficiencyEntity>
    fun getPrimaryLanguage(userId: UUID): UserLanguageProficiencyEntity?
    fun setPrimaryLanguage(userId: UUID, languageCode: String)
    fun removeLanguage(userId: UUID, languageCode: String)

    // Smart helpers
    fun suggestSourceLanguage(userId: UUID, targetLanguageCode: String): String
    fun inferFromSession(userId: UUID, session: ChatSessionEntity)
}
