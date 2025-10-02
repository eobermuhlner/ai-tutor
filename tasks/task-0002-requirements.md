# Task 0002: Catalog-Based Tutor/Language/Course Management

**Status**: Draft
**Proposal**: #1 - Catalog-Based System with Templates
**Theme**: "Choose Your Adventure" - Pre-configured learning paths with customization

## Overview

Implement a catalog-based system where users can browse and select from pre-configured **Language Packs**, **Tutor Profiles**, and **Course Templates**. This provides a beginner-friendly, guided experience while maintaining flexibility for advanced users.

## Goals

1. **Reduce Decision Paralysis**: Provide curated, ready-to-use learning configurations
2. **Improve Discoverability**: Make it easy to explore available tutors, languages, and courses
3. **Enable Scalability**: Allow easy addition of new courses and tutors
4. **Support Both CLI and Future UI**: Design with both interfaces in mind
5. **Maintain Flexibility**: Allow custom configurations for advanced users

## User Stories

### As a new user
- I want to see what languages are available so I can choose one to learn
- I want to see suggested courses for my chosen language so I know where to start
- I want to pick a tutor personality that matches my learning style
- I want to start learning with minimal configuration

### As a returning user
- I want to see my enrolled courses so I can continue where I left off
- I want to track my progress within each course
- I want to switch between multiple courses/languages easily

### As an advanced user
- I want to create custom language pairs not in the catalog
- I want to define custom tutor personalities
- I want to create my own learning paths

## Domain Model

### Core Entities

```kotlin
// User Language Proficiency - Track user's known languages
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
    Native,      // Native speaker (no CEFR level needed)
    Learning,    // Currently learning (has CEFR level)
    Studied,     // Previously studied, not active (has CEFR level)
    Interested   // Wants to learn (no CEFR level yet)
}

// Language Pack - Metadata about a language
data class LanguagePack(
    val id: UUID,
    val code: String,              // ISO 639-1 (e.g., "es", "fr")
    val nameJson: String,          // JSON map: {"en": "Spanish", "es": "Español", "de": "Spanisch"}
    val flagEmoji: String,         // Unicode flag emoji
    val nativeName: String,        // Native language name (always in target language, e.g., "Español")
    val difficultyJson: String,    // JSON map of difficulty descriptions per source language
    val descriptionJson: String,   // JSON map of descriptions
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

enum class Difficulty {
    Easy, Medium, Hard
}

// Tutor Profile - Template for tutor personality
// NOTE: Tutors are language-specific for authenticity
// - Names match target language culture (e.g., "Maria" for Spanish, "François" for French)
// - DUAL STORAGE: English for AI prompts + JSON for UI localization
data class TutorProfile(
    val id: UUID,
    val name: String,              // Culture-appropriate name (e.g., "Maria", "François", "Yuki")
    val emoji: String,             // Avatar emoji (e.g., "👩‍🏫", "👨‍🎓")

    // For AI system prompt (English only, always required)
    val personaEnglish: String,    // "patient coach"
    val domainEnglish: String,     // "general conversation, grammar, typography"
    val descriptionEnglish: String, // "Patient coach from Madrid who loves helping beginners"

    // For UI catalog (multilingual JSON with AI translation fallback)
    val personaJson: String,       // JSON map: {"en": "patient coach", "de": "geduldiger Coach", ...}
    val domainJson: String,        // JSON map: {"en": "general conversation, grammar", ...}
    val descriptionJson: String,   // JSON map of full descriptions
    val culturalBackgroundJson: String?,  // Optional JSON map: {"en": "from Barcelona", "es": "de Barcelona", ...}

    val personality: TutorPersonality,
    val targetLanguageCode: String,  // The language this tutor teaches (e.g., "es", "fr")
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

enum class TutorPersonality {
    Encouraging,    // Positive reinforcement, patient
    Strict,         // High standards, formal
    Casual,         // Friendly, informal
    Professional,   // Business-focused, formal but approachable
    Academic        // Scholarly, detailed explanations
}

// Course Template - Structured learning path
data class CourseTemplate(
    val id: UUID,
    val languageCode: String,
    val nameJson: String,          // JSON map: {"en": "Spanish for Travelers", "es": "Español para viajeros", ...}
    val shortDescriptionJson: String,  // JSON map of short descriptions
    val descriptionJson: String,   // JSON map of full descriptions
    val category: CourseCategory,
    val targetAudienceJson: String,  // JSON map: {"en": "Beginners planning a trip", ...}
    val startingLevel: CEFRLevel,
    val targetLevel: CEFRLevel,
    val estimatedWeeks: Int?,      // null = self-paced
    val suggestedTutorIds: List<UUID>,  // Recommended tutors
    val defaultPhase: ConversationPhase = ConversationPhase.Auto,
    val topicSequence: List<String>?,  // Suggested topic progression (language-neutral or localized?)
    val learningGoalsJson: String,   // JSON array of learning goals per language
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val tags: List<String> = emptyList()
)

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

// User Course Enrollment - User's active courses
data class UserCourseEnrollment(
    val id: UUID,
    val userId: UUID,
    val courseTemplateId: UUID,
    val tutorProfileId: UUID,
    val languageCode: String,      // Target language
    val sourceLanguageCode: String = "en",
    val customName: String?,       // User can rename enrollment
    val currentLevel: CEFRLevel,
    val currentTopicIndex: Int = 0,
    val enrolledAt: Instant,
    val lastAccessedAt: Instant,
    val completedTopics: List<String> = emptyList(),
    val progressPercentage: Int = 0,
    val vocabularyCount: Int = 0,
    val sessionCount: Int = 0,
    val streakDays: Int = 0,
    val isActive: Boolean = true
)
```

