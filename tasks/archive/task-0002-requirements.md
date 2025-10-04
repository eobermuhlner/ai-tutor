# Task 0002: Catalog-Based Tutor/Language/Course Management

**Status**: Architecture Review Applied - Simplified Design
**Proposal**: #1 - Catalog-Based System with Templates (Simplified)
**Theme**: "Choose Your Adventure" - Pre-configured learning paths with customization
**Review**: See `task-0002-architecture-review.md` for full analysis

## Overview

Implement a **simplified** catalog-based system where users can browse and select from pre-configured **Tutor Profiles** and **Course Templates**. This provides a beginner-friendly, guided experience while maintaining flexibility for advanced users.

**Key Simplifications Applied:**
- ❌ **Removed UserCourseEnrollment entity** - Course metadata added directly to ChatSessionEntity
- ❌ **Removed LanguagePack entity** - Language metadata moved to configuration
- ❌ **Removed SessionFactoryService** - Logic merged into ChatService
- ❌ **Removed LocalizedTutorProfile wrapper** - Use DTOs directly
- ✅ **Result**: 3 entities instead of 5, ~40% less code, same functionality

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
- I want to see my active learning sessions so I can continue where I left off
- I want to track my progress within each session
- I want to switch between multiple sessions/languages easily

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
}

// ❌ REMOVED: LanguagePack entity
// Language metadata is now in application configuration (LanguageConfig.kt)
// Rationale: Static data that rarely changes, no need for database table
data class LanguageMetadata(
    val code: String,              // ISO 639-1 (e.g., "es", "fr")
    val nameJson: String,          // JSON map: {"en": "Spanish", "es": "Español", "de": "Spanisch"}
    val flagEmoji: String,         // Unicode flag emoji
    val nativeName: String,        // Native language name (e.g., "Español")
    val difficulty: Difficulty,
    val descriptionJson: String
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

// ❌ REMOVED: UserCourseEnrollment entity
// Course metadata is now stored directly in ChatSessionEntity
// Rationale: Enrollment was essentially a "session with metadata" - simpler to extend existing entity

// Extended ChatSession Entity - Adds course/tutor metadata to existing sessions
// NOTE: This extends the existing ChatSessionEntity (see chat/domain/ChatSessionEntity.kt)
// New fields added to ChatSessionEntity:
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // ... existing fields (id, userId, tutorName, tutorPersona, tutorDomain, etc.) ...

    // NEW: Course-related fields (nullable for backward compatibility)
    @Column(name = "course_template_id")
    var courseTemplateId: UUID? = null,     // Reference to CourseTemplate

    @Column(name = "tutor_profile_id")
    var tutorProfileId: UUID? = null,       // Reference to TutorProfile

    @Column(name = "custom_session_name", length = 256)
    var customName: String? = null,         // User can rename session (e.g., "Spanish with Maria")

    // ... existing fields (createdAt, updatedAt) ...
)

// Progress is calculated on-demand from existing data:
// - Message count: Count of ChatMessageEntity for this session
// - Vocabulary count: Count of VocabularyItemEntity for user+language
// - Streak: Calculated from session access patterns
// No denormalized fields needed in MVP!
```

### Repository Layer

```kotlin
// User Language Proficiency Repository
interface UserLanguageProficiencyRepository : JpaRepository<UserLanguageProficiencyEntity, UUID> {
    fun findByUserIdOrderByIsNativeDescCefrLevelDesc(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndLanguageCode(userId: UUID, languageCode: String): UserLanguageProficiencyEntity?
    fun findByUserIdAndIsNativeTrue(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndIsPrimaryTrue(userId: UUID): UserLanguageProficiencyEntity?
    fun findByUserIdAndProficiencyType(userId: UUID, type: LanguageProficiencyType): List<UserLanguageProficiencyEntity>
}

// ❌ REMOVED: LanguagePackRepository (language metadata now in configuration)

// Tutor Profile Repository
interface TutorProfileRepository : JpaRepository<TutorProfileEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<TutorProfileEntity>
    fun findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<TutorProfileEntity>
}

// Course Template Repository
interface CourseTemplateRepository : JpaRepository<CourseTemplateEntity, UUID> {
    fun findByLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<CourseTemplateEntity>
    fun findByCategoryAndIsActiveTrue(category: CourseCategory): List<CourseTemplateEntity>
    fun findByStartingLevelLessThanEqualAndIsActiveTrue(level: CEFRLevel): List<CourseTemplateEntity>
}

// ❌ REMOVED: UserCourseEnrollmentRepository (enrollment logic now in ChatSessionRepository)

// Extended ChatSession Repository - Add course-related queries
interface ChatSessionRepository : JpaRepository<ChatSessionEntity, UUID> {
    // ... existing methods ...

