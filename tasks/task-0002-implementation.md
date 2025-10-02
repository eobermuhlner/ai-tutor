# Task 0002: Implementation Plan - Catalog-Based Tutor/Language/Course Management

**Status**: Ready for Implementation
**Based on**: task-0002-requirements.md (Simplified Architecture)
**Timeline**: 4-5 weeks
**Estimated Lines of Code**: ~1,500 lines

---

## Implementation Overview

This document provides a detailed, phase-by-phase implementation plan for the simplified catalog-based system. Each phase builds on the previous one and includes specific files, code snippets, and testing requirements.

---

## Phase 1: Core Domain & Data Layer (Week 1)

### 1.1 Extend ChatSessionEntity

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/domain/ChatSessionEntity.kt`

**Changes**: Add 4 new fields to existing entity

```kotlin
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // ... existing fields ...

    // NEW: Course-related fields (nullable for backward compatibility)
    @Column(name = "course_template_id")
    var courseTemplateId: UUID? = null,

    @Column(name = "tutor_profile_id")
    var tutorProfileId: UUID? = null,

    @Column(name = "custom_session_name", length = 256)
    var customName: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    // ... existing timestamp fields ...
)
```

---

### 1.2 Create Domain Models (Enums & Data Classes)

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/core/model/catalog/`

#### 1.2.1 LanguageProficiencyType.kt
```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

enum class LanguageProficiencyType {
    Native,      // Native speaker (no CEFR level needed)
    Learning,    // Currently learning (has CEFR level)
}
```

#### 1.2.2 Difficulty.kt
```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

enum class Difficulty {
    Easy,
    Medium,
    Hard
}
```

#### 1.2.3 TutorPersonality.kt
```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

enum class TutorPersonality {
    Encouraging,    // Positive reinforcement, patient
    Strict,         // High standards, formal
    Casual,         // Friendly, informal
    Professional,   // Business-focused, formal but approachable
    Academic        // Scholarly, detailed explanations
}
```

#### 1.2.4 CourseCategory.kt
```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

enum class CourseCategory {
    Travel,
    Business,
    Conversational,
    Grammar,
    ExamPrep,
    Hobby,
    Academic,
    General
}
```

#### 1.2.5 LanguageMetadata.kt
```kotlin
package ch.obermuhlner.aitutor.core.model.catalog

/**
 * Language metadata stored in configuration (not database).
 * Used by LanguageConfig to provide language information.
 */
data class LanguageMetadata(
    val code: String,              // ISO 639-1 (e.g., "es", "fr")
    val nameJson: String,          // JSON map: {"en": "Spanish", "es": "Español", "de": "Spanisch"}
    val flagEmoji: String,         // Unicode flag emoji
    val nativeName: String,        // Native language name (e.g., "Español")
    val difficulty: Difficulty,
    val descriptionJson: String    // JSON map of descriptions
)
```

---

### 1.3 Create JPA Entities

#### 1.3.1 TutorProfileEntity.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/domain/`

```kotlin
package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tutor_profiles")
class TutorProfileEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 128)
    var name: String,

    @Column(name = "emoji", nullable = false, length = 16)
    var emoji: String,

    // For AI system prompt (English only, always required)
    @Column(name = "persona_english", nullable = false, length = 256)
    var personaEnglish: String,

    @Column(name = "domain_english", nullable = false, length = 256)
    var domainEnglish: String,

    @Column(name = "description_english", nullable = false, columnDefinition = "TEXT")
    var descriptionEnglish: String,

    // For UI catalog (multilingual JSON with AI translation fallback)
    @Column(name = "persona_json", nullable = false, columnDefinition = "TEXT")
    var personaJson: String,

    @Column(name = "domain_json", nullable = false, columnDefinition = "TEXT")
    var domainJson: String,

    @Column(name = "description_json", nullable = false, columnDefinition = "TEXT")
    var descriptionJson: String,

    @Column(name = "cultural_background_json", columnDefinition = "TEXT")
    var culturalBackgroundJson: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "personality", nullable = false, length = 32)
    var personality: TutorPersonality,

    @Column(name = "target_language_code", nullable = false, length = 32)
    var targetLanguageCode: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

#### 1.3.2 CourseTemplateEntity.kt

```kotlin
package ch.obermuhlner.aitutor.catalog.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "course_templates")
class CourseTemplateEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "language_code", nullable = false, length = 32)
    var languageCode: String,

    @Column(name = "name_json", nullable = false, columnDefinition = "TEXT")
    var nameJson: String,

    @Column(name = "short_description_json", nullable = false, columnDefinition = "TEXT")
    var shortDescriptionJson: String,

    @Column(name = "description_json", nullable = false, columnDefinition = "TEXT")
    var descriptionJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    var category: CourseCategory,

    @Column(name = "target_audience_json", nullable = false, columnDefinition = "TEXT")
    var targetAudienceJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "starting_level", nullable = false, length = 8)
    var startingLevel: CEFRLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_level", nullable = false, length = 8)
    var targetLevel: CEFRLevel,

    @Column(name = "estimated_weeks")
    var estimatedWeeks: Int? = null,

    @Column(name = "suggested_tutor_ids", columnDefinition = "TEXT")
    var suggestedTutorIdsJson: String? = null,  // JSON array of UUIDs

    @Enumerated(EnumType.STRING)
    @Column(name = "default_phase", nullable = false, length = 16)
    var defaultPhase: ConversationPhase = ConversationPhase.Auto,

    @Column(name = "topic_sequence", columnDefinition = "TEXT")
    var topicSequenceJson: String? = null,  // JSON array of topics

    @Column(name = "learning_goals_json", nullable = false, columnDefinition = "TEXT")
    var learningGoalsJson: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "tags", columnDefinition = "TEXT")
    var tagsJson: String? = null,  // JSON array of strings

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

#### 1.3.3 UserLanguageProficiencyEntity.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/user/domain/`

```kotlin
package ch.obermuhlner.aitutor.user.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "user_language_proficiency",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "language_code"])
    ]
)
class UserLanguageProficiencyEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "language_code", nullable = false, length = 32)
    var languageCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_type", nullable = false, length = 32)
    var proficiencyType: LanguageProficiencyType,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 8)
    var cefrLevel: CEFRLevel? = null,

    @Column(name = "is_native", nullable = false)
    var isNative: Boolean = false,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "self_assessed", nullable = false)
    var selfAssessed: Boolean = true,

    @Column(name = "last_assessed_at")
    var lastAssessedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

---

### 1.4 Create Repositories

#### 1.4.1 TutorProfileRepository.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/repository/`

```kotlin
package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TutorProfileRepository : JpaRepository<TutorProfileEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<TutorProfileEntity>
    fun findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<TutorProfileEntity>
}
```

#### 1.4.2 CourseTemplateRepository.kt

```kotlin
package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseTemplateRepository : JpaRepository<CourseTemplateEntity, UUID> {
    fun findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<CourseTemplateEntity>
    fun findByCategoryAndIsActiveTrue(category: CourseCategory): List<CourseTemplateEntity>
    fun findByStartingLevelLessThanEqualAndIsActiveTrue(level: CEFRLevel): List<CourseTemplateEntity>
}
```

#### 1.4.3 UserLanguageProficiencyRepository.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/user/repository/`

```kotlin
package ch.obermuhlner.aitutor.user.repository

import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserLanguageProficiencyRepository : JpaRepository<UserLanguageProficiencyEntity, UUID> {
    fun findByUserIdOrderByIsNativeDescCefrLevelDesc(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndLanguageCode(userId: UUID, languageCode: String): UserLanguageProficiencyEntity?
    fun findByUserIdAndIsNativeTrue(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndIsPrimaryTrue(userId: UUID): UserLanguageProficiencyEntity?
    fun findByUserIdAndProficiencyType(userId: UUID, type: LanguageProficiencyType): List<UserLanguageProficiencyEntity>
}
```