### Repository Layer

```kotlin
// JPA Entities + Repositories
interface UserLanguageProficiencyRepository : JpaRepository<UserLanguageProficiencyEntity, UUID> {
    fun findByUserIdOrderByIsNativeDescCefrLevelDesc(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndLanguageCode(userId: UUID, languageCode: String): UserLanguageProficiencyEntity?
    fun findByUserIdAndIsNativeTrue(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndIsPrimaryTrue(userId: UUID): UserLanguageProficiencyEntity?
    fun findByUserIdAndProficiencyTypeIn(userId: UUID, types: List<LanguageProficiencyType>): List<UserLanguageProficiencyEntity>
}

interface LanguagePackRepository : JpaRepository<LanguagePackEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<LanguagePackEntity>
    fun findByCode(code: String): LanguagePackEntity?
}

interface TutorProfileRepository : JpaRepository<TutorProfileEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<TutorProfileEntity>
    fun findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<TutorProfileEntity>
    fun findByTargetLanguageCodeInAndIsActiveTrueOrderByDisplayOrder(languageCodes: List<String>): List<TutorProfileEntity>
}

interface CourseTemplateRepository : JpaRepository<CourseTemplateEntity, UUID> {
    fun findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<CourseTemplateEntity>
    fun findByCategoryAndIsActiveTrue(category: CourseCategory): List<CourseTemplateEntity>
}

interface UserCourseEnrollmentRepository : JpaRepository<UserCourseEnrollmentEntity, UUID> {
    fun findByUserIdAndIsActiveTrueOrderByLastAccessedAtDesc(userId: UUID): List<UserCourseEnrollmentEntity>
    fun findByUserIdAndCourseTemplateIdAndIsActiveTrue(userId: UUID, courseTemplateId: UUID): UserCourseEnrollmentEntity?
}
```

### Service Layer