    // NEW: Find sessions with course metadata
    fun findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId: UUID): List<ChatSessionEntity>
    fun findByUserIdAndCourseTemplateIdAndIsActiveTrue(userId: UUID, courseTemplateId: UUID): ChatSessionEntity?
    fun findByUserIdAndTutorProfileIdAndIsActiveTrue(userId: UUID, tutorProfileId: UUID): List<ChatSessionEntity>
}
```

### Service Layer

```kotlin
// Localization Service - Handle multilingual content with AI translation fallback
interface LocalizationService {
    fun getLocalizedText(
        jsonText: String,
        languageCode: String,
        englishFallback: String,
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

// Catalog Service - Browse available options (SIMPLIFIED)
interface CatalogService {
    // Language metadata from configuration
    fun getAvailableLanguages(): List<LanguageMetadata>
    fun getLanguageByCode(code: String): LanguageMetadata?

    // Course browsing
    fun getCoursesForLanguage(languageCode: String, userLevel: CEFRLevel? = null): List<CourseTemplate>
    fun getCourseById(courseId: UUID): CourseTemplate?
    fun getCoursesByCategory(category: CourseCategory): List<CourseTemplate>
    fun searchCourses(query: String, languageCode: String? = null): List<CourseTemplate>

    // Tutor browsing
    fun getTutorsForLanguage(targetLanguageCode: String): List<TutorProfile>
    fun getTutorById(tutorId: UUID): TutorProfile?
    fun getTutorsForCourse(courseTemplateId: UUID): List<TutorProfile>

    // Localization helpers (returns DTO directly, no intermediate wrapper)
    fun localizeCourse(course: CourseTemplate, sourceLanguageCode: String): CourseResponse
    fun localizeTutor(tutor: TutorProfile, sourceLanguageCode: String): TutorResponse
}

// ❌ REMOVED: LocalizedTutorProfile wrapper (use DTOs directly)
// ❌ REMOVED: EnrollmentService (logic moved to ChatService)
// ❌ REMOVED: SessionFactoryService (logic moved to ChatService)

// Extended ChatService - Add course-based session creation
interface ChatService {
    // ... existing methods ...

    // NEW: Create session from course/tutor selection
    fun createSessionFromCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String,
        customName: String? = null
    ): SessionResponse

    // NEW: Get user's active learning sessions
    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse>

    // NEW: Calculate session progress
    fun getSessionProgress(sessionId: UUID): SessionProgressResponse
}

// Session Progress Response (calculated on-demand)
data class SessionProgressResponse(
    val sessionId: UUID,
    val messageCount: Int,
    val vocabularyCount: Int,
    val daysActive: Int,
    val lastAccessedAt: Instant
)
```

### REST API (SIMPLIFIED)

```kotlin
// User Language Controller - Manage user's language proficiency
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

// Catalog Controller - Browse languages, courses, and tutors
@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController(
    private val catalogService: CatalogService,
    private val authorizationService: AuthorizationService
) {
    @GetMapping("/languages")
    fun getLanguages(
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<LanguageResponse>

    @GetMapping("/languages/{code}/courses")
    fun getCoursesForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String,
        @RequestParam(required = false) userLevel: CEFRLevel?
    ): List<CourseResponse>

    @GetMapping("/languages/{code}/tutors")
    fun getTutorsForLanguage(
        @PathVariable code: String,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): List<TutorResponse>

    @GetMapping("/courses/{id}")
    fun getCourseDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): CourseDetailResponse

    @GetMapping("/tutors/{id}")
    fun getTutorDetails(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "en") sourceLanguage: String
    ): TutorDetailResponse
}

// ❌ REMOVED: EnrollmentController (endpoints merged into ChatController)

// Extended ChatController - Add course-based session creation
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val authorizationService: AuthorizationService
) {
    // ... existing endpoints ...

    // NEW: Create session from course/tutor selection
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

    // NEW: Get user's active learning sessions with progress
    @GetMapping("/sessions/active")
    fun getActiveLearningSessions(
        @RequestParam userId: UUID
    ): List<SessionWithProgressResponse> {
        authorizationService.requireAccessToUser(userId)
        return chatService.getActiveLearningSessions(userId)
    }

    // NEW: Get session progress
    @GetMapping("/sessions/{sessionId}/progress")
    fun getSessionProgress(
        @PathVariable sessionId: UUID
    ): SessionProgressResponse {
        return chatService.getSessionProgress(sessionId)
    }
}
```

## CLI Experience Design (SIMPLIFIED)

### New Commands

```
/languages              List available languages
/courses [lang]         List courses (optionally filtered by language)
/tutors [lang]          List tutors (optionally filtered by language)
/start-course           Interactive course selection wizard
/sessions               List your active learning sessions
/continue [id]          Continue a session
/progress               Show progress for current session
```

**Changes from original design:**
- `/enroll` → `/start-course` (clearer intent)
- `/enrollments` → `/sessions` (matches domain model)
- No separate "enrollment" concept in UX

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

✓ Created learning session: "Conversational Spanish with Maria"
  Course: Conversational Spanish
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
📚 Your Learning Sessions:
  1. 🇪🇸 Conversational Spanish with Maria
     150 words | 25 messages | 7 days active
     Last: 2 hours ago

  2. 🇩🇪 Work German with Herr Schmidt
     30 words | 8 messages | 3 days active
     Last: 2 weeks ago

Continue which session? (1-2, or 'new'): 1

Resuming "Conversational Spanish with Maria"...
Current topic: Ordering food at restaurants

Maria: ¡Hola Eric! Ready to continue practicing?
```