#### 1.4.4 Extend ChatSessionRepository.kt

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/repository/ChatSessionRepository.kt`

**Add new methods:**

```kotlin
@Repository
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, UUID> {
    // ... existing methods ...

    // NEW: Course-related queries
    fun findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId: UUID): List<ChatSessionEntity>
    fun findByUserIdAndCourseTemplateIdAndIsActiveTrue(userId: UUID, courseTemplateId: UUID): ChatSessionEntity?
    fun findByUserIdAndTutorProfileIdAndIsActiveTrue(userId: UUID, tutorProfileId: UUID): List<ChatSessionEntity>
}
```

---

### 1.5 Database Migration

**File**: `src/main/resources/db/migration/V3__add_catalog_tables.sql`

```sql
-- Extend chat_sessions table with course metadata
ALTER TABLE chat_sessions
ADD COLUMN course_template_id UUID,
ADD COLUMN tutor_profile_id UUID,
ADD COLUMN custom_session_name VARCHAR(256),
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create tutor_profiles table
CREATE TABLE tutor_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    persona_english VARCHAR(256) NOT NULL,
    domain_english VARCHAR(256) NOT NULL,
    description_english TEXT NOT NULL,
    persona_json TEXT NOT NULL,
    domain_json TEXT NOT NULL,
    description_json TEXT NOT NULL,
    cultural_background_json TEXT,
    personality VARCHAR(32) NOT NULL,
    target_language_code VARCHAR(32) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tutor_profiles_target_language ON tutor_profiles(target_language_code);
CREATE INDEX idx_tutor_profiles_active_display ON tutor_profiles(is_active, display_order);

