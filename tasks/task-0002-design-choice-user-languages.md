# Design Choice: User Language Proficiency Tracking

**Date**: 2025-01-17
**Status**: Decision Required
**Related**: task-0002-requirements.md

## Context

Should we track a list of languages the user knows (with CEFR levels) in their profile? This would enable:
- Personalized language recommendations
- Smart source language detection
- Progress tracking across multiple languages
- Better onboarding experience

## Current State

Currently, language proficiency is tracked **per session**:
```kotlin
data class ChatSessionEntity(
    val userId: UUID,
    val sourceLanguageCode: String,     // User's native language (for this session)
    val targetLanguageCode: String,     // Language being learned
    val estimatedCEFRLevel: CEFRLevel,  // Current level (for this session)
    // ...
)
```

**Problem**: No centralized view of user's language abilities.

## Option 1: Track User Language Proficiency Profile

### Design

```kotlin
// New entity: User's known languages with proficiency
data class UserLanguageProficiency(
    val id: UUID,
    val userId: UUID,
    val languageCode: String,           // ISO 639-1 (e.g., "es", "fr", "de")
    val proficiencyType: LanguageProficiencyType,
    val cefrLevel: CEFRLevel?,          // A1-C2 (null for native)
    val isNative: Boolean = false,      // True for native languages
    val isPrimary: Boolean = false,     // True for primary native language (used as default source)
    val selfAssessed: Boolean = true,   // True if user self-assessed, false if system-assessed
    val lastAssessedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class LanguageProficiencyType {
    Native,      // Native speaker (no CEFR level)
    Learning,    // Currently learning (has CEFR level)
    Studied,     // Previously studied, not active (has CEFR level)
    Interested   // Wants to learn (no CEFR level yet)
}

// Updated User entity
data class UserEntity(
    val id: UUID,
    val username: String,
    val email: String,
    // ... existing fields ...

    // Optional: cached primary language for quick access
    val primaryLanguageCode: String? = null  // Default source language
)
```

### Repository

```kotlin
interface UserLanguageProficiencyRepository : JpaRepository<UserLanguageProficiencyEntity, UUID> {
    fun findByUserIdOrderByIsNativeDescCefrLevelDesc(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndLanguageCode(userId: UUID, languageCode: String): UserLanguageProficiencyEntity?
    fun findByUserIdAndIsNativeTrue(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndIsPrimaryTrue(userId: UUID): UserLanguageProficiencyEntity?
    fun findByUserIdAndProficiencyTypeIn(userId: UUID, types: List<LanguageProficiencyType>): List<UserLanguageProficiencyEntity>
}
```

### Service Layer

```kotlin
interface UserLanguageService {
    fun addLanguage(userId: UUID, languageCode: String, type: LanguageProficiencyType, cefrLevel: CEFRLevel? = null, isNative: Boolean = false): UserLanguageProficiency
    fun updateLanguage(userId: UUID, languageCode: String, cefrLevel: CEFRLevel): UserLanguageProficiency
    fun getUserLanguages(userId: UUID): List<UserLanguageProficiency>
    fun getNativeLanguages(userId: UUID): List<UserLanguageProficiency>
    fun getLearningLanguages(userId: UUID): List<UserLanguageProficiency>
    fun getPrimaryLanguage(userId: UUID): UserLanguageProficiency?
    fun setPrimaryLanguage(userId: UUID, languageCode: String)
    fun removeLanguage(userId: UUID, languageCode: String)

    // Smart helpers
    fun suggestSourceLanguage(userId: UUID, targetLanguageCode: String): String  // Best source language for teaching target
    fun getRecommendedCourses(userId: UUID): List<CourseTemplate>  // Based on user's languages
}
```

### REST API

```kotlin
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
        return userLanguageService.addLanguage(
            userId = userId,
            languageCode = request.languageCode,
            type = request.type,
            cefrLevel = request.cefrLevel,
            isNative = request.isNative
        ).toResponse()
    }

    @PatchMapping("/{languageCode}")
    fun updateLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        @RequestBody request: UpdateLanguageRequest
    ): UserLanguageProficiencyResponse {
        authorizationService.requireAccessToUser(userId)
        return userLanguageService.updateLanguage(userId, languageCode, request.cefrLevel).toResponse()
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
}
```

### Impact on Course Selection Flow