```kotlin
// Localization Service - Handle multilingual content with AI translation fallback
interface LocalizationService {
    fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,           // NEW: English text for AI translation if missing
        fallbackLanguage: String = "en"
    ): String
    fun parseMultilingualJson(jsonText: String): Map<String, String>
}

// Translation Service - AI-powered translation
interface TranslationService {
    fun translate(text: String, from: String, to: String): String
    fun batchTranslate(texts: List<String>, from: String, to: String): List<String>
}

// User Language Service - Manage user's language proficiency
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
    fun suggestSourceLanguage(userId: UUID, targetLanguageCode: String): String
    fun inferFromSession(userId: UUID, session: ChatSessionEntity)  // Auto-populate from sessions
}

// Catalog Service - Browse available options
interface CatalogService {
    fun getAvailableLanguages(): List<LanguagePack>
    fun getCoursesForLanguage(languageCode: String, sourceLanguageCode: String = "en"): List<CourseTemplate>
    fun getTutorsForLanguage(targetLanguageCode: String, sourceLanguageCode: String = "en"): List<TutorProfile>
    fun getTutorsForCourse(courseTemplateId: UUID, sourceLanguageCode: String = "en"): List<TutorProfile>
    fun getCoursesByCategory(category: CourseCategory, sourceLanguageCode: String = "en"): List<CourseTemplate>
    fun searchCourses(query: String, languageCode: String? = null, sourceLanguageCode: String = "en"): List<CourseTemplate>

    // Helper to localize tutor profile
    fun localizeProfile(profile: TutorProfile, sourceLanguageCode: String): LocalizedTutorProfile
}

// Localized view of TutorProfile (with resolved text)
data class LocalizedTutorProfile(
    val id: UUID,
    val name: String,
    val emoji: String,
    val persona: String,              // Resolved from JSON
    val domain: String,               // Resolved from JSON
    val personality: TutorPersonality,
    val description: String,          // Resolved from JSON
    val targetLanguageCode: String,
    val culturalBackground: String?,  // Resolved from JSON
    val isActive: Boolean,
    val displayOrder: Int
)

// Enrollment Service - Manage user enrollments
interface EnrollmentService {
    fun enrollInCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String = "en",
        customName: String? = null,
        startingLevel: CEFRLevel? = null
    ): UserCourseEnrollment

    fun getUserEnrollments(userId: UUID): List<UserCourseEnrollment>
    fun getActiveEnrollments(userId: UUID): List<UserCourseEnrollment>
    fun updateEnrollmentProgress(enrollmentId: UUID, topicsCompleted: List<String>, vocabularyCount: Int)
    fun updateEnrollmentAccess(enrollmentId: UUID)  // Update lastAccessedAt
    fun deactivateEnrollment(enrollmentId: UUID)
}

// Session Factory - Create sessions from enrollments
interface SessionFactoryService {
    fun createSessionFromEnrollment(enrollmentId: UUID): SessionResponse
    fun getRecommendedTopic(enrollmentId: UUID): String?
}
```

### REST API

```kotlin
// New controller: /api/v1/users/{userId}/languages
@RestController
@RequestMapping("/api/v1/users/{userId}/languages")
class UserLanguageController(
    private val userLanguageService: UserLanguageService,
    private val authorizationService: AuthorizationService
) {
    @GetMapping
    fun getUserLanguages(@PathVariable userId: UUID): List<UserLanguageProficiencyResponse>

    @PostMapping
    fun addLanguage(
        @PathVariable userId: UUID,
        @RequestBody request: AddLanguageRequest
    ): UserLanguageProficiencyResponse

    @PatchMapping("/{languageCode}")
    fun updateLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String,
        @RequestBody request: UpdateLanguageRequest
    ): UserLanguageProficiencyResponse

    @PatchMapping("/{languageCode}/set-primary")
    fun setPrimaryLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String
    ): ResponseEntity<Void>

    @DeleteMapping("/{languageCode}")
    fun removeLanguage(
        @PathVariable userId: UUID,
        @PathVariable languageCode: String
    ): ResponseEntity<Void>
}

// New controller: /api/v1/catalog
@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService
) {
    @GetMapping("/languages")
    fun getLanguages(): List<LanguagePackResponse>

    @GetMapping("/languages/{code}/courses")
    fun getCoursesForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<CourseTemplateResponse>

    @GetMapping("/languages/{code}/tutors")
    fun getTutorsForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<TutorProfileResponse>

    @GetMapping("/courses/{id}")
    fun getCourseDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): CourseTemplateDetailResponse

    @GetMapping("/tutors/{id}")
    fun getTutorDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): TutorProfileDetailResponse
}

// New controller: /api/v1/enrollments
@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(
    private val enrollmentService: EnrollmentService,
    private val sessionFactoryService: SessionFactoryService,
    private val authorizationService: AuthorizationService
) {
    @PostMapping
    fun enrollInCourse(@RequestBody request: EnrollmentRequest): EnrollmentResponse

    @GetMapping
    fun getUserEnrollments(@RequestParam userId: UUID?): List<EnrollmentResponse>

    @GetMapping("/{id}")
    fun getEnrollmentDetails(@PathVariable id: UUID): EnrollmentDetailResponse

    @PostMapping("/{id}/start-session")
    fun startSessionFromEnrollment(@PathVariable id: UUID): SessionResponse

    @PatchMapping("/{id}/progress")
    fun updateProgress(@PathVariable id: UUID, @RequestBody request: UpdateProgressRequest): EnrollmentResponse

    @DeleteMapping("/{id}")
    fun deactivateEnrollment(@PathVariable id: UUID): ResponseEntity<Void>
}
```