-- Create course_templates table
CREATE TABLE course_templates (
    id UUID PRIMARY KEY,
    language_code VARCHAR(32) NOT NULL,
    name_json TEXT NOT NULL,
    short_description_json TEXT NOT NULL,
    description_json TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    target_audience_json TEXT NOT NULL,
    starting_level VARCHAR(8) NOT NULL,
    target_level VARCHAR(8) NOT NULL,
    estimated_weeks INT,
    suggested_tutor_ids_json TEXT,
    default_phase VARCHAR(16) NOT NULL DEFAULT 'Auto',
    topic_sequence_json TEXT,
    learning_goals_json TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    tags_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_course_templates_language ON course_templates(language_code);
CREATE INDEX idx_course_templates_category ON course_templates(category);
CREATE INDEX idx_course_templates_active_display ON course_templates(is_active, display_order);

-- Create user_language_proficiency table
CREATE TABLE user_language_proficiency (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    language_code VARCHAR(32) NOT NULL,
    proficiency_type VARCHAR(32) NOT NULL,
    cefr_level VARCHAR(8),
    is_native BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    self_assessed BOOLEAN NOT NULL DEFAULT TRUE,
    last_assessed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_language UNIQUE (user_id, language_code)
);

CREATE INDEX idx_user_language_proficiency_user ON user_language_proficiency(user_id);
CREATE INDEX idx_user_language_proficiency_primary ON user_language_proficiency(user_id, is_primary);

-- Add foreign key constraints
ALTER TABLE chat_sessions
ADD CONSTRAINT fk_chat_sessions_course_template
    FOREIGN KEY (course_template_id) REFERENCES course_templates(id) ON DELETE SET NULL;

ALTER TABLE chat_sessions
ADD CONSTRAINT fk_chat_sessions_tutor_profile
    FOREIGN KEY (tutor_profile_id) REFERENCES tutor_profiles(id) ON DELETE SET NULL;

ALTER TABLE user_language_proficiency
ADD CONSTRAINT fk_user_language_proficiency_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
```

---

### 1.6 Testing (Phase 1)

**New test files:**

1. `TutorProfileRepositoryTest.kt` - @DataJpaTest
2. `CourseTemplateRepositoryTest.kt` - @DataJpaTest
3. `UserLanguageProficiencyRepositoryTest.kt` - @DataJpaTest
4. `ChatSessionRepositoryTest.kt` - Extend existing with new query tests

---

## Phase 2: Service Layer (Week 2)

### 2.1 Localization Services

#### 2.1.1 LocalizationService.kt (Interface)

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/language/service/`

```kotlin
package ch.obermuhlner.aitutor.language.service

interface LocalizationService {
    /**
     * Get localized text from multilingual JSON.
     * If translation missing, uses AI translation from englishFallback.
     */
    fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,
        fallbackLanguage: String = "en"
    ): String

    /**
     * Parse multilingual JSON string into map.
     */
    fun parseMultilingualJson(jsonText: String): Map<String, String>
}
```

#### 2.1.2 LocalizationServiceImpl.kt

```kotlin
package ch.obermuhlner.aitutor.language.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class LocalizationServiceImpl(
    private val translationService: TranslationService,
    private val objectMapper: ObjectMapper
) : LocalizationService {

    override fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,
        fallbackLanguage: String
    ): String {
        val translations = parseMultilingualJson(jsonText)

        // 1. Try requested language
        translations[languageCode]?.let { return it }

        // 2. Try fallback language (usually English)
        translations[fallbackLanguage]?.let { return it }

        // 3. AI TRANSLATION: Generate on-the-fly
        if (englishFallback.isNotBlank()) {
            return try {
                translationService.translate(englishFallback, "en", languageCode)
            } catch (e: Exception) {
                englishFallback  // Fallback to English if translation fails
            }
        }

        // 4. Last resort
        return "Translation missing"
    }

    override fun parseMultilingualJson(jsonText: String): Map<String, String> {
        return try {
            objectMapper.readValue(jsonText, object : TypeReference<Map<String, String>>() {})
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
```

#### 2.1.3 TranslationService.kt (Interface)

```kotlin
package ch.obermuhlner.aitutor.language.service

interface TranslationService {
    /**
     * Translate text from one language to another using AI.
     */
    fun translate(text: String, from: String, to: String): String

    /**
     * Batch translate multiple texts for efficiency.
     */
    fun batchTranslate(texts: List<String>, from: String, to: String): List<String>
}
```

#### 2.1.4 OpenAITranslationService.kt

```kotlin
package ch.obermuhlner.aitutor.language.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class OpenAITranslationService(
    private val chatClient: ChatClient
) : TranslationService {

    override fun translate(text: String, from: String, to: String): String {
        val prompt = """
            Translate the following text from $from to $to.
            Preserve tone, style, and formality.
            Return ONLY the translation, no explanations.

            Text: $text
        """.trimIndent()

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content() ?: text  // Fallback to original if translation fails
    }

    override fun batchTranslate(texts: List<String>, from: String, to: String): List<String> {
        if (texts.isEmpty()) return emptyList()

        val prompt = """
            Translate each line from $from to $to.
            Preserve tone, style, and formality.
            Return translations in the same order, one per line.

            ${texts.joinToString("\n")}
        """.trimIndent()

        val response = chatClient.prompt()
            .user(prompt)
            .call()
            .content() ?: return texts

        return response.lines().take(texts.size)
    }
}
```

---

### 2.2 Language Configuration

#### 2.2.1 LanguageConfig.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/language/config/`

```kotlin
package ch.obermuhlner.aitutor.language.config

import ch.obermuhlner.aitutor.core.model.catalog.Difficulty
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LanguageConfig {

    @Bean
    fun supportedLanguages(): Map<String, LanguageMetadata> {
        return mapOf(
            "es" to LanguageMetadata(
                code = "es",
                nameJson = """{"en": "Spanish", "es": "Español", "de": "Spanisch", "fr": "Espagnol"}""",
                flagEmoji = "🇪🇸",
                nativeName = "Español",
                difficulty = Difficulty.Easy,
                descriptionJson = """{"en": "One of the most spoken languages worldwide, with clear pronunciation rules", "es": "Uno de los idiomas más hablados del mundo, con reglas de pronunciación claras"}"""
            ),
            "fr" to LanguageMetadata(
                code = "fr",
                nameJson = """{"en": "French", "es": "Francés", "de": "Französisch", "fr": "Français"}""",
                flagEmoji = "🇫🇷",
                nativeName = "Français",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Romance language spoken across five continents", "fr": "Langue romane parlée sur cinq continents"}"""
            ),
            "de" to LanguageMetadata(
                code = "de",
                nameJson = """{"en": "German", "es": "Alemán", "de": "Deutsch", "fr": "Allemand"}""",
                flagEmoji = "🇩🇪",
                nativeName = "Deutsch",
                difficulty = Difficulty.Medium,
                descriptionJson = """{"en": "Germanic language with precise grammar rules", "de": "Germanische Sprache mit präzisen Grammatikregeln"}"""
            ),
            "ja" to LanguageMetadata(
                code = "ja",
                nameJson = """{"en": "Japanese", "es": "Japonés", "de": "Japanisch", "fr": "Japonais", "ja": "日本語"}""",
                flagEmoji = "🇯🇵",
                nativeName = "日本語",
                difficulty = Difficulty.Hard,
                descriptionJson = """{"en": "East Asian language with three writing systems", "ja": "三つの文字体系を持つ東アジアの言語"}"""
            )
        )
    }
}
```

---

### 2.3 User Language Service

#### 2.3.1 UserLanguageService.kt (Interface)

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/user/service/`

```kotlin
package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import java.util.*

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
```

#### 2.3.2 UserLanguageServiceImpl.kt

```kotlin
package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import ch.obermuhlner.aitutor.user.repository.UserLanguageProficiencyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class UserLanguageServiceImpl(
    private val userLanguageProficiencyRepository: UserLanguageProficiencyRepository
) : UserLanguageService {

    @Transactional
    override fun addLanguage(
        userId: UUID,
        languageCode: String,
        type: LanguageProficiencyType,
        cefrLevel: CEFRLevel?,
        isNative: Boolean
    ): UserLanguageProficiencyEntity {
        // Check if already exists
        val existing = userLanguageProficiencyRepository.findByUserIdAndLanguageCode(userId, languageCode)
        if (existing != null) {
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

        return userLanguageProficiencyRepository.save(entity)
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
```

---

### 2.4 Catalog Service

#### 2.4.1 CatalogService.kt (Interface)

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/`

```kotlin
package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import java.util.*

interface CatalogService {
    // Language metadata from configuration
    fun getAvailableLanguages(): List<LanguageMetadata>
    fun getLanguageByCode(code: String): LanguageMetadata?

    // Course browsing
    fun getCoursesForLanguage(languageCode: String, userLevel: CEFRLevel? = null): List<CourseTemplateEntity>
    fun getCourseById(courseId: UUID): CourseTemplateEntity?
    fun getCoursesByCategory(category: CourseCategory): List<CourseTemplateEntity>
    fun searchCourses(query: String, languageCode: String? = null): List<CourseTemplateEntity>

    // Tutor browsing
    fun getTutorsForLanguage(targetLanguageCode: String): List<TutorProfileEntity>
    fun getTutorById(tutorId: UUID): TutorProfileEntity?
    fun getTutorsForCourse(courseTemplateId: UUID): List<TutorProfileEntity>
}
```

#### 2.4.2 CatalogServiceImpl.kt

```kotlin
package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class CatalogServiceImpl(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val supportedLanguages: Map<String, LanguageMetadata>,
    private val objectMapper: ObjectMapper
) : CatalogService {

    override fun getAvailableLanguages(): List<LanguageMetadata> {
        return supportedLanguages.values.toList()
    }

    override fun getLanguageByCode(code: String): LanguageMetadata? {
        return supportedLanguages[code]
    }

    override fun getCoursesForLanguage(languageCode: String, userLevel: CEFRLevel?): List<CourseTemplateEntity> {
        val courses = courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode)

        // Filter by user level if provided
        return if (userLevel != null) {
            courses.filter { it.startingLevel.ordinal <= userLevel.ordinal }
        } else {
            courses
        }
    }

    override fun getCourseById(courseId: UUID): CourseTemplateEntity? {
        return courseTemplateRepository.findById(courseId).orElse(null)
    }

    override fun getCoursesByCategory(category: CourseCategory): List<CourseTemplateEntity> {
        return courseTemplateRepository.findByCategoryAndIsActiveTrue(category)
    }

    override fun searchCourses(query: String, languageCode: String?): List<CourseTemplateEntity> {
        // Simple search implementation - can be enhanced with full-text search
        val allCourses = if (languageCode != null) {
            courseTemplateRepository.findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode)
        } else {
            courseTemplateRepository.findAll()
        }

        return allCourses.filter { course ->
            course.nameJson.contains(query, ignoreCase = true) ||
            course.descriptionJson.contains(query, ignoreCase = true) ||
            course.category.name.contains(query, ignoreCase = true)
        }
    }

    override fun getTutorsForLanguage(targetLanguageCode: String): List<TutorProfileEntity> {
        return tutorProfileRepository.findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(targetLanguageCode)
    }

    override fun getTutorById(tutorId: UUID): TutorProfileEntity? {
        return tutorProfileRepository.findById(tutorId).orElse(null)
    }

    override fun getTutorsForCourse(courseTemplateId: UUID): List<TutorProfileEntity> {
        val course = getCourseById(courseTemplateId) ?: return emptyList()

        // Parse suggested tutor IDs from JSON
        val suggestedIds = course.suggestedTutorIdsJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<List<UUID>>() {})
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        // If no suggested tutors, return all tutors for the language
        if (suggestedIds.isEmpty()) {
            return getTutorsForLanguage(course.languageCode)
        }

        // Return tutors in suggested order
        return suggestedIds.mapNotNull { getTutorById(it) }
    }
}
```

---

### 2.5 Extend ChatService

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt`

**Add these methods to existing ChatService class:**

```kotlin
@Service
class ChatService(
    // ... existing dependencies ...
    private val catalogService: CatalogService  // NEW dependency
) {
    // ... existing methods ...

    @Transactional
    fun createSessionFromCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String,
        customName: String? = null
    ): SessionResponse {
        val course = catalogService.getCourseById(courseTemplateId)
            ?: throw IllegalArgumentException("Course not found: $courseTemplateId")

        val tutor = catalogService.getTutorById(tutorProfileId)
            ?: throw IllegalArgumentException("Tutor not found: $tutorProfileId")

        // Validate tutor teaches the course language
        if (tutor.targetLanguageCode != course.languageCode) {
            throw IllegalArgumentException("Tutor does not teach course language")
        }

        val session = ChatSessionEntity(
            userId = userId,
            tutorName = tutor.name,
            tutorPersona = tutor.personaEnglish,  // Always use English for AI
            tutorDomain = tutor.domainEnglish,
            sourceLanguageCode = sourceLanguageCode,
            targetLanguageCode = course.languageCode,
            conversationPhase = course.defaultPhase,
            estimatedCEFRLevel = course.startingLevel,
            currentTopic = null,
            courseTemplateId = courseTemplateId,
            tutorProfileId = tutorProfileId,
            customName = customName,
            isActive = true
        )

        val saved = chatSessionRepository.save(session)
        return toSessionResponse(saved)
    }

    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse> {
        val sessions = chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)

        return sessions.map { session ->
            val progress = calculateSessionProgress(session.id)
            SessionWithProgressResponse(
                session = toSessionResponse(session),
                progress = progress
            )
        }
    }

    fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
        return calculateSessionProgress(sessionId)
    }

    private fun calculateSessionProgress(sessionId: UUID): SessionProgressResponse {
        val session = chatSessionRepository.findById(sessionId).orElse(null)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        val messageCount = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).size

        val vocabularyCount = vocabularyRepository.findByUserIdAndLang(
            session.userId,
            session.targetLanguageCode
        ).size

        val daysActive = java.time.Duration.between(
            session.createdAt ?: java.time.Instant.now(),
            java.time.Instant.now()
        ).toDays().toInt()

        return SessionProgressResponse(
            sessionId = sessionId,
            messageCount = messageCount,
            vocabularyCount = vocabularyCount,
            daysActive = daysActive,
            lastAccessedAt = session.updatedAt ?: java.time.Instant.now()
        )
    }

    // Update toSessionResponse to include new fields
    private fun toSessionResponse(entity: ChatSessionEntity): SessionResponse {
        return SessionResponse(
            id = entity.id,
            userId = entity.userId,
            tutorName = entity.tutorName,
            tutorPersona = entity.tutorPersona,
            tutorDomain = entity.tutorDomain,
            sourceLanguageCode = entity.sourceLanguageCode,
            targetLanguageCode = entity.targetLanguageCode,
            conversationPhase = entity.conversationPhase,
            estimatedCEFRLevel = entity.estimatedCEFRLevel,
            currentTopic = entity.currentTopic,
            courseTemplateId = entity.courseTemplateId,  // NEW
            tutorProfileId = entity.tutorProfileId,      // NEW
            customName = entity.customName,              // NEW
            isActive = entity.isActive,                  // NEW
            createdAt = entity.createdAt ?: java.time.Instant.now(),
            updatedAt = entity.updatedAt ?: java.time.Instant.now()
        )
    }
}
```

**Note**: Add missing dependency injection:
```kotlin
private val vocabularyRepository: VocabularyItemRepository
```

---

### 2.6 Testing (Phase 2)

**New test files:**

1. `LocalizationServiceTest.kt` - Unit tests with MockK
2. `OpenAITranslationServiceTest.kt` - Unit tests with mocked ChatClient
3. `UserLanguageServiceTest.kt` - Unit tests with MockK
4. `CatalogServiceTest.kt` - Unit tests with MockK
5. `ChatServiceTest.kt` - Extend existing with new method tests

---

## Phase 3: REST API (Week 3)

### 3.1 DTOs

#### User Language DTOs

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/user/dto/`