**Before (without user languages):**
```
1. User clicks "Start Learning"
2. Pick target language: Spanish / French / German
3. Pick source language: English / German / Spanish
4. Pick starting level: A1 / A2 / B1 / ...
5. Browse courses
6. Pick tutor
7. Enroll
```

**After (with user languages):**
```
1. User profile shows:
   - Native: English (primary), Spanish
   - Learning: German (A2), French (A1)
   - Interested: Japanese

2. Dashboard shows:
   - "Continue German (A2)" - Quick resume
   - "Continue French (A1)" - Quick resume
   - "Start learning Japanese" - Suggested courses

3. User clicks "Continue German (A2)":
   - Source language auto-detected: English (primary)
   - Target language: German
   - Level: A2 (from profile)
   - Courses filtered for A2-B1
   - Pick tutor
   - Enroll

4. OR user clicks "Start learning Japanese":
   - Target language: Japanese
   - Source language auto-detected: English
   - Starting level: A1 (new language)
   - Browse beginner courses
   - Pick tutor
   - Enroll
```

### CLI Experience

**Initial Setup (First Run):**
```
$ ./gradlew runCli

Welcome to AI Tutor!
===================

Let's set up your language profile.

What's your native language?
  1. English
  2. Spanish
  3. German
  4. French
  5. Japanese
  6. Other...

> 1

Great! English is your primary language.

Do you speak any other languages? (y/n)
> y

Which language?
  1. Spanish
  2. German
  3. French
  4. Japanese
  [... more options ...]

> 1 (Spanish)

What's your Spanish level?
  1. Native speaker
  2. C2 - Mastery
  3. C1 - Advanced
  4. B2 - Upper Intermediate
  5. B1 - Intermediate
  6. A2 - Elementary
  7. A1 - Beginner

> 6 (A2)

✓ Added Spanish (A2)

Add another language? (y/n)
> n

Perfect! Now let's start learning.

What would you like to learn?
  1. 🇩🇪 German (New!)
  2. 🇫🇷 French (New!)
  3. 🇯🇵 Japanese (New!)
  4. 🇪🇸 Spanish - Continue from A2
  5. ➕ Other language...

> 1 (German)

Great! I'll use English to teach you German.
Browse German courses or start with recommended course?
[... continue to course selection ...]
```

**Returning User:**
```
$ ./gradlew runCli

Welcome back, Eric!
===================
Your Languages:
  Native: 🇬🇧 English (primary), 🇪🇸 Spanish
  Learning: 🇩🇪 German (A2), 🇫🇷 French (A1)

Quick Actions:
  1. Continue German (A2) - Last session: 2 hours ago
  2. Continue French (A1) - Last session: 3 days ago
  3. Practice Spanish - Improve from native to C2?
  4. Start new language
  5. Manage languages

> 1

Resuming German (A2)...
[... continue to existing enrollment or create new session ...]
```

### Pros

✅ **Better Onboarding**: Clear language profile setup
✅ **Smart Defaults**: Auto-detect source language based on proficiency
✅ **Progress Visibility**: See all languages and levels at a glance
✅ **Personalized Recommendations**: Suggest courses based on current levels
✅ **Multi-Language Support**: Easy to track progress across multiple languages
✅ **Source Language Detection**: Automatically pick best source language for tutoring
✅ **Resume Learning**: Quick access to continue learning languages
✅ **Data Insights**: Track user language learning journey over time
✅ **Better UX**: Fewer choices during session creation (auto-filled)
✅ **Gamification Ready**: Can show progress badges, streaks per language

### Cons

❌ **Initial Setup Friction**: Extra steps during registration/first launch
❌ **Data Staleness**: User's level might change but profile not updated
❌ **Complexity**: More tables, services, APIs to maintain
❌ **User Uncertainty**: Users may not know their CEFR level accurately
❌ **Schema Changes**: Requires migration if added later
❌ **Redundancy**: CEFR level stored in both user profile and session

---

## Option 2: Keep Session-Based Only (Status Quo)

### Design

No changes. User specifies language and level each time they create a session.

```kotlin
// Existing
data class ChatSessionEntity(
    val userId: UUID,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val estimatedCEFRLevel: CEFRLevel,
    // ...
)
```

### Flow

