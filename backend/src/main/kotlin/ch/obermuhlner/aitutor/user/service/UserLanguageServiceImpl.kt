package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserLanguageServiceImpl(
    private val userLanguageProficiencyRepository: UserLanguageProficiencyRepository
) : UserLanguageService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun addLanguage(
        userId: UUID,
        languageCode: String,
        type: LanguageProficiencyType,
        cefrLevel: CEFRLevel?,
        isNative: Boolean
    ): UserLanguageProficiencyEntity {
        logger.info("Adding language for user $userId: $languageCode (type=$type, level=$cefrLevel, native=$isNative)")

        // Check if already exists
        val existing = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode)
        if (existing != null) {
            logger.debug("Language $languageCode already exists for user $userId")
            return existing
        }

        val entity = UserLanguageProficiencyEntity(
            userId = userId,
            languageCode = languageCode,
            proficiencyType = type,
            cefrLevel = cefrLevel,
            isNative = isNative,
            isPrimary = false,  // Set explicitly via setPrimaryLanguage
            selfAssessed = true,
            lastAssessedAt = Instant.now()
        )

        val saved = userLanguageProficiencyRepository.save(entity)
        logger.info("Language added successfully: ${saved.languageCode} for user $userId")
        return saved
    }

    @Transactional
    override fun updateLanguage(userId: UUID, languageCode: String, cefrLevel: CEFRLevel): UserLanguageProficiencyEntity {
        val entity = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode)
            ?: throw IllegalArgumentException("Language proficiency not found for user $userId and language $languageCode")

        entity.cefrLevel = cefrLevel
        entity.lastAssessedAt = Instant.now()

        return userLanguageProficiencyRepository.save(entity)
    }

    override fun getUserLanguages(userId: UUID): List<UserLanguageProficiencyEntity> {
        return userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId)
    }

    override fun getNativeLanguages(userId: UUID): List<UserLanguageProficiencyEntity> {
        return userLanguageProficiencyRepository.findByUserIdAndIsNativeTrue(userId)
    }

    override fun getLearningLanguages(userId: UUID): List<UserLanguageProficiencyEntity> {
        return userLanguageProficiencyRepository.findByUserIdAndProficiencyType(userId, LanguageProficiencyType.Learning)
    }

    override fun getPrimaryLanguage(userId: UUID): UserLanguageProficiencyEntity? {
        return userLanguageProficiencyRepository.findByUserIdAndIsPrimaryTrue(userId)
    }

    @Transactional
    override fun setPrimaryLanguage(userId: UUID, languageCode: String) {
        // Clear existing primary
        val allLanguages = userLanguageProficiencyRepository.findByUserIdOrderByIsNativeDescCefrLevelDesc(userId)
        allLanguages.forEach { it.isPrimary = false }
        userLanguageProficiencyRepository.saveAll(allLanguages)

        // Set new primary
        val entity = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode)
            ?: throw IllegalArgumentException("Language proficiency not found for user $userId and language $languageCode")

        entity.isPrimary = true
        userLanguageProficiencyRepository.save(entity)
    }

    @Transactional
    override fun removeLanguage(userId: UUID, languageCode: String) {
        val entity = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode)
            ?: return

        userLanguageProficiencyRepository.delete(entity)
    }

    override fun suggestSourceLanguage(userId: UUID, targetLanguageCode: String): String {
        // Try primary language first
        val primary = getPrimaryLanguage(userId)
        if (primary != null && primary.languageCode != targetLanguageCode) {
            return primary.languageCode
        }

        // Try first native language
        val natives = getNativeLanguages(userId)
        val nativeNonTarget = natives.firstOrNull { it.languageCode != targetLanguageCode }
        if (nativeNonTarget != null) {
            return nativeNonTarget.languageCode
        }

        // Default to English
        return "en"
    }

    @Transactional
    override fun inferFromSession(userId: UUID, session: ChatSessionEntity) {
        // Infer source language as native
        val sourceExists = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, session.sourceLanguageCode)
        if (sourceExists == null) {
            addLanguage(
                userId = userId,
                languageCode = session.sourceLanguageCode,
                type = LanguageProficiencyType.Native,
                cefrLevel = null,
                isNative = true
            )
        }

        // Infer target language as learning
        val targetExists = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, session.targetLanguageCode)
        if (targetExists == null) {
            addLanguage(
                userId = userId,
                languageCode = session.targetLanguageCode,
                type = LanguageProficiencyType.Learning,
                cefrLevel = session.estimatedCEFRLevel,
                isNative = false
            )
        }
    }
}