## CLI Experience Design

### New Commands

```
/languages              List available languages
/courses [lang]         List courses (optionally filtered by language)
/tutors [lang]          List tutors (optionally filtered by language)
/enroll                 Interactive enrollment wizard
/enrollments            List your enrolled courses
/continue [id]          Continue an enrollment (creates session)
/progress               Show progress for current enrollment
```

### CLI Workflow

#### First-Time User Flow
```
$ ./gradlew runCli

Welcome to AI Tutor!
===================
No enrollments found. Let's get started!

📚 Available Languages:
  1. Spanish 🇪🇸 (12 courses) - Easy
  2. French 🇫🇷 (8 courses) - Medium
  3. German 🇩🇪 (6 courses) - Medium
  4. Japanese 🇯🇵 (4 courses) - Hard
  5. ➕ Custom language pair

Choose a language (1-5): 1

Spanish 🇪🇸 - Available Courses:
  1. ⭐ Spanish for Travelers (A1→A2, 6 weeks)
     "Essential phrases for tourists and travelers"

  2. 💼 Business Spanish (B1→B2, 8 weeks)
     "Professional communication for workplace"

  3. 🗣️ Conversational Spanish (A1→C1, Self-paced)
     "Master everyday conversations"

  4. 📖 Spanish Grammar Intensive (A2→B2, 10 weeks)
     "Deep dive into Spanish grammar rules"

  5. ➕ Custom course

Choose a course (1-5): 3

Conversational Spanish 🗣️
=========================
Description: Master everyday conversations at your own pace
Target: A1 → C1
Category: Conversational
Duration: Self-paced

What you'll learn:
  • Greetings and introductions
  • Talking about daily routines
  • Describing people and places
  • Ordering food and shopping
  • Expressing opinions and feelings
  • And much more!

Choose your tutor:
  1. 👩‍🏫 Maria (Patient coach)
     "Encouraging and supportive, perfect for beginners"
     Personality: Encouraging

  2. 🎓 Professor Rodriguez (Strict academic)
     "High standards, detailed explanations"
     Personality: Strict

  3. 😊 Carlos (Casual friend)
     "Relaxed and fun, like chatting with a friend"
     Personality: Casual

  4. ➕ Custom tutor

Choose tutor (1-4): 1

Your Spanish level:
  1. A1 - Beginner (just starting)
  2. A2 - Elementary (basic phrases)
  3. B1 - Intermediate (simple conversations)
  4. B2 - Upper Intermediate (complex topics)
  5. C1 - Advanced (fluent)

Your level (1-5): 1

✓ Enrolled in "Conversational Spanish"
  Tutor: Maria (Patient coach)
  Level: A1

Starting your first session...

Maria: ¡Hola! Welcome to your Spanish journey! I'm Maria...
```