**File 1: UserLanguageProficiencyResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import java.time.Instant
import java.util.UUID

data class UserLanguageProficiencyResponse(
    val id: UUID,
    val userId: UUID,
    val languageCode: String,
    val proficiencyType: LanguageProficiencyType,
    val cefrLevel: CEFRLevel?,
    val isNative: Boolean,
    val isPrimary: Boolean,
    val selfAssessed: Boolean,
    val lastAssessedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

**File 2: AddLanguageRequest.kt**
```kotlin
package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType

data class AddLanguageRequest(
    val languageCode: String,
    val type: LanguageProficiencyType,
    val cefrLevel: CEFRLevel? = null,
    val isNative: Boolean = false
)
```

**File 3: UpdateLanguageRequest.kt**
```kotlin
package ch.obermuhlner.aitutor.user.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel

data class UpdateLanguageRequest(
    val cefrLevel: CEFRLevel
)
```

#### Catalog DTOs

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/dto/`

**File 1: LanguageResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.Difficulty

data class LanguageResponse(
    val code: String,
    val name: String,           // Localized
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val description: String,    // Localized
    val courseCount: Int = 0    // Populated by controller
)
```

**File 2: TutorResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import java.util.UUID

data class TutorResponse(
    val id: UUID,
    val name: String,
    val emoji: String,
    val persona: String,              // Localized
    val domain: String,               // Localized
    val personality: TutorPersonality,
    val description: String,          // Localized
    val targetLanguageCode: String,
    val culturalBackground: String?,  // Localized
    val displayOrder: Int
)
```

**File 3: TutorDetailResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import java.time.Instant
import java.util.UUID

data class TutorDetailResponse(
    val id: UUID,
    val name: String,
    val emoji: String,
    val persona: String,              // Localized
    val domain: String,               // Localized
    val personality: TutorPersonality,
    val description: String,          // Localized
    val targetLanguageCode: String,
    val culturalBackground: String?,  // Localized
    val createdAt: Instant,
    val updatedAt: Instant
)
```

**File 4: CourseResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import java.util.UUID

data class CourseResponse(
    val id: UUID,
    val languageCode: String,
    val name: String,                // Localized
    val shortDescription: String,    // Localized
    val category: CourseCategory,
    val targetAudience: String,      // Localized
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,
    val displayOrder: Int
)
```

**File 5: CourseDetailResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.catalog.dto

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import java.time.Instant
import java.util.UUID

data class CourseDetailResponse(
    val id: UUID,
    val languageCode: String,
    val name: String,                // Localized
    val shortDescription: String,    // Localized
    val description: String,         // Localized
    val category: CourseCategory,
    val targetAudience: String,      // Localized
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,
    val suggestedTutors: List<TutorResponse>,
    val defaultPhase: ConversationPhase,
    val topicSequence: List<String>?,
    val learningGoals: List<String>,  // Localized list
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

#### Chat DTOs

**Extend existing package**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/dto/`

**File 1: CreateSessionFromCourseRequest.kt**
```kotlin
package ch.obermuhlner.aitutor.chat.dto

import java.util.UUID

data class CreateSessionFromCourseRequest(
    val userId: UUID,
    val courseTemplateId: UUID,
    val tutorProfileId: UUID,
    val sourceLanguageCode: String,
    val customName: String? = null
)
```

**File 2: SessionWithProgressResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.chat.dto

data class SessionWithProgressResponse(
    val session: SessionResponse,
    val progress: SessionProgressResponse
)
```

**File 3: SessionProgressResponse.kt**
```kotlin
package ch.obermuhlner.aitutor.chat.dto

import java.time.Instant
import java.util.UUID

data class SessionProgressResponse(
    val sessionId: UUID,
    val messageCount: Int,
    val vocabularyCount: Int,
    val daysActive: Int,
    val lastAccessedAt: Instant
)
```

**Update existing SessionResponse.kt** - Add new fields:
```kotlin
data class SessionResponse(
    // ... existing fields ...
    val courseTemplateId: UUID? = null,  // NEW
    val tutorProfileId: UUID? = null,    // NEW
    val customName: String? = null,      // NEW
    val isActive: Boolean = true,        // NEW
    // ... existing timestamp fields ...
)
```

---

### 3.2 Controllers

#### 3.2.1 UserLanguageController.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/user/controller/`

```kotlin
package ch.obermuhlner.aitutor.user.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.user.dto.AddLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UpdateLanguageRequest
import ch.obermuhlner.aitutor.user.dto.UserLanguageProficiencyResponse
import ch.obermuhlner.aitutor.user.service.UserLanguageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/users/{userId}/languages")
class UserLanguageController(
    private val userLanguageService: UserLanguageService,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    fun getUserLanguages(@PathVariable userId: UUID): List<UserLanguageProficiencyResponse> {
        authorizationService.requireAccessToUser(userId)
        return userLanguageService.getUserLanguages(userId).map { it.toResponse() }
    }

    @PostMapping
    fun addLanguage(
        @PathVariable userId: UUID,
        @RequestBody request: AddLanguageRequest
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        val entity = userLanguageService.addLanguage(
            userId = userId,
            languageCode = request.languageCode,
            type = request.type,
            cefrLevel = request.cefrLevel,
            isNative = request.isNative
        )
        return entity.toResponse()
    }

    @PatchMapping("/{languageCode}")
    fun updateLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        @RequestBody request: UpdateLanguageRequest
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        val entity = userLanguageService.updateLanguage(userId, languageCode, request.cefrLevel)
        return entity.toResponse()
    }

    @PatchMapping("/{languageCode}/set-primary")
    fun setPrimaryLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String
    ): ResponseEntity<Void> {
        authorizationService.requireAccessToUser(userId)
        userLanguageService.setPrimaryLanguage(userId, languageCode)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{languageCode}")
    fun removeLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String
    ): ResponseEntity<Void> {
        authorizationService.requireAccessToUser(userId)
        userLanguageService.removeLanguage(userId, languageCode)
        return ResponseEntity.noContent().build()
    }

    private fun ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity.toResponse() =
        UserLanguageProficiencyResponse(
            id = id,
            userId = userId,
            languageCode = languageCode,
            proficiencyType = proficiencyType,
            cefrLevel = cefrLevel,
            isNative = isNative,
            isPrimary = isPrimary,
            selfAssessed = selfAssessed,
            lastAssessedAt = lastAssessedAt,
            createdAt = createdAt ?: java.time.Instant.now(),
            updatedAt = updatedAt ?: java.time.Instant.now()
        )
}
```

#### 3.2.2 CatalogController.kt

**New package**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/controller/`

```kotlin
package ch.obermuhlner.aitutor.catalog.controller

import ch.obermuhlner.aitutor.catalog.dto.*
import ch.obermuhlner.aitutor.catalog.service.CatalogService
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.language.service.LocalizationService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService,
    private val localizationService: LocalizationService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/languages")
    fun getLanguages(
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<LanguageResponse> {
        return catalogService.getAvailableLanguages().map { lang ->
            val name = localizationService.getLocalizedText(
                lang.nameJson, sourceLanguage, lang.nativeName
            )
            val description = localizationService.getLocalizedText(
                lang.descriptionJson, sourceLanguage, lang.descriptionJson
            )
            val courseCount = catalogService.getCoursesForLanguage(lang.code).size

            LanguageResponse(
                code = lang.code,
                name = name,
                flagEmoji = lang.flagEmoji,
                nativeName = lang.nativeName,
                difficulty = lang.difficulty,
                description = description,
                courseCount = courseCount
            )
        }
    }

    @GetMapping("/languages/{code}/courses")
    fun getCoursesForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String,
        @RequestParam(required = false) userLevel: CEFRLevel?
    ): List<CourseResponse> {
        val courses = catalogService.getCoursesForLanguage(code, userLevel)
        return courses.map { course ->
            CourseResponse(
                id = course.id,
                languageCode = course.languageCode,
                name = localizationService.getLocalizedText(
                    course.nameJson, sourceLanguage, course.nameJson
                ),
                shortDescription = localizationService.getLocalizedText(
                    course.shortDescriptionJson, sourceLanguage, course.shortDescriptionJson
                ),
                category = course.category,
                targetAudience = localizationService.getLocalizedText(
                    course.targetAudienceJson, sourceLanguage, course.targetAudienceJson
                ),
                startingLevel = course.startingLevel,
                targetLevel = course.targetLevel,
                estimatedWeeks = course.estimatedWeeks,
                displayOrder = course.displayOrder
            )
        }
    }

    @GetMapping("/languages/{code}/tutors")
    fun getTutorsForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<TutorResponse> {
        val tutors = catalogService.getTutorsForLanguage(code)
        return tutors.map { it.toResponse(sourceLanguage) }
    }

    @GetMapping("/courses/{id}")
    fun getCourseDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): CourseDetailResponse {
        val course = catalogService.getCourseById(id)
            ?: throw IllegalArgumentException("Course not found: $id")

        val suggestedTutors = catalogService.getTutorsForCourse(id)
            .map { it.toResponse(sourceLanguage) }

        val learningGoals = try {
            val goalsJson = localizationService.getLocalizedText(
                course.learningGoalsJson, sourceLanguage, course.learningGoalsJson
            )
            objectMapper.readValue(goalsJson, object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        val tags = course.tagsJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val topicSequence = course.topicSequenceJson?.let {
            try {
                objectMapper.readValue(it, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                null
            }
        }

        return CourseDetailResponse(
            id = course.id,
            languageCode = course.languageCode,
            name = localizationService.getLocalizedText(
                course.nameJson, sourceLanguage, course.nameJson
            ),
            shortDescription = localizationService.getLocalizedText(
                course.shortDescriptionJson, sourceLanguage, course.shortDescriptionJson
            ),
            description = localizationService.getLocalizedText(
                course.descriptionJson, sourceLanguage, course.descriptionJson
            ),
            category = course.category,
            targetAudience = localizationService.getLocalizedText(
                course.targetAudienceJson, sourceLanguage, course.targetAudienceJson
            ),
            startingLevel = course.startingLevel,
            targetLevel = course.targetLevel,
            estimatedWeeks = course.estimatedWeeks,
            suggestedTutors = suggestedTutors,
            defaultPhase = course.defaultPhase,
            topicSequence = topicSequence,
            learningGoals = learningGoals,
            tags = tags,
            createdAt = course.createdAt ?: java.time.Instant.now(),
            updatedAt = course.updatedAt ?: java.time.Instant.now()
        )
    }

    @GetMapping("/tutors/{id}")
    fun getTutorDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): TutorDetailResponse {
        val tutor = catalogService.getTutorById(id)
            ?: throw IllegalArgumentException("Tutor not found: $id")

        return TutorDetailResponse(
            id = tutor.id,
            name = tutor.name,
            emoji = tutor.emoji,
            persona = localizationService.getLocalizedText(
                tutor.personaJson, sourceLanguage, tutor.personaEnglish
            ),
            domain = localizationService.getLocalizedText(
                tutor.domainJson, sourceLanguage, tutor.domainEnglish
            ),
            personality = tutor.personality,
            description = localizationService.getLocalizedText(
                tutor.descriptionJson, sourceLanguage, tutor.descriptionEnglish
            ),
            targetLanguageCode = tutor.targetLanguageCode,
            culturalBackground = tutor.culturalBackgroundJson?.let {
                localizationService.getLocalizedText(it, sourceLanguage, it)
            },
            createdAt = tutor.createdAt ?: java.time.Instant.now(),
            updatedAt = tutor.updatedAt ?: java.time.Instant.now()
        )
    }

    private fun ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity.toResponse(sourceLanguage: String) =
        TutorResponse(
            id = id,
            name = name,
            emoji = emoji,
            persona = localizationService.getLocalizedText(
                personaJson, sourceLanguage, personaEnglish
            ),
            domain = localizationService.getLocalizedText(
                domainJson, sourceLanguage, domainEnglish
            ),
            personality = personality,
            description = localizationService.getLocalizedText(
                descriptionJson, sourceLanguage, descriptionEnglish
            ),
            targetLanguageCode = targetLanguageCode,
            culturalBackground = culturalBackgroundJson?.let {
                localizationService.getLocalizedText(it, sourceLanguage, it)
            },
            displayOrder = displayOrder
        )
}
```

#### 3.2.3 Extend ChatController.kt

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt`

**Add these endpoints to existing controller:**

```kotlin
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val authorizationService: AuthorizationService
) {
    // ... existing endpoints ...

    @PostMapping("/sessions/from-course")
    fun createSessionFromCourse(
        @RequestBody request: CreateSessionFromCourseRequest
    ): SessionResponse {
        authorizationService.requireAccessToUser(request.userId)
        return chatService.createSessionFromCourse(
            userId = request.userId,
            courseTemplateId = request.courseTemplateId,
            tutorProfileId = request.tutorProfileId,
            sourceLanguageCode = request.sourceLanguageCode,
            customName = request.customName
        )
    }

    @GetMapping("/sessions/active")
    fun getActiveLearningSessions(
        @RequestParam userId: UUID
    ): List<SessionWithProgressResponse> {
        authorizationService.requireAccessToUser(userId)
        return chatService.getActiveLearningSessions(userId)
    }

    @GetMapping("/sessions/{sessionId}/progress")
    fun getSessionProgress(
        @PathVariable sessionId: UUID
    ): SessionProgressResponse {
        return chatService.getSessionProgress(sessionId)
    }
}
```

---

### 3.3 HTTP Test Examples

**File**: `src/test/http/http-client-requests.http`

**Add these sections at the end:**

```http
###########################
### Catalog Examples
###########################

### Get Available Languages
GET {{host}}/api/v1/catalog/languages?sourceLanguage=en
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Courses for Spanish
GET {{host}}/api/v1/catalog/languages/es/courses?sourceLanguage=en
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Courses for Spanish (Filtered by User Level)
GET {{host}}/api/v1/catalog/languages/es/courses?sourceLanguage=en&userLevel=A1
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Tutors for Spanish
GET {{host}}/api/v1/catalog/languages/es/tutors?sourceLanguage=en
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Course Details
# @name getCourseDetails
GET {{host}}/api/v1/catalog/courses/COURSE_ID?sourceLanguage=en
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Tutor Details
GET {{host}}/api/v1/catalog/tutors/TUTOR_ID?sourceLanguage=en
Authorization: Bearer {{accessToken}}
Accept: application/json

###########################
### User Language Management
###########################

### Get User Languages
GET {{host}}/api/v1/users/{{userId}}/languages
Authorization: Bearer {{accessToken}}
Accept: application/json

### Add Language (Native)
POST {{host}}/api/v1/users/{{userId}}/languages
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "languageCode": "en",
  "type": "Native",
  "isNative": true
}

### Add Language (Learning)
POST {{host}}/api/v1/users/{{userId}}/languages
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "languageCode": "es",
  "type": "Learning",
  "cefrLevel": "A1"
}

### Update Language Level
PATCH {{host}}/api/v1/users/{{userId}}/languages/es
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "cefrLevel": "A2"
}

### Set Primary Language
PATCH {{host}}/api/v1/users/{{userId}}/languages/en/set-primary
Authorization: Bearer {{accessToken}}

### Remove Language
DELETE {{host}}/api/v1/users/{{userId}}/languages/es
Authorization: Bearer {{accessToken}}

###########################
### Course-Based Sessions
###########################

### Create Session from Course
POST {{host}}/api/v1/chat/sessions/from-course
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "userId": "{{userId}}",
  "courseTemplateId": "COURSE_TEMPLATE_ID",
  "tutorProfileId": "TUTOR_PROFILE_ID",
  "sourceLanguageCode": "en",
  "customName": "My Spanish Learning Journey"
}

### Get Active Learning Sessions with Progress
GET {{host}}/api/v1/chat/sessions/active?userId={{userId}}
Authorization: Bearer {{accessToken}}
Accept: application/json

### Get Session Progress
GET {{host}}/api/v1/chat/sessions/{{chatSessionId}}/progress
Authorization: Bearer {{accessToken}}
Accept: application/json
```

---

### 3.4 Testing (Phase 3)

**New test files:**

1. `UserLanguageControllerTest.kt` - @WebMvcTest
2. `CatalogControllerTest.kt` - @WebMvcTest
3. `ChatControllerTest.kt` - Extend existing with new endpoint tests

---

## Phase 4: Seed Data & Content (Week 4)

### 4.1 Seed Data Service

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/SeedDataService.kt`

```kotlin
package ch.obermuhlner.aitutor.catalog.service

import ch.obermuhlner.aitutor.catalog.domain.CourseTemplateEntity
import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import ch.obermuhlner.aitutor.catalog.repository.CourseTemplateRepository
import ch.obermuhlner.aitutor.catalog.repository.TutorProfileRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.core.model.catalog.CourseCategory
import ch.obermuhlner.aitutor.core.model.catalog.TutorPersonality
import ch.obermuhlner.aitutor.tutor.domain.ConversationPhase
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.*

@Component
@Profile("dev", "default")  // Only run in dev mode
class SeedDataService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val courseTemplateRepository: CourseTemplateRepository,
    private val objectMapper: ObjectMapper
) {

    @PostConstruct
    fun seedData() {
        // Only seed if database is empty
        if (tutorProfileRepository.count() > 0) {
            println("Seed data already exists, skipping...")
            return
        }

        println("Seeding catalog data...")

        val tutors = seedTutors()
        val courses = seedCourses(tutors)

        println("Seeded ${tutors.size} tutors and ${courses.size} courses")
    }

    private fun seedTutors(): Map<String, List<TutorProfileEntity>> {
        val spanishTutors = listOf(
            createTutor(
                name = "María",
                emoji = "👩‍🏫",
                personaEnglish = "patient coach",
                domainEnglish = "general conversation, grammar, typography",
                descriptionEnglish = "Patient coach from Madrid who loves helping beginners feel confident",
                personality = TutorPersonality.Encouraging,
                targetLanguageCode = "es",
                displayOrder = 0
            ),
            createTutor(
                name = "Professor Rodríguez",
                emoji = "🎓",
                personaEnglish = "strict academic",
                domainEnglish = "Spanish grammar, literature, formal writing",
                descriptionEnglish = "Academic expert in Spanish grammar and literature with high standards",
                personality = TutorPersonality.Academic,
                targetLanguageCode = "es",
                displayOrder = 1
            ),
            createTutor(
                name = "Carlos",
                emoji = "😊",
                personaEnglish = "casual friend",
                domainEnglish = "everyday conversation, slang, culture",
                descriptionEnglish = "Friendly guy from Barcelona who makes learning Spanish fun and relaxed",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "es",
                displayOrder = 2
            )
        )

        val frenchTutors = listOf(
            createTutor(
                name = "François",
                emoji = "👨‍🏫",
                personaEnglish = "academic professor",
                domainEnglish = "French linguistics, literature, formal grammar",
                descriptionEnglish = "Parisian professor with expertise in French linguistics and literature",
                personality = TutorPersonality.Academic,
                targetLanguageCode = "fr",
                displayOrder = 0
            ),
            createTutor(
                name = "Céline",
                emoji = "😊",
                personaEnglish = "casual tutor",
                domainEnglish = "everyday French, conversation, culture",
                descriptionEnglish = "Young tutor from Lyon who loves casual conversation and cultural exchange",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "fr",
                displayOrder = 1
            )
        )

        val germanTutors = listOf(
            createTutor(
                name = "Herr Schmidt",
                emoji = "🎓",
                personaEnglish = "strict teacher",
                domainEnglish = "German grammar, formal writing, business German",
                descriptionEnglish = "Traditional grammar expert from Berlin who focuses on proper form",
                personality = TutorPersonality.Strict,
                targetLanguageCode = "de",
                displayOrder = 0
            ),
            createTutor(
                name = "Anna",
                emoji = "😊",
                personaEnglish = "friendly tutor",
                domainEnglish = "everyday German, conversation, culture",
                descriptionEnglish = "Friendly tutor from Munich who keeps German learning relaxed and fun",
                personality = TutorPersonality.Casual,
                targetLanguageCode = "de",
                displayOrder = 1
            )
        )

        val japaneseTutors = listOf(
            createTutor(
                name = "Yuki",
                emoji = "👩‍🏫",
                personaEnglish = "patient teacher",
                domainEnglish = "Japanese basics, hiragana, katakana, simple conversation",
                descriptionEnglish = "Patient teacher from Tokyo who specializes in helping beginners",
                personality = TutorPersonality.Encouraging,
                targetLanguageCode = "ja",
                displayOrder = 0
            ),
            createTutor(
                name = "Tanaka-sensei",
                emoji = "🎓",
                personaEnglish = "traditional teacher",
                domainEnglish = "Japanese grammar, kanji, formal language, etiquette",
                descriptionEnglish = "Traditional teacher focused on proper form and cultural etiquette",
                personality = TutorPersonality.Strict,
                targetLanguageCode = "ja",
                displayOrder = 1
            )
        )

        val allTutors = spanishTutors + frenchTutors + germanTutors + japaneseTutors
        tutorProfileRepository.saveAll(allTutors)

        return mapOf(
            "es" to spanishTutors,
            "fr" to frenchTutors,
            "de" to germanTutors,
            "ja" to japaneseTutors
        )
    }

    private fun createTutor(
        name: String,
        emoji: String,
        personaEnglish: String,
        domainEnglish: String,
        descriptionEnglish: String,
        personality: TutorPersonality,
        targetLanguageCode: String,
        displayOrder: Int
    ): TutorProfileEntity {
        return TutorProfileEntity(
            name = name,
            emoji = emoji,
            personaEnglish = personaEnglish,
            domainEnglish = domainEnglish,
            descriptionEnglish = descriptionEnglish,
            personaJson = """{"en": "$personaEnglish"}""",
            domainJson = """{"en": "$domainEnglish"}""",
            descriptionJson = """{"en": "$descriptionEnglish"}""",
            culturalBackgroundJson = null,
            personality = personality,
            targetLanguageCode = targetLanguageCode,
            isActive = true,
            displayOrder = displayOrder
        )
    }

    private fun seedCourses(tutorsByLanguage: Map<String, List<TutorProfileEntity>>): List<CourseTemplateEntity> {
        val courses = mutableListOf<CourseTemplateEntity>()

        // Spanish courses
        tutorsByLanguage["es"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "es",
                    nameEnglish = "Conversational Spanish",
                    shortDescriptionEnglish = "Master everyday conversations at your own pace",
                    descriptionEnglish = "Learn to communicate naturally in Spanish through practical conversations covering greetings, daily routines, opinions, and more",
                    category = CourseCategory.Conversational,
                    targetAudienceEnglish = "Anyone wanting to speak Spanish naturally",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.C1,
                    estimatedWeeks = null,  // Self-paced
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Greet people and introduce yourself",
                        "Talk about daily routines and activities",
                        "Describe people, places, and things",
                        "Order food and shop for items",
                        "Express opinions and feelings"
                    ),
                    displayOrder = 0
                )
            )
            courses.add(
                createCourse(
                    languageCode = "es",
                    nameEnglish = "Spanish for Travelers",
                    shortDescriptionEnglish = "Essential phrases for tourists and travelers",
                    descriptionEnglish = "Quick-start Spanish course focusing on practical phrases you'll need for travel",
                    category = CourseCategory.Travel,
                    targetAudienceEnglish = "Travelers planning a trip to Spanish-speaking countries",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.A2,
                    estimatedWeeks = 6,
                    suggestedTutorIds = tutors.take(2).map { it.id },
                    learningGoalsEnglish = listOf(
                        "Navigate airports and transportation",
                        "Check into hotels and accommodations",
                        "Order food at restaurants",
                        "Ask for directions and help",
                        "Handle emergencies"
                    ),
                    displayOrder = 1
                )
            )
        }

        // French courses
        tutorsByLanguage["fr"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "fr",
                    nameEnglish = "French Conversation",
                    shortDescriptionEnglish = "Develop fluency in everyday French",
                    descriptionEnglish = "Practice natural French conversation skills from basic to intermediate level",
                    category = CourseCategory.Conversational,
                    targetAudienceEnglish = "Learners wanting to speak French confidently",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.B2,
                    estimatedWeeks = null,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Engage in everyday conversations",
                        "Understand French pronunciation",
                        "Use common idiomatic expressions",
                        "Discuss various topics naturally"
                    ),
                    displayOrder = 0
                )
            )
        }

        // German courses
        tutorsByLanguage["de"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "de",
                    nameEnglish = "German Fundamentals",
                    shortDescriptionEnglish = "Build a strong foundation in German",
                    descriptionEnglish = "Comprehensive beginner to intermediate German course covering grammar and conversation",
                    category = CourseCategory.General,
                    targetAudienceEnglish = "New German learners wanting a structured approach",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.B1,
                    estimatedWeeks = 12,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Master German cases and grammar",
                        "Build essential vocabulary",
                        "Form correct sentences",
                        "Understand German culture"
                    ),
                    displayOrder = 0
                )
            )
        }

        // Japanese courses
        tutorsByLanguage["ja"]?.let { tutors ->
            courses.add(
                createCourse(
                    languageCode = "ja",
                    nameEnglish = "Japanese for Beginners",
                    shortDescriptionEnglish = "Start your Japanese journey from zero",
                    descriptionEnglish = "Learn hiragana, katakana, and basic Japanese conversation step by step",
                    category = CourseCategory.General,
                    targetAudienceEnglish = "Complete beginners to Japanese",
                    startingLevel = CEFRLevel.A1,
                    targetLevel = CEFRLevel.A2,
                    estimatedWeeks = 16,
                    suggestedTutorIds = tutors.map { it.id },
                    learningGoalsEnglish = listOf(
                        "Master hiragana and katakana",
                        "Learn basic kanji",
                        "Understand Japanese sentence structure",
                        "Practice polite conversation"
                    ),
                    displayOrder = 0
                )
            )
        }

        courseTemplateRepository.saveAll(courses)
        return courses
    }

    private fun createCourse(
        languageCode: String,
        nameEnglish: String,
        shortDescriptionEnglish: String,
        descriptionEnglish: String,
        category: CourseCategory,
        targetAudienceEnglish: String,
        startingLevel: CEFRLevel,
        targetLevel: CEFRLevel,
        estimatedWeeks: Int?,
        suggestedTutorIds: List<UUID>,
        learningGoalsEnglish: List<String>,
        displayOrder: Int
    ): CourseTemplateEntity {
        return CourseTemplateEntity(
            languageCode = languageCode,
            nameJson = """{"en": "$nameEnglish"}""",
            shortDescriptionJson = """{"en": "$shortDescriptionEnglish"}""",
            descriptionJson = """{"en": "$descriptionEnglish"}""",
            category = category,
            targetAudienceJson = """{"en": "$targetAudienceEnglish"}""",
            startingLevel = startingLevel,
            targetLevel = targetLevel,
            estimatedWeeks = estimatedWeeks,
            suggestedTutorIdsJson = objectMapper.writeValueAsString(suggestedTutorIds),
            defaultPhase = ConversationPhase.Auto,
            topicSequenceJson = null,
            learningGoalsJson = """{"en": ${objectMapper.writeValueAsString(learningGoalsEnglish)}}""",
            isActive = true,
            displayOrder = displayOrder,
            tagsJson = null
        )
    }
}
```

---

### 4.2 Testing (Phase 4)

**New test files:**

1. `SeedDataServiceTest.kt` - Integration test verifying seed data
2. `LanguageConfigTest.kt` - Unit test verifying language configuration

---

## Phase 5: CLI Integration & Documentation (Week 5)

### 5.1 CLI Client Updates

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/cli/AiTutorCli.kt`