**Every Time User Creates Session:**
```
1. Pick target language: Spanish / French / German / ...
2. Pick source language: English / Spanish / German / ...
3. Pick starting level: A1 / A2 / B1 / ...
4. Browse courses (all levels shown)
5. Pick tutor
6. Enroll
```

### Pros

✅ **Simple**: No extra data model
✅ **Flexible**: User can experiment with different levels
✅ **No Stale Data**: Level is always session-specific
✅ **Less Code**: Fewer services, repositories, APIs
✅ **Fast Onboarding**: No language profile setup required

### Cons

❌ **Repetitive**: User must select language/level every time
❌ **No Personalization**: Can't recommend courses based on user's known languages
❌ **Poor Discovery**: All courses shown, not filtered by user's level
❌ **No Progress Tracking**: Can't see language learning journey
❌ **Confusing UX**: Users might create sessions at wrong levels
❌ **No Smart Defaults**: Can't auto-detect source language

---

## Option 3: Hybrid - Optional Language Profile

### Design

Make language profile **optional** but **encouraged**:
- First-time users can skip language setup
- System prompts to add languages after first session
- Language profile enables better features but isn't required

```kotlin
data class UserEntity(
    // ...
    val hasCompletedLanguageProfile: Boolean = false,  // Track setup completion
    val primaryLanguageCode: String? = null  // Cached for quick access
)

// Optional language proficiency (same as Option 1)
data class UserLanguageProficiency(/* ... */)
```

### Flow

**First-Time User (Skip Profile):**
```
1. User launches CLI
2. "Quick start or set up language profile? (q/s)"
3. User chooses "q" (quick start)
4. Standard session creation flow (pick language, level, etc.)
5. After first session: "Would you like to save your language profile? (y/n)"
   - If yes: Saves current session's language/level to profile
   - If no: Continue without profile
```

**User with Profile:**
```
1. User launches CLI
2. "Continue German (A2) or start new language? (c/n)"
3. Smart defaults based on profile
```

### Pros

✅ **Flexible**: Users can choose their experience
✅ **Progressive Enhancement**: Features improve with profile
✅ **Low Friction**: Can skip setup initially
✅ **Growth Path**: Encourages profile completion over time
✅ **Personalization Available**: When profile exists

### Cons

❌ **Complex Logic**: Must handle both with/without profile
❌ **Inconsistent UX**: Different flows for different users
❌ **Incomplete Data**: Some users never complete profile
❌ **Testing Complexity**: More edge cases to test

---

## Recommendation: **Option 1 (Track User Language Profile)**

### Why Option 1 is Best

**Primary Reasons:**

1. **Better User Experience**
   - Smart defaults reduce friction (auto-detect source language)
   - Personalized course recommendations
   - Quick resume for active languages

2. **Product Vision Alignment**
   - Supports multi-language learners (common in language learning apps)
   - Enables progress tracking and gamification
   - Foundation for future features (certificates, streaks, achievements)

3. **Competitive Parity**
   - Duolingo, Babbel, Rosetta Stone all track user language profiles
   - Industry standard for language learning apps

4. **Data Value**
   - Understand user language learning journeys
   - Better recommendations over time
   - Analytics on language popularity and progression

**Secondary Reasons:**

- **Onboarding Quality**: Well-designed setup builds trust and engagement
- **Reduces Repetition**: Don't ask for level every session
- **Scalability**: Supports users learning multiple languages simultaneously
- **Future-Proof**: Enables features like language switching, cross-language insights

### Implementation Strategy

**Phase 1 (MVP - Week 1-2):**
- ✅ Create `UserLanguageProficiency` entity and repository
- ✅ Create `UserLanguageService` with basic CRUD
- ✅ Add REST API endpoints
- ✅ Simple CLI onboarding (native language only)
- ✅ Auto-populate from first session

**Phase 2 (Enhancement - Week 3-4):**
- ✅ Add multiple language support to CLI onboarding
- ✅ Smart source language detection
- ✅ Course recommendations based on levels
- ✅ Quick resume in CLI

**Phase 3 (Polish - Week 5+):**
- ✅ Language proficiency self-assessment quiz (optional)
- ✅ Automatic CEFR level updates based on performance
- ✅ Language progress tracking and visualization
- ✅ Multi-language dashboard in UI

### Addressing the Cons

**Concern: Initial Setup Friction**
- **Solution**: Make it fast (2-3 questions max initially)
- **Solution**: Allow quick start with auto-detection from first session
- **Solution**: Progressive disclosure (add more languages later)