#### Returning User Flow
```
$ ./gradlew runCli

Welcome back, Eric!
===================
📚 Your Courses:
  1. 🇪🇸 Conversational Spanish (Maria)
     Progress: 20% | 150 words | 25 sessions
     Last: 2 hours ago | 🔥 7-day streak

  2. 🇩🇪 Work German (Herr Schmidt)
     Progress: 5% | 30 words | 3 sessions
     Last: 2 weeks ago

Continue which course? (1-2, or 'new'): 1

Resuming "Conversational Spanish" with Maria...
Current topic: Ordering food at restaurants

Maria: ¡Hola Eric! Ready to continue practicing?
```

### CLI Configuration Changes

Update `CliConfig.kt` to support enrollments:

```kotlin
@Serializable
data class CliConfig(
    // ... existing fields ...

    // Enrollment preferences
    val lastEnrollmentId: String? = null,
    val defaultSourceLanguage: String = "en",

    // Deprecated (migrate to enrollment-based)
    @Deprecated("Use enrollments instead")
    val defaultTutor: String = "Maria",
    // ... other deprecated fields
)
```

## Implementation Plan

### Phase 1: Core Domain & Data Layer (Week 1-2)

**Tasks:**
1. ✅ Create domain models (UserLanguageProficiency, LanguagePack, TutorProfile, CourseTemplate, UserCourseEnrollment)
2. ✅ Create JPA entities with proper relationships
3. ✅ Create repositories with query methods
4. ✅ Add database migrations (Flyway/Liquibase) for new tables
5. ✅ Write unit tests for domain models

**Deliverables:**
- Domain models in `core/model/catalog/`
- JPA entities in `catalog/domain/` and `user/domain/`
- Repositories in `catalog/repository/` and `user/repository/`
- SQL migration scripts

### Phase 2: Service Layer (Week 2-3)

**Tasks:**
1. ✅ Implement `UserLanguageService` with proficiency management
2. ✅ Implement `CatalogService` with business logic
3. ✅ Implement `EnrollmentService` with enrollment management
4. ✅ Implement `SessionFactoryService` to create sessions from enrollments
5. ✅ Implement `LocalizationService` with AI translation fallback
6. ✅ Implement `TranslationService` (OpenAI-powered)
7. ✅ Add validation and error handling
8. ✅ Write unit tests with MockK

**Deliverables:**
- Service implementations in `catalog/service/` and `user/service/`
- Test coverage >80%

### Phase 3: REST API (Week 3-4)

**Tasks:**
1. ✅ Create DTOs for user language, catalog, and enrollment endpoints
2. ✅ Implement `UserLanguageController` with endpoints
3. ✅ Implement `CatalogController` with endpoints
4. ✅ Implement `EnrollmentController` with endpoints
5. ✅ Add authorization checks (users can only manage their own data)
6. ✅ Write controller tests with MockMvc
7. ✅ Add HTTP client test examples

**Deliverables:**
- Controllers in `catalog/controller/` and `user/controller/`
- DTOs in `catalog/dto/` and `user/dto/`
- HTTP test examples in `src/test/http/`

### Phase 4: Seed Data & Content (Week 4-5)

**Tasks:**
1. ✅ Create seed data for LanguagePacks (Spanish, French, German, Japanese)
2. ✅ Create seed data for TutorProfiles (5-10 diverse personalities per language)
3. ✅ Create seed data for CourseTemplates (3-5 per language)
4. ✅ Add data initialization service (runs on startup in dev mode)
5. ✅ Document seed data structure for future additions

**Deliverables:**
- Seed data JSON/SQL files
- Data initialization service
- Content guidelines documentation

**Example Tutor Profiles by Language:**

**Spanish Tutors:**
- **María** 👩‍🏫 - Encouraging, "Patient coach from Madrid who loves helping beginners"
- **Professor Rodríguez** 🎓 - Strict, "Academic expert in Spanish grammar and literature"
- **Carlos** 😊 - Casual, "Friendly guy from Barcelona who makes learning fun"
- **Señora Hernández** 💼 - Professional, "Business Spanish specialist with 20 years experience"
- **Abuela Rosa** 👵 - Encouraging, "Warm grandmother figure who teaches like family"

