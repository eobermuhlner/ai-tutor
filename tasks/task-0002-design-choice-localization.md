# Design Choice: Tutor Localization Strategy

**Date**: 2025-01-17
**Status**: Decision Required
**Related**: task-0002-requirements.md

## Context

We need to decide how to handle tutor personality descriptions for users who speak different native languages (source languages). The tutor's persona and domain are used in:
1. **System Prompt**: Injected into AI prompt to shape tutor behavior
2. **User Interface**: Displayed in catalog for users to choose tutors

## Current Implementation

In `TutorService.kt`, tutor personality is used in the system prompt:
```kotlin
val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
    "tutorPersona" to tutor.persona,  // e.g., "patient coach"
    "tutorDomain" to tutor.domain,    // e.g., "general conversation, grammar"
    // ...
))
```

## The Problem

If we store multilingual descriptions:
- **UI**: German user sees "geduldiger Coach" (good!)
- **System Prompt**: Should we pass "geduldiger Coach" or "patient coach" to the AI?

## Option 1: Dual Storage (English for AI + Localized for UI)

### Design
```kotlin
data class TutorProfile(
    val id: UUID,
    val name: String,

    // English-only (for AI system prompt)
    val personaEnglish: String,          // "patient coach"
    val domainEnglish: String,           // "general conversation, grammar"
    val descriptionEnglish: String,      // "Patient coach from Madrid..."

    // Multilingual (for UI catalog)
    val personaJson: String,             // {"en": "patient coach", "de": "geduldiger Coach", ...}
    val domainJson: String,
    val descriptionJson: String,
    val culturalBackgroundJson: String?,

    val targetLanguageCode: String,
    val personality: TutorPersonality,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
```

### Pros
- ✅ **Consistent AI Behavior**: All users get identical tutor personalities (AI always sees English)
- ✅ **Predictable Prompts**: No risk of translation affecting AI behavior
- ✅ **Easier Testing**: Single English prompt to validate
- ✅ **Better AI Understanding**: LLMs trained primarily on English
- ✅ **Simpler Prompt Engineering**: Write prompts once in English
- ✅ **Version Control**: Easy to track changes to tutor personalities
- ✅ **Less Token Usage**: English descriptions often more concise

### Cons
- ❌ **Data Duplication**: Store both English and translations
- ❌ **Maintenance Burden**: Must update both English base and translations
- ❌ **Schema Complexity**: More fields in database
- ❌ **Potential Inconsistency**: English and translations might drift

### Implementation Impact
**Database:**
```sql
CREATE TABLE tutor_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(128),

    -- AI system prompt (English only)
    persona_english VARCHAR(256),
    domain_english VARCHAR(256),
    description_english TEXT,

    -- UI catalog (multilingual JSON)
    persona_json TEXT,
    domain_json TEXT,
    description_json TEXT,
    cultural_background_json TEXT,

    target_language_code VARCHAR(32),
    personality VARCHAR(32),
    -- ...
);
```

**Service Layer:**
```kotlin
class TutorService {
    fun respond(tutor: Tutor, ...) {
        // ALWAYS use English for system prompt
        val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
            "tutorPersona" to tutor.personaEnglish,  // Fixed English
            "tutorDomain" to tutor.domainEnglish,
            // ...
        ))
    }
}

class CatalogService {
    fun getTutorsForLanguage(targetLang: String, sourceLang: String): List<TutorProfile> {
        val tutors = repository.findByTargetLanguageCode(targetLang)

        // Localize for UI based on user's source language
        return tutors.map { tutor ->
            localizeProfile(tutor, sourceLang)  // Uses personaJson, domainJson, etc.
        }
    }
}
```

---

## Option 2: Single Storage with On-the-Fly Translation

### Design
```kotlin
data class TutorProfile(
    val id: UUID,
    val name: String,

    // Multilingual only (translate for AI if needed)
    val personaJson: String,       // {"en": "patient coach", "de": "geduldiger Coach"}
    val domainJson: String,
    val descriptionJson: String,
    val culturalBackgroundJson: String?,

    val targetLanguageCode: String,
    val personality: TutorPersonality,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
```

### Pros
- ✅ **Single Source of Truth**: One field per text attribute
- ✅ **Simpler Schema**: Fewer fields in database
- ✅ **No Duplication**: Translations only, no redundant English
- ✅ **Cleaner Data Model**: Less confusing for developers
- ✅ **Flexible**: Could translate AI prompt to any language (future feature?)