### CLI Configuration Changes

Update `CliConfig.kt` to support course-based sessions:

```kotlin
@Serializable
data class CliConfig(
    // ... existing fields ...

    // Course-based session preferences
    val lastSessionId: String? = null,          // Last active learning session
    val defaultSourceLanguage: String = "en",   // User's primary language

    // Deprecated (migrate to course-based sessions)
    @Deprecated("Use course-based sessions instead")
    val defaultTutor: String = "Maria",
    // ... other deprecated fields
)
```

## Implementation Plan (SIMPLIFIED - 4-5 Weeks)

### Phase 1: Core Domain & Data Layer (Week 1)

**Tasks:**
1. ✅ Create domain models (UserLanguageProficiency, TutorProfile, CourseTemplate)
2. ✅ Extend ChatSessionEntity with course fields (courseTemplateId, tutorProfileId, customName)
3. ✅ Create JPA entities with proper relationships
4. ✅ Create repositories with query methods
5. ✅ Add database migration for new columns and tables
6. ✅ Write unit tests for domain models

**Deliverables:**
- Domain models in `core/model/catalog/`
- Extended ChatSessionEntity in `chat/domain/`
- JPA entities in `catalog/domain/` and `user/domain/`
- Repositories in `catalog/repository/` and `user/repository/`
- SQL migration scripts

**Removed from original plan:**
- ❌ UserCourseEnrollment entity
- ❌ LanguagePack entity

### Phase 2: Service Layer (Week 2)

**Tasks:**
1. ✅ Implement `UserLanguageService` with proficiency management
2. ✅ Implement `CatalogService` with business logic (simplified)
3. ✅ Create `LanguageConfig.kt` with language metadata configuration
4. ✅ Extend `ChatService` with course-based session creation
5. ✅ Implement `LocalizationService` with AI translation fallback
6. ✅ Implement `TranslationService` (OpenAI-powered)
7. ✅ Add validation and error handling
8. ✅ Write unit tests with MockK

**Deliverables:**
- Service implementations in `catalog/service/`, `user/service/`, `language/service/`
- Test coverage >80%

**Removed from original plan:**
- ❌ EnrollmentService
- ❌ SessionFactoryService
- ❌ LocalizedTutorProfile wrapper

### Phase 3: REST API (Week 3)

**Tasks:**
1. ✅ Create DTOs for user language, catalog, and session endpoints
2. ✅ Implement `UserLanguageController` with endpoints
3. ✅ Implement `CatalogController` with endpoints
4. ✅ Extend `ChatController` with course-based session endpoints
5. ✅ Add authorization checks (users can only manage their own data)
6. ✅ Write controller tests with MockMvc
7. ✅ Add HTTP client test examples

**Deliverables:**
- Controllers in `catalog/controller/` and `user/controller/`
- Extended ChatController in `chat/controller/`
- DTOs in `catalog/dto/`, `user/dto/`, `chat/dto/`
- HTTP test examples in `src/test/http/`

**Removed from original plan:**
- ❌ EnrollmentController

### Phase 4: Seed Data & Content (Week 4)

**Tasks:**
1. ✅ Create language metadata configuration (Spanish, French, German, Japanese)
2. ✅ Create seed data for TutorProfiles (3-5 diverse personalities per language)
3. ✅ Create seed data for CourseTemplates (2-3 per language)
4. ✅ Add data initialization service (runs on startup in dev mode)
5. ✅ Document seed data structure for future additions

**Deliverables:**
- LanguageConfig.kt with language metadata
- Seed data JSON/SQL files for tutors and courses
- Data initialization service
- Content guidelines documentation

**Reduced scope from original plan:**
- Language metadata in configuration (not database)
- Fewer tutors per language (3-5 instead of 5-10)
- Fewer courses per language (2-3 instead of 3-5)

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

### Phase 5: CLI Integration & Documentation (Week 5)