**French Tutors:**
- **François** 👨‍🏫 - Academic, "Parisian professor with expertise in French linguistics"
- **Céline** 😊 - Casual, "Young tutor from Lyon who loves casual conversation"
- **Madame Dubois** 💼 - Professional, "Business French expert for professionals"
- **Pierre** 🎓 - Strict, "Traditional teacher focused on proper grammar"

**German Tutors:**
- **Herr Schmidt** 🎓 - Strict, "Traditional grammar expert from Berlin"
- **Anna** 😊 - Casual, "Friendly tutor from Munich who keeps it relaxed"
- **Frau Müller** 💼 - Professional, "Business German specialist"
- **Hans** 👨‍🏫 - Encouraging, "Patient teacher who makes German fun"

**Japanese Tutors:**
- **Yuki** 👩‍🏫 - Encouraging, "Patient teacher from Tokyo, specializes in beginners"
- **Tanaka-sensei** 🎓 - Strict, "Traditional teacher focused on proper form and etiquette"
- **Kenji** 😊 - Casual, "Young tutor who teaches modern conversational Japanese"
- **Yamamoto-san** 💼 - Professional, "Business Japanese expert for corporate learners"

### Phase 5: CLI Integration (Week 5-6)

**Tasks:**
1. ✅ Add language profile onboarding flow (first-time users)
2. ✅ Add new CLI commands (/languages, /courses, /tutors, /enroll, etc.)
3. ✅ Implement smart defaults based on user language profile
4. ✅ Implement interactive enrollment wizard
5. ✅ Update startup flow to show language profile and quick resume
6. ✅ Add enrollment-based session resumption
7. ✅ Update CliConfig to support enrollment IDs
8. ✅ Add progress display in CLI
9. ✅ Update CLI help text

**Deliverables:**
- Updated `AiTutorCli.kt` with new commands and onboarding
- Updated `HttpApiClient.kt` with catalog/enrollment/language endpoints
- Migration guide for CLI config

### Phase 6: Documentation & Polish (Week 6-7)

**Tasks:**
1. ✅ Update README.md with catalog/enrollment features
2. ✅ Update CLAUDE.md with new architecture
3. ✅ Add API documentation (OpenAPI/Swagger)
4. ✅ Create user guide for course catalog
5. ✅ Create content creator guide for adding courses
6. ✅ Add telemetry for popular courses/tutors

**Deliverables:**
- Updated documentation
- Content creation guide
- Analytics setup

### Phase 7: Testing & Release (Week 7-8)

**Tasks:**
1. ✅ Integration testing with real database
2. ✅ End-to-end CLI testing
3. ✅ Performance testing (catalog queries)
4. ✅ User acceptance testing
5. ✅ Bug fixes and polish
6. ✅ Release notes

**Deliverables:**
- Test reports
- Bug fixes
- v1.0 release with catalog system

## Future Enhancements (Post-MVP)

### Phase 8: Advanced Features
- **Course Progress Tracking**: Track completion of topics within courses
- **Certificates**: Generate completion certificates for courses
- **Custom Courses**: Allow users to create and share custom courses
- **Course Ratings**: User ratings and reviews for courses
- **Adaptive Courses**: AI-adjusted course difficulty based on performance

### Phase 9: Community Features
- **Community Courses**: User-generated course marketplace
- **Tutor Customization**: Users can customize tutor personalities
- **Course Bundles**: Package related courses together
- **Learning Paths**: Multi-course learning journeys (e.g., "Zero to Fluent Spanish")

### Phase 10: Integration with Phase 2 & 3 Proposals
- Add "Smart Picker" quick-start flow alongside catalog
- Add "Learning Profiles" for users with multiple enrollments
- Hybrid navigation: Quick continue + Browse catalog

## Success Metrics