### Cons
- ❌ **Inconsistent AI Behavior**: Different translations might affect tutor personality
  - "patient coach" vs. "geduldiger Coach" might subtly change AI responses
  - Nuances lost in translation could alter tutor persona
- ❌ **Translation Quality Risk**: Poor translations affect AI behavior
- ❌ **Harder Testing**: Must test AI behavior with multiple language prompts
- ❌ **AI Performance**: LLMs perform better with English prompts
- ❌ **Prompt Engineering Complexity**: Must validate prompts in multiple languages
- ❌ **Token Overhead**: Non-English text may use more tokens

### Implementation Impact
**Database:**
```sql
CREATE TABLE tutor_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(128),

    -- Multilingual only
    persona_json TEXT,
    domain_json TEXT,
    description_json TEXT,
    cultural_background_json TEXT,

    target_language_code VARCHAR(32),
    personality VARCHAR(32),
    -- ...
);
```

**Service Layer:**
```kotlin
class TutorService {
    fun respond(tutor: Tutor, sourceLanguage: String, ...) {
        // Option A: Always use English for system prompt
        val persona = localizationService.getLocalizedText(tutor.personaJson, "en")

        // Option B: Use user's source language (risky!)
        // val persona = localizationService.getLocalizedText(tutor.personaJson, sourceLanguage)

        val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
            "tutorPersona" to persona,
            // ...
        ))
    }
}
```

**Note**: Even with this approach, we'd likely force English for the system prompt, making it equivalent to Option 1 but with awkward extraction.

---

## Option 3: Hybrid (Store in Primary Language, Translate UI Only)

### Design
```kotlin
data class TutorProfile(
    val id: UUID,
    val name: String,

    // Primary language (English, used for AI)
    val persona: String,               // "patient coach"
    val domain: String,                // "general conversation, grammar"
    val description: String,           // "Patient coach from Madrid..."
    val culturalBackground: String?,

    val targetLanguageCode: String,
    val personality: TutorPersonality,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

// Translation service dynamically translates on request
class CatalogService {
    fun getTutorsForLanguage(targetLang: String, sourceLang: String): List<TutorProfileResponse> {
        val tutors = repository.findByTargetLanguageCode(targetLang)

        return tutors.map { tutor ->
            TutorProfileResponse(
                id = tutor.id,
                name = tutor.name,
                persona = translationService.translate(tutor.persona, from = "en", to = sourceLang),
                description = translationService.translate(tutor.description, from = "en", to = sourceLang),
                // ...
            )
        }
    }
}
```

### Pros
- ✅ **Simple Schema**: Store once in English
- ✅ **Consistent AI**: System prompt always uses original English
- ✅ **Dynamic**: Add new source languages without DB changes
- ✅ **AI-Powered**: Use GPT-4 for high-quality translations
- ✅ **Fresh Translations**: Always up-to-date if English changes

### Cons
- ❌ **API Latency**: Must call translation API on every request
- ❌ **Cost**: Translation API costs per request
- ❌ **Caching Complexity**: Need aggressive caching to avoid costs
- ❌ **Translation Quality Variance**: Different results over time
- ❌ **Offline Support**: Can't work without translation API
- ❌ **No Manual Override**: Can't fix bad translations manually

---

## Recommendation: Option 1 (Dual Storage)

### Why Option 1 is Best

**Primary Reason**: **AI Consistency and Quality**
- LLMs perform best with English prompts (trained primarily on English)
- Ensures all users experience the same tutor personality
- Eliminates translation quality risk in AI behavior

**Secondary Reasons**:
1. **Predictability**: Developers can test and validate tutor behavior once
2. **Performance**: No translation overhead at runtime
3. **Control**: Full control over both AI behavior and UI text
4. **Future-Proof**: Could add more languages without affecting AI

### Implementation Recommendations

**Phase 1 (MVP):**
- Store English fields: `personaEnglish`, `domainEnglish`, `descriptionEnglish`
- Store JSON fields with English only: `{"en": "patient coach"}`
- UI falls back to English if translation missing

**Phase 2:**
- Add translations to JSON fields using AI
- Manual review of critical translations
- A/B test to ensure translation quality