**Add these new commands to existing CLI:**

```kotlin
// Add to command parsing logic
when {
    input == "/languages" -> cmdLanguages()
    input.startsWith("/courses") -> cmdCourses(input)
    input.startsWith("/tutors") -> cmdTutors(input)
    input == "/start-course" -> cmdStartCourse()
    input == "/sessions" -> cmdSessions()
    input == "/progress" -> cmdProgress()
    // ... existing commands ...
}

// New command implementations
private fun cmdLanguages() {
    println("📚 Available Languages:")
    val languages = apiClient.getAvailableLanguages()
    languages.forEachIndexed { index, lang ->
        println("  ${index + 1}. ${lang.name} ${lang.flagEmoji} (${lang.courseCount} courses) - ${lang.difficulty}")
        println("     ${lang.description}")
    }
}

private fun cmdCourses(input: String) {
    val langCode = input.substringAfter("/courses").trim().takeIf { it.isNotEmpty() }

    if (langCode == null) {
        println("Available commands:")
        println("  /courses <language-code>  - List courses for a language")
        println("Example: /courses es")
        return
    }

    val courses = apiClient.getCoursesForLanguage(langCode)
    if (courses.isEmpty()) {
        println("No courses found for language: $langCode")
        return
    }

    println("\nCourses for $langCode:")
    courses.forEachIndexed { index, course ->
        println("  ${index + 1}. ${course.name} (${course.startingLevel}→${course.targetLevel})")
        println("     ${course.shortDescription}")
        println("     Category: ${course.category}, Duration: ${course.estimatedWeeks?.let { "$it weeks" } ?: "Self-paced"}")
    }
}

private fun cmdTutors(input: String) {
    val langCode = input.substringAfter("/tutors").trim().takeIf { it.isNotEmpty() }

    if (langCode == null) {
        println("Available commands:")
        println("  /tutors <language-code>  - List tutors for a language")
        println("Example: /tutors es")
        return
    }

    val tutors = apiClient.getTutorsForLanguage(langCode)
    if (tutors.isEmpty()) {
        println("No tutors found for language: $langCode")
        return
    }

    println("\nTutors for $langCode:")
    tutors.forEachIndexed { index, tutor ->
        println("  ${index + 1}. ${tutor.emoji} ${tutor.name} (${tutor.personality})")
        println("     ${tutor.description}")
    }
}

private fun cmdStartCourse() {
    // Interactive wizard implementation
    println("Interactive course selection wizard...")
    // Implementation would guide user through:
    // 1. Select language
    // 2. Select course
    // 3. Select tutor
    // 4. Create session
}

private fun cmdSessions() {
    val sessions = apiClient.getActiveLearningSessions()
    if (sessions.isEmpty()) {
        println("No active learning sessions")
        return
    }

    println("📚 Your Learning Sessions:")
    sessions.forEachIndexed { index, session ->
        val progress = session.progress
        println("  ${index + 1}. ${session.session.customName ?: "${session.session.tutorName} - ${session.session.targetLanguageCode}"}")
        println("     ${progress.messageCount} messages | ${progress.vocabularyCount} words | ${progress.daysActive} days active")
        println("     Last: ${formatTimestamp(progress.lastAccessedAt)}")
    }
}

private fun cmdProgress() {
    if (currentSessionId == null) {
        println("No active session")
        return
    }

    val progress = apiClient.getSessionProgress(currentSessionId!!)
    println("\nSession Progress:")
    println("  Messages: ${progress.messageCount}")
    println("  Vocabulary: ${progress.vocabularyCount} words")
    println("  Days active: ${progress.daysActive}")
    println("  Last accessed: ${formatTimestamp(progress.lastAccessedAt)}")
}
```