### User Engagement
- **Enrollment Rate**: % of users who enroll in at least one course
- **Course Completion Rate**: % of enrolled users who complete courses
- **Multi-Enrollment Rate**: % of users enrolled in 2+ courses
- **Retention**: 7-day and 30-day user retention

### Content Metrics
- **Course Popularity**: Most enrolled courses
- **Tutor Popularity**: Most selected tutors
- **Language Popularity**: Most studied languages
- **Drop-off Points**: Where users abandon courses

### Technical Metrics
- **API Performance**: Catalog endpoint response times <200ms
- **Database Query Performance**: Enrollment queries <100ms
- **CLI Startup Time**: <2s from launch to ready

## Design Decisions

### Language-Specific Tutors

**Decision**: Each tutor is tied to a specific target language, with culturally appropriate names and backgrounds.

**Rationale:**
1. **Authenticity**: A tutor named "María" teaching Spanish feels more authentic than a generic "Tutor A"
2. **Cultural Context**: Tutors can embody cultural nuances of their language (e.g., "Parisian" vs. "Quebec French")
3. **Immersion**: Names in the target language increase immersion and learning motivation
4. **Scalability**: Easy to add regional variants (e.g., "María from Spain" vs. "María from Mexico")

**Implementation:**
- Tutor names use target language conventions (María, François, Yuki, Hans)
- **DUAL STORAGE**: English fields for AI system prompts + JSON fields for UI localization
- System prompt **always uses English** for consistent AI behavior
- UI displays localized text based on user's source language preference
- **AI translation fallback**: If translation missing, automatically translates from English
- Optional `culturalBackground` adds regional flavor in multiple languages
- Tutors are filtered by `targetLanguageCode` when browsing courses

**Multilingual Storage Format:**
All text fields are stored as JSON objects mapping language codes to translations:
```json
{
  "en": "Patient coach who loves helping beginners",
  "es": "Entrenadora paciente que ama ayudar a principiantes",
  "de": "Geduldiger Coach, der gerne Anfängern hilft",
  "fr": "Coach patient qui aime aider les débutants"
}
```

**Examples (Seed Data - MVP with English only):**
```kotlin
TutorProfile(
    name = "María",
    emoji = "👩‍🏫",

    // For AI system prompt (always English)
    personaEnglish = "patient coach",
    domainEnglish = "general conversation, grammar, typography",
    descriptionEnglish = "Patient coach from Madrid who loves helping beginners feel confident",

    // For UI catalog (English only initially, AI translates on-demand)
    personaJson = """{"en": "patient coach"}""",
    domainJson = """{"en": "general conversation, grammar, typography"}""",
    descriptionJson = """{"en": "Patient coach from Madrid who loves helping beginners feel confident"}""",
    culturalBackgroundJson = """{"en": "from Madrid"}""",

    targetLanguageCode = "es",
    personality = Encouraging
)

TutorProfile(
    name = "François",
    emoji = "👨‍🏫",

    // For AI system prompt (always English)
    personaEnglish = "academic professor",
    domainEnglish = "French linguistics, literature, formal grammar",
    descriptionEnglish = "Parisian professor with expertise in French linguistics and literature",

    // For UI catalog (English only initially)
    personaJson = """{"en": "academic professor"}""",
    domainJson = """{"en": "French linguistics, literature, formal grammar"}""",
    descriptionJson = """{"en": "Parisian professor with expertise in French linguistics and literature"}""",
    culturalBackgroundJson = """{"en": "Parisian"}""",

    targetLanguageCode = "fr",
    personality = Academic
)
```

**Note**: Only English in JSON initially. When a German user views María's profile:
1. Service calls `getLocalizedText(personaJson, "de", personaEnglish)`
2. Finds no "de" translation in JSON
3. AI translates "patient coach" → "geduldiger Coach"
4. Returns "geduldiger Coach" to user
5. (Optional) Async caches translation back to database