**Tasks:**
1. ✅ Add language profile onboarding flow (first-time users)
2. ✅ Add new CLI commands (/languages, /courses, /tutors, /start-course, /sessions)
3. ✅ Implement smart defaults based on user language profile
4. ✅ Implement interactive course selection wizard
5. ✅ Update startup flow to show active sessions with progress
6. ✅ Update CliConfig to support course-based sessions
7. ✅ Add progress calculation display in CLI
8. ✅ Update CLI help text
9. ✅ Update README.md with catalog features
10. ✅ Update CLAUDE.md with new architecture

**Deliverables:**
- Updated `AiTutorCli.kt` with new commands and onboarding
- Updated `HttpApiClient.kt` with catalog/language/session endpoints
- Updated documentation (README.md, CLAUDE.md)
- Migration guide for CLI config

**Merged from Phase 6:**
- Documentation updates merged into Phase 5 for efficiency

### Phase 6: Testing & Release (Optional - Week 6)

**Tasks:**
1. ✅ Integration testing with real database
2. ✅ End-to-end CLI testing
3. ✅ Performance testing (catalog queries, progress calculation)
4. ✅ User acceptance testing
5. ✅ Bug fixes and polish
6. ✅ Release notes

**Deliverables:**
- Test reports
- Bug fixes
- v1.0 release with simplified catalog system

**Removed from original plan:**
- Phase 7 is now Phase 6 (1 week saved)

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

1. ~~**Content Creation**: Who creates initial course content? Do we need a CMS?~~ **RESOLVED**: No initial content. In the future add an import functionality (e.g. from yml file) 
2. ~~**Multi-Language UI**: Should course descriptions be multilingual?~~ **RESOLVED**: Yes, all content stored as JSON maps with AI translation fallback
3. ~~**Enrollment Model**: Should we have separate enrollment entity?~~ **RESOLVED**: No, course metadata stored directly in ChatSessionEntity
4. ~~**Progress Tracking**: Should we denormalize progress fields?~~ **RESOLVED**: No, calculate on-demand in MVP for accuracy
5. ~~**Language Metadata Storage**: Should LanguagePack be a database entity?~~ **RESOLVED**: No, use configuration file for static data
6. **Course Updates**: How do we handle updates to course templates for existing sessions?
7. **Free vs. Paid**: Should some courses be premium/paid?
8. **Progress Syncing**: How do we sync progress between CLI and future web UI?
9. ~~**Offline Mode**: Should CLI support offline course access?~~ **RESOLVED**: No
10. **Regional Variants**: Do we need separate tutors for regional variants (e.g., Spain vs. Latin America Spanish)?
11. **Tutor Voices**: Should tutors have AI-generated voice profiles for future audio features?
12. **Translation Coverage**: Which source languages should we support initially? (English, Spanish, German, French, Japanese?)
13. ~~**Translation Quality**: Use AI translation for seed data or require manual translations?~~ **RESOLVED**: Use AI translation on-demand with optional caching
14. ~~**Fallback Strategy**: If translation missing, show English or target language?~~ **RESOLVED**: AI translates from English on-the-fly

## References

- Current codebase: `/src/main/kotlin/ch/obermuhlner/aitutor/`
- CLI client: `/src/main/kotlin/ch/obermuhlner/aitutor/cli/`
- Session management: `/src/main/kotlin/ch/obermuhlner/aitutor/chat/`
- Existing domain models: `/src/main/kotlin/ch/obermuhlner/aitutor/core/model/`

### Design Choice Documents
- **Architecture Review**: `task-0002-architecture-review.md` - Comprehensive analysis identifying overengineering and simplification opportunities
- **Localization Strategy**: `task-0002-design-choice-localization.md` - Dual storage (English for AI + JSON for UI) with AI translation fallback
- **User Language Proficiency**: `task-0002-design-choice-user-languages.md` - Track user's known languages with CEFR levels for personalization

## Summary of Changes from Original Design

### Removed Entities (2)
- ❌ **UserCourseEnrollment** → Merged into ChatSessionEntity
- ❌ **LanguagePack** → Moved to configuration

### Removed Services (2)
- ❌ **SessionFactoryService** → Logic moved to ChatService
- ❌ **EnrollmentService** → Logic moved to ChatService

### Removed Controllers (1)
- ❌ **EnrollmentController** → Endpoints moved to ChatController

### Removed Domain Objects (1)
- ❌ **LocalizedTutorProfile** → Use DTOs directly

### Impact
- **Code Reduction**: ~40% less code (~1,500 lines instead of ~2,500)
- **Timeline**: 5-6 weeks instead of 8 weeks
- **Entities**: 3 new entities instead of 5
- **Services**: 4 new services instead of 6
- **Controllers**: 2 new controllers instead of 3
- **Complexity**: Simpler mental model (3 concepts instead of 5)
- **Functionality**: 100% feature parity maintained