---

### 5.2 HTTP Client Updates

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/cli/HttpApiClient.kt`

**Add these new methods:**

```kotlin
class HttpApiClient(/* ... */) {
    // ... existing methods ...

    // User Language Management
    fun getUserLanguages(userId: UUID): List<UserLanguageProficiencyResponse> {
        // Implementation
    }

    fun addLanguage(userId: UUID, request: AddLanguageRequest): UserLanguageProficiencyResponse {
        // Implementation
    }

    // Catalog Browsing
    fun getAvailableLanguages(sourceLanguage: String = "en"): List<LanguageResponse> {
        // Implementation
    }

    fun getCoursesForLanguage(languageCode: String, sourceLanguage: String = "en", userLevel: CEFRLevel? = null): List<CourseResponse> {
        // Implementation
    }

    fun getTutorsForLanguage(languageCode: String, sourceLanguage: String = "en"): List<TutorResponse> {
        // Implementation
    }

    fun getCourseDetails(courseId: UUID, sourceLanguage: String = "en"): CourseDetailResponse {
        // Implementation
    }

    fun getTutorDetails(tutorId: UUID, sourceLanguage: String = "en"): TutorDetailResponse {
        // Implementation
    }

    // Course-Based Sessions
    fun createSessionFromCourse(request: CreateSessionFromCourseRequest): SessionResponse {
        // Implementation
    }

    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse> {
        // Implementation
    }

    fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
        // Implementation
    }
}
```

---

### 5.3 Configuration Updates

**File**: `src/main/kotlin/ch/obermuhlner/aitutor/cli/CliConfig.kt`

**Add new fields:**

```kotlin
@Serializable
data class CliConfig(
    // ... existing fields ...

    // Course-based session preferences
    val lastSessionId: String? = null,
    val defaultSourceLanguage: String = "en",

    // Deprecated (migrate to course-based sessions)
    @Deprecated("Use course-based sessions instead")
    val defaultTutor: String = "Maria"
)
```

---

### 5.4 Documentation Updates

#### 5.4.1 README.md

**Add new sections:**

```markdown
## Catalog-Based Learning

### Browse Languages and Courses

```bash
curl http://localhost:8080/api/v1/catalog/languages?sourceLanguage=en \
  -H "Authorization: Bearer {accessToken}"
```

### Create Learning Session from Course

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions/from-course \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "{userId}",
    "courseTemplateId": "{courseId}",
    "tutorProfileId": "{tutorId}",
    "sourceLanguageCode": "en",
    "customName": "My Spanish Learning"
  }'
```

### Track Progress

```bash
curl http://localhost:8080/api/v1/chat/sessions/{sessionId}/progress \
  -H "Authorization: Bearer {accessToken}"