**API Response (localized for user's source language):**
```kotlin
// When German speaker (sourceLanguage="de") browses Spanish tutors:
TutorProfileResponse(
    name = "María",
    persona = "geduldiger Coach",  // Pulled from JSON based on user's sourceLanguage
    description = "Geduldiger Coach aus Madrid, der Anfängern gerne hilft",
    culturalBackground = "aus Madrid",
    targetLanguageCode = "es"
)
```

### Alternative Considered: Universal Tutors

We considered language-agnostic tutors (e.g., "The Patient Coach" for all languages), but rejected it because:
- ❌ Less immersive
- ❌ Harder to add cultural context
- ❌ Misses opportunity for regional dialects/variants
- ❌ Less engaging for learners

### Multilingual Content Architecture

**Decision**: Store all user-facing text as JSON maps (language code → translation) rather than single-language strings.

**Rationale:**
1. **Global Accessibility**: Users speak different native languages (source language varies)
2. **Better UX**: German speakers see German descriptions of Spanish tutors, not English
3. **Scalability**: Easy to add new source language translations without schema changes
4. **Fallback Support**: If translation missing, fall back to English (lingua franca)

**Storage Example:**
```json
{
  "descriptionJson": "{\"en\": \"Patient coach from Madrid\", \"es\": \"Entrenadora paciente de Madrid\", \"de\": \"Geduldiger Coach aus Madrid\"}"
}
```

**API Flow:**
1. Client sends `sourceLanguage` parameter (e.g., `sourceLanguage=de`)
2. Service uses `LocalizationService` to extract German text from JSON
3. Response contains localized text: `"description": "Geduldiger Coach aus Madrid"`
4. If German missing, falls back to English

**Implementation Strategy:**
```kotlin
class LocalizationServiceImpl : LocalizationService {
    private val objectMapper = ObjectMapper()

    override fun getLocalizedText(jsonText: String, languageCode: String, fallbackLanguage: String): String {
        val translations = parseMultilingualJson(jsonText)
        return translations[languageCode]
            ?: translations[fallbackLanguage]
            ?: translations["en"]
            ?: "Translation missing"
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

**Initial Language Coverage (MVP):**
- ✅ English (en) - Primary, always required
- ✅ Spanish (es)
- ✅ German (de)
- 🔄 French (fr) - Phase 2
- 🔄 Japanese (ja) - Phase 2

**Translation Strategy:**
- Phase 1 (MVP): English only, with placeholder structure for other languages
- Phase 2: Add AI-generated translations (GPT-4), manual review recommended
- Phase 3: Community contributions + professional translation for marketing materials

## Open Questions

1. **Content Creation**: Who creates initial course content? Do we need a CMS?
2. ~~**Multi-Language UI**: Should course descriptions be multilingual?~~ **RESOLVED**: Yes, all content stored as JSON maps
3. **Course Updates**: How do we handle updates to course templates for existing enrollments?
4. **Free vs. Paid**: Should some courses be premium/paid?
5. **Progress Syncing**: How do we sync progress between CLI and future web UI?
6. **Offline Mode**: Should CLI support offline course access?
7. **Regional Variants**: Do we need separate tutors for regional variants (e.g., Spain vs. Latin America Spanish)?
8. **Tutor Voices**: Should tutors have AI-generated voice profiles for future audio features?
9. **Translation Coverage**: Which source languages should we support initially? (English, Spanish, German, French, Japanese?)
10. **Translation Quality**: Use AI translation for seed data or require manual translations?
11. **Fallback Strategy**: If translation missing for user's source language, show English or target language?

## References

- Current codebase: `/src/main/kotlin/ch/obermuhlner/aitutor/`
- CLI client: `/src/main/kotlin/ch/obermuhlner/aitutor/cli/`
- Session management: `/src/main/kotlin/ch/obermuhlner/aitutor/chat/`
- Existing domain models: `/src/main/kotlin/ch/obermuhlner/aitutor/core/model/`

### Design Choice Documents
- **Localization Strategy**: `task-0002-design-choice-localization.md` - Dual storage (English for AI + JSON for UI) with AI translation fallback
- **User Language Proficiency**: `task-0002-design-choice-user-languages.md` - Track user's known languages with CEFR levels for personalization