**Phase 3:**
- Community contributions for translations
- Professional translations for marketing

### Migration Path from Current Design

If we've already started with multilingual JSON only:
1. Extract English from JSON → populate `*English` fields
2. Keep JSON for UI localization
3. Update `TutorService` to use `*English` fields

### Example Implementation

```kotlin
data class TutorProfile(
    val id: UUID,
    val name: String,              // "María"
    val emoji: String,             // "👩‍🏫"

    // For AI system prompt (English only)
    val personaEnglish: String,    // "patient coach"
    val domainEnglish: String,     // "general conversation, grammar, typography"
    val descriptionEnglish: String, // "Patient coach from Madrid who loves helping beginners"

    // For UI catalog (multilingual)
    val personaJson: String,       // {"en": "patient coach", "de": "geduldiger Coach", "es": "entrenadora paciente"}
    val domainJson: String,
    val descriptionJson: String,
    val culturalBackgroundJson: String?,

    val targetLanguageCode: String,
    val personality: TutorPersonality,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

// Usage in TutorService
val systemPrompt = PromptTemplate(systemPromptTemplate).render(mapOf(
    "tutorPersona" to tutorProfile.personaEnglish,  // Always English
    "tutorDomain" to tutorProfile.domainEnglish,
    // ...
))

// Usage in CatalogService
fun getTutorsForLanguage(targetLang: String, sourceLang: String): List<TutorProfileResponse> {
    val tutors = repository.findByTargetLanguageCode(targetLang)

    return tutors.map { tutor ->
        TutorProfileResponse(
            id = tutor.id,
            name = tutor.name,
            emoji = tutor.emoji,
            persona = localizationService.getLocalizedText(tutor.personaJson, sourceLang),
            domain = localizationService.getLocalizedText(tutor.domainJson, sourceLang),
            description = localizationService.getLocalizedText(tutor.descriptionJson, sourceLang),
            // ...
        )
    }
}
```

## Decision

**✅ APPROVED: Option 1 with AI Translation Fallback**

**Date**: 2025-01-17

We will use **dual storage** (English for AI prompts + JSON for UI) with **automatic AI translation** when translations are missing.

### Implementation Strategy

```kotlin
data class TutorProfile(
    val id: UUID,
    val name: String,

    // For AI system prompt (English only, always required)
    val personaEnglish: String,
    val domainEnglish: String,
    val descriptionEnglish: String,

    // For UI catalog (multilingual JSON, with fallback)
    val personaJson: String,       // {"en": "patient coach", ...}
    val domainJson: String,
    val descriptionJson: String,
    val culturalBackgroundJson: String?,

    // ...
)
```

### Translation Fallback Logic

```kotlin
class LocalizationServiceImpl(
    private val translationService: TranslationService,  // AI-powered translation
    private val objectMapper: ObjectMapper
) : LocalizationService {

    override fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,  // NEW: English text for AI translation
        fallbackLanguage: String = "en"
    ): String {
        val translations = parseMultilingualJson(jsonText)

        // 1. Try requested language
        translations[languageCode]?.let { return it }

        // 2. Try fallback language (usually English)
        translations[fallbackLanguage]?.let { return it }

        // 3. AI TRANSLATION: Generate on-the-fly and cache
        if (englishFallback.isNotBlank()) {
            return translateAndCache(englishFallback, languageCode, jsonText)
        }

        // 4. Last resort: return untranslated
        return "Translation missing"
    }

    private fun translateAndCache(
        englishText: String,
        targetLanguage: String,
        jsonText: String
    ): String {
        // Translate using AI
        val translated = translationService.translate(
            text = englishText,
            from = "en",
            to = targetLanguage
        )

        // TODO: Optionally persist translation back to database for caching
        // This would require an async update mechanism

        return translated
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

### Translation Service (AI-Powered)

```kotlin
interface TranslationService {
    fun translate(text: String, from: String, to: String): String
    fun batchTranslate(texts: List<String>, from: String, to: String): List<String>
}

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
        // Batch translation for efficiency
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

### CatalogService Usage

```kotlin
@Service
class CatalogServiceImpl(
    private val tutorProfileRepository: TutorProfileRepository,
    private val localizationService: LocalizationService
) : CatalogService {

    override fun getTutorsForLanguage(
        targetLanguageCode: String,
        sourceLanguageCode: String
    ): List<LocalizedTutorProfile> {
        val tutors = tutorProfileRepository
            .findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(targetLanguageCode)

        return tutors.map { tutor ->
            LocalizedTutorProfile(
                id = tutor.id,
                name = tutor.name,
                emoji = tutor.emoji,
                // Use English fallback for AI translation if needed
                persona = localizationService.getLocalizedText(
                    jsonText = tutor.personaJson,
                    languageCode = sourceLanguageCode,
                    englishFallback = tutor.personaEnglish  // AI translates if missing
                ),
                domain = localizationService.getLocalizedText(
                    jsonText = tutor.domainJson,
                    languageCode = sourceLanguageCode,
                    englishFallback = tutor.domainEnglish
                ),
                description = localizationService.getLocalizedText(
                    jsonText = tutor.descriptionJson,
                    languageCode = sourceLanguageCode,
                    englishFallback = tutor.descriptionEnglish
                ),
                targetLanguageCode = tutor.targetLanguageCode,
                personality = tutor.personality,
                isActive = tutor.isActive,
                displayOrder = tutor.displayOrder
            )
        }
    }
}
```

### Benefits of This Approach

1. ✅ **Zero-Translation MVP**: Launch with English only, no manual translations needed
2. ✅ **Automatic Coverage**: AI fills gaps for any language on-demand
3. ✅ **Progressive Enhancement**: Can manually override AI translations later
4. ✅ **Consistent AI Behavior**: System prompts always use `*English` fields
5. ✅ **Best UX**: Users see content in their language immediately
6. ✅ **Cost-Effective**: Only translate when requested (lazy loading)
7. ✅ **Quality Control**: Can review and persist good translations

### Optional: Async Translation Caching

For better performance, persist AI translations back to the database:

```kotlin
@Service
class TranslationCacheService(
    private val tutorProfileRepository: TutorProfileRepository,
    private val translationService: TranslationService,
    private val objectMapper: ObjectMapper
) {

    @Async
    fun cacheTranslation(
        profileId: UUID,
        field: String,  // "persona", "domain", "description"
        languageCode: String,
        translatedText: String
    ) {
        val profile = tutorProfileRepository.findById(profileId).orElse(null) ?: return

        // Update the JSON field with new translation
        val jsonField = when (field) {
            "persona" -> profile.personaJson
            "domain" -> profile.domainJson
            "description" -> profile.descriptionJson
            else -> return
        }

        val translations = objectMapper.readValue<MutableMap<String, String>>(
            jsonField,
            object : TypeReference<MutableMap<String, String>>() {}
        )

        translations[languageCode] = translatedText

        val updatedJson = objectMapper.writeValueAsString(translations)

        // Save back to database
        when (field) {
            "persona" -> profile.personaJson = updatedJson
            "domain" -> profile.domainJson = updatedJson
            "description" -> profile.descriptionJson = updatedJson
        }

        tutorProfileRepository.save(profile)
    }
}
```

### Seed Data Format (MVP)

```json
{
  "id": "uuid-here",
  "name": "María",
  "emoji": "👩‍🏫",

  "personaEnglish": "patient coach",
  "domainEnglish": "general conversation, grammar, typography",
  "descriptionEnglish": "Patient coach from Madrid who loves helping beginners feel confident",

  "personaJson": "{\"en\": \"patient coach\"}",
  "domainJson": "{\"en\": \"general conversation, grammar, typography\"}",
  "descriptionJson": "{\"en\": \"Patient coach from Madrid who loves helping beginners feel confident\"}",
  "culturalBackgroundJson": "{\"en\": \"from Madrid\"}",

  "targetLanguageCode": "es",
  "personality": "Encouraging"
}
```

**Note**: Only English translations in JSON initially. AI will generate others on-demand.

### Phase Rollout

**Phase 1 (MVP - Week 1-4):**
- ✅ Implement dual storage schema
- ✅ Create seed data with English only
- ✅ Implement `LocalizationService` with AI fallback
- ✅ No manual translations needed

**Phase 2 (Enhancement - Week 5-8):**
- ✅ Enable async translation caching
- ✅ Review and approve AI translations
- ✅ Add manual overrides for critical text

**Phase 3 (Scale - Week 9+):**
- ✅ Community translation contributions
- ✅ Professional translations for marketing
- ✅ A/B test translation quality