**Concern: Data Staleness**
- **Solution**: Auto-update CEFR level based on session performance
- **Solution**: Prompt user to review levels periodically
- **Solution**: Allow manual level adjustment anytime

**Concern: User Uncertainty (Don't Know CEFR Level)**
- **Solution**: Provide descriptions: "A1 - Beginner (just starting out)"
- **Solution**: Offer self-assessment quiz (optional)
- **Solution**: Default to A1 for new languages, let system adjust

**Concern: Complexity**
- **Solution**: Encapsulate in dedicated service
- **Solution**: Keep API simple (REST follows CRUD patterns)
- **Solution**: Make CLI helpers abstract complexity

### Migration Path

If we already have users:
1. Create `UserLanguageProficiency` table
2. Migrate existing sessions → infer user languages
3. Prompt existing users to confirm/update on next login

```sql
-- Migration: Infer user languages from sessions
INSERT INTO user_language_proficiency (user_id, language_code, proficiency_type, cefr_level, self_assessed)
SELECT DISTINCT
    user_id,
    target_language_code,
    'Learning',
    MAX(estimated_cefr_level),  -- Take highest level from all sessions
    true
FROM chat_sessions
GROUP BY user_id, target_language_code;

-- Mark most common source language as primary
UPDATE users u
SET primary_language_code = (
    SELECT source_language_code
    FROM chat_sessions
    WHERE user_id = u.id
    GROUP BY source_language_code
    ORDER BY COUNT(*) DESC
    LIMIT 1
);
```

### Example Implementation

```kotlin
// Onboarding service
@Service
class UserOnboardingService(
    private val userLanguageService: UserLanguageService,
    private val catalogService: CatalogService
) {
    fun setupNewUser(userId: UUID, nativeLanguageCode: String, primaryLanguage: Boolean = true) {
        userLanguageService.addLanguage(
            userId = userId,
            languageCode = nativeLanguageCode,
            type = LanguageProficiencyType.Native,
            isNative = true
        )

        if (primaryLanguage) {
            userLanguageService.setPrimaryLanguage(userId, nativeLanguageCode)
        }
    }

    fun inferFromSession(userId: UUID, session: ChatSessionEntity) {
        // Check if source language exists
        val sourceExists = userLanguageService.getUserLanguages(userId)
            .any { it.languageCode == session.sourceLanguageCode }

        if (!sourceExists) {
            // Infer as native or high proficiency
            userLanguageService.addLanguage(
                userId = userId,
                languageCode = session.sourceLanguageCode,
                type = LanguageProficiencyType.Native,  // Assume native if used as source
                isNative = true
            )
        }

        // Add or update target language
        val targetLang = userLanguageService.getUserLanguages(userId)
            .find { it.languageCode == session.targetLanguageCode }

        if (targetLang == null) {
            userLanguageService.addLanguage(
                userId = userId,
                languageCode = session.targetLanguageCode,
                type = LanguageProficiencyType.Learning,
                cefrLevel = session.estimatedCEFRLevel
            )
        } else if (targetLang.cefrLevel != session.estimatedCEFRLevel) {
            // Update level if session has higher level
            if (session.estimatedCEFRLevel.ordinal > targetLang.cefrLevel?.ordinal ?: 0) {
                userLanguageService.updateLanguage(userId, session.targetLanguageCode, session.estimatedCEFRLevel)
            }
        }
    }
}
```

### CLI Commands

```
/languages              List your language profile
/languages add          Add a language to your profile
/languages update       Update language level
/languages remove       Remove a language
/languages primary      Set primary native language
```

## Decision

**✅ APPROVED: Option 1 - Track User Language Proficiency Profile**

**Date**: 2025-01-17

This provides the best foundation for a quality language learning platform while supporting both single-language and multi-language learners.

### Implementation Scope

**Phase 1 (MVP):**
- Track user's native languages (at least one, with primary flag)
- Track languages being learned with CEFR levels
- Auto-populate from first session if not set
- Smart source language detection
- Basic CLI onboarding (1-2 questions)

**Phase 2:**
- Multiple language support
- Quick resume functionality
- Course recommendations based on levels
- Progress tracking visualization

**Phase 3:**
- Self-assessment quiz
- Automatic level updates based on performance
- Multi-language dashboard
- Gamification features