```
```

Add endpoint tables for:
- User Language Management (5 endpoints)
- Catalog Browsing (5 endpoints)
- Course-Based Sessions (3 endpoints)

#### 5.4.2 CLAUDE.md

**Update architecture section:**

```markdown
### New Packages (Catalog System)

- **catalog/**
  - **domain/** - TutorProfileEntity, CourseTemplateEntity
  - **repository/** - TutorProfileRepository, CourseTemplateRepository
  - **service/** - CatalogService, SeedDataService
  - **controller/** - CatalogController
  - **dto/** - Language/Tutor/Course response DTOs

- **language/**
  - **service/** - LocalizationService, TranslationService
  - **config/** - LanguageConfig

- **user/domain/** - UserLanguageProficiencyEntity
- **user/repository/** - UserLanguageProficiencyRepository
- **user/service/** - UserLanguageService
- **user/controller/** - UserLanguageController
- **user/dto/** - User language DTOs
```

---

## Testing Summary

### Unit Tests
- **Phase 1**: Repository tests (3 files)
- **Phase 2**: Service tests (5 files)
- **Phase 3**: Controller tests (2 files)
- **Phase 4**: Seed data tests (2 files)

### Integration Tests
- Full catalog browsing flow
- Course-based session creation
- Progress calculation accuracy
- Localization with AI translation

### E2E Tests
- CLI onboarding flow
- Course selection wizard
- Session resumption with progress

---

## Success Criteria

- ✅ All unit tests pass (>80% coverage)
- ✅ All integration tests pass
- ✅ Seed data loads successfully
- ✅ API endpoints documented and tested
- ✅ CLI commands work correctly
- ✅ Database migration runs without errors
- ✅ No breaking changes to existing functionality
- ✅ Documentation updated

---

## Rollout Plan

1. **Week 1**: Deploy Phase 1 (database migration)
2. **Week 2**: Deploy Phase 2 (services)
3. **Week 3**: Deploy Phase 3 (REST API)
4. **Week 4**: Deploy Phase 4 (seed data)
5. **Week 5**: Deploy Phase 5 (CLI + docs)

---

## Maintenance

### Adding New Languages
1. Update `LanguageConfig.kt` with new language metadata
2. Create seed data for tutors (3-4 tutors)
3. Create seed data for courses (2-3 courses)

### Adding New Tutors
1. Add tutor entry in `SeedDataService.kt`
2. Ensure dual storage (English + JSON)
3. Test localization

### Adding New Courses
1. Add course entry in `SeedDataService.kt`
2. Link suggested tutors
3. Define learning goals and topic sequence