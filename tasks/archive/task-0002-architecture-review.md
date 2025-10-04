# Task 0002: Architecture Review

**Date**: 2025-01-17
**Reviewer**: Claude Code
**Document**: task-0002-requirements.md
**Status**: Critical Analysis

---

## Executive Summary

The proposed catalog-based tutor/language/course management system is **well-designed and architecturally sound**, but contains **significant overengineering** in several areas. The design follows Spring Boot best practices and has clear separation of concerns, but introduces unnecessary complexity that could be simplified by 30-40% while maintaining all core functionality.

**Recommendation**: **SIMPLIFY** - Reduce from 5 new entities to 3, eliminate SessionFactoryService, and simplify the enrollment model.

---

## 1. Architecture Quality Assessment

### ✅ Strengths

1. **Clean Layering**
   - Clear separation: Controller → Service → Repository → Entity
   - Proper use of DTOs to decouple API from domain
   - Consistent naming conventions

2. **Spring Boot Best Practices**
   - JPA entities with proper annotations
   - Repository pattern with custom query methods
   - Service interfaces for abstraction
   - REST API follows RESTful conventions

3. **Localization Strategy**
   - Dual storage (English + JSON) is pragmatic
   - AI translation fallback is innovative and cost-effective
   - Avoids premature manual translation work

4. **User Language Proficiency**
   - Smart defaults reduce friction
   - Supports multi-language learners
   - Good foundation for personalization

5. **Domain Model**
   - Well-thought-out enums (LanguageProficiencyType, TutorPersonality, CourseCategory)
   - Proper use of UUIDs for IDs
   - Timestamp tracking (createdAt, updatedAt)

### ❌ Architectural Concerns

1. **Entity Explosion**
   - 5 new entities: UserLanguageProficiency, LanguagePack, TutorProfile, CourseTemplate, UserCourseEnrollment
   - **Impact**: Increased complexity, more migrations, more tests, more maintenance

2. **Service Proliferation**
   - 6 new services: UserLanguageService, CatalogService, EnrollmentService, SessionFactoryService, LocalizationService, TranslationService
   - **Impact**: Harder to understand system flow, more dependencies to inject

3. **Unclear Relationship: Enrollment vs Session**
   - UserCourseEnrollment seems to be a "meta-session" or "session template"
   - Relationship to ChatSessionEntity is unclear
   - **Question**: Does enrollment replace sessions or wrap them?

4. **Progress Tracking Ambiguity**
   - `UserCourseEnrollment.progressPercentage` - How is this calculated?
   - `completedTopics` - How does this relate to actual conversation topics?
   - `vocabularyCount` - Is this a denormalized count from VocabularyItemEntity?
   - `sessionCount` - Redundant if we have foreign key relationship?

---

## 2. Overengineering Analysis

### 🔴 Critical: UserCourseEnrollment - Do We Need This?

**Current Design:**
```kotlin
data class UserCourseEnrollment(
    val id: UUID,
    val userId: UUID,
    val courseTemplateId: UUID,    // References CourseTemplate
    val tutorProfileId: UUID,      // References TutorProfile
    val languageCode: String,
    val sourceLanguageCode: String,
    val customName: String?,
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

**Problem:** This looks like a "session with metadata" rather than an "enrollment".

**Alternative 1: Extend ChatSessionEntity**

Why not just extend the existing `ChatSessionEntity` with course-related fields?

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

    @Column(name = "enrolled_at")
    var enrolledAt: Instant? = null,

    @Column(name = "streak_days")
    var streakDays: Int = 0,

    @Column(name = "is_active")
    var isActive: Boolean = true
)
```

**Benefits:**
- ✅ Eliminates UserCourseEnrollment entity entirely
- ✅ One less table, one less repository, one less service
- ✅ Clear 1:1 relationship between enrollment and session
- ✅ Backward compatible (new fields are nullable)
- ✅ Progress tracking uses existing ChatMessage/Vocabulary relationships
- ✅ Simpler queries (no JOINs between enrollment and session)

**Progress Calculation:**
```kotlin
// Progress is calculated on-the-fly from existing data
fun calculateProgress(sessionId: UUID): Int {
    val session = sessionRepository.findById(sessionId)
    val course = courseRepository.findById(session.courseTemplateId)
    val messagesCount = messageRepository.countBySessionId(sessionId)
    val vocabularyCount = vocabularyRepository.countByUserIdAndLanguageCode(
        session.userId,
        session.targetLanguageCode
    )

    // Progress based on messages and vocabulary learned
    return calculateProgressScore(messagesCount, vocabularyCount, course.estimatedWeeks)
}
```

**Alternative 2: Keep Enrollment BUT Link to Sessions**

If we keep UserCourseEnrollment, clarify relationship:

```kotlin
data class UserCourseEnrollment(
    val id: UUID,
    val userId: UUID,
    val courseTemplateId: UUID,
    val tutorProfileId: UUID,
    // ...
)

@Entity
class ChatSessionEntity(
    // ... existing fields ...

    @Column(name = "enrollment_id")
    var enrollmentId: UUID? = null  // Link to enrollment
)
```

But this still begs the question: **Why have both?**

---

### 🟡 Medium: SessionFactoryService - Unnecessary Abstraction

**Current Design:**
```kotlin
interface SessionFactoryService {
    fun createSessionFromEnrollment(enrollmentId: UUID): SessionResponse
    fun getRecommendedTopic(enrollmentId: UUID): String?
}
```

**Problem:** This is a single-use service that just calls `ChatService.createSession()` with enrollment data.

**Simpler Alternative:**

Move this logic directly into `ChatService` or `EnrollmentService`:

```kotlin
@Service
class ChatService(/* ... */) {

    fun createSessionFromEnrollment(enrollmentId: UUID): SessionResponse {
        val enrollment = enrollmentService.getEnrollment(enrollmentId)
        val tutor = catalogService.getTutorProfile(enrollment.tutorProfileId)
        val course = catalogService.getCourse(enrollment.courseTemplateId)

        return createSession(
            userId = enrollment.userId,
            tutorName = tutor.name,
            tutorPersona = tutor.personaEnglish,
            tutorDomain = tutor.domainEnglish,
            sourceLanguageCode = enrollment.sourceLanguageCode,
            targetLanguageCode = enrollment.languageCode,
            conversationPhase = course.defaultPhase,
            estimatedCEFRLevel = enrollment.currentLevel
        )
    }
}
```

**Benefits:**
- ✅ One less service to maintain
- ✅ Simpler dependency graph
- ✅ Logic lives where it's used

---

### 🟡 Medium: LocalizedTutorProfile - Unnecessary Wrapper

**Current Design:**
```kotlin
data class LocalizedTutorProfile(
    val id: UUID,
    val name: String,
    val emoji: String,
    val persona: String,              // Resolved from JSON
    val domain: String,               // Resolved from JSON
    val personality: TutorPersonality,
    val description: String,          // Resolved from JSON
    val targetLanguageCode: String,
    val culturalBackground: String?,
    val isActive: Boolean,
    val displayOrder: Int
)
```

**Problem:** This is just a DTO. Why not use `TutorProfileResponse` directly?

**Simpler Alternative:**

```kotlin
// In CatalogController
@GetMapping("/tutors/{id}")
fun getTutorDetails(
    @PathVariable id: UUID,
    @RequestParam(defaultValue = "en") sourceLanguage: String
): TutorProfileResponse {  // Use DTO directly, no intermediate wrapper
    val tutor = catalogService.getTutorProfile(id)

    return TutorProfileResponse(
        id = tutor.id,
        name = tutor.name,
        emoji = tutor.emoji,
        persona = localizationService.getLocalizedText(
            tutor.personaJson, sourceLanguage, tutor.personaEnglish
        ),
        // ... etc
    )
}
```

**Benefits:**
- ✅ Eliminate intermediate domain model
- ✅ Convert directly from Entity → DTO
- ✅ Less object creation

---

### 🟢 Minor: LanguageProficiencyType - Too Many States?

**Current Design:**
```kotlin
enum class LanguageProficiencyType {
    Native,      // Native speaker
    Learning,    // Currently learning
    Studied,     // Previously studied, not active
    Interested   // Wants to learn
}
```

**Concern:** Do we need all 4 states?

**Question:** What's the difference between "Interested" and "Learning at A1"?

**Simpler Alternative:**
```kotlin
enum class LanguageProficiencyType {
    Native,      // Native speaker (no CEFR level)
    Learning,    // Currently learning or studied before (has CEFR level)
}
```

Use `cefrLevel: null` to indicate "interested but not started".

**But:** Current design is fine if you want to track language learning journey explicitly.

---

### 🟢 Minor: CourseTemplate.topicSequence - String or Localized?

**Current Design:**
```kotlin
data class CourseTemplate(
    // ...
    val topicSequence: List<String>?,  // Suggested topic progression
    // ...
)
```

**Question:** Are topics language-neutral keys ("travel", "food") or localized text ("Viajar", "Comida")?

**Recommendation:** Use language-neutral topic keys and localize in UI:

```kotlin
// Store as keys
topicSequence = ["greetings", "daily_routines", "food_ordering", "travel"]

// Localize in UI
val localizedTopics = topicSequence.map { topicKey ->
    topicLocalizationService.getLocalizedTopic(topicKey, sourceLanguage)
}
```

**But:** This is low-priority - can decide during implementation.

---

## 3. Developer Experience (DX) Analysis

### ✅ Good DX

1. **Clear Naming**
   - Services, repositories, controllers follow conventions
   - Enum names are self-explanatory

2. **Type Safety**
   - Strong typing with Kotlin data classes
   - Enums for constrained values

3. **REST API Design**
   - Follows REST conventions
   - Predictable endpoint structure

### ❌ Poor DX

1. **Too Many Concepts**
   - Developer must understand: LanguagePack, TutorProfile, CourseTemplate, UserCourseEnrollment, UserLanguageProficiency
   - **Cognitive load**: 5 new entities + relationships

2. **Unclear Flow**
   - How do you create a session?
     - Option A: Create enrollment → Create session from enrollment
     - Option B: Create session directly (existing flow)
   - **Confusion**: Two ways to do the same thing?

3. **Testing Complexity**
   - 5 new entities = 5 new test data factories
   - 6 new services = complex mocking in tests
   - More integration test setup

4. **Migration Path Unclear**
   - What happens to existing ChatSessionEntity records?
   - Do they need `enrollmentId` backfilled?
   - Or are they "legacy sessions"?

---

## 4. User Experience (UX) Analysis

### ✅ Good UX

1. **Onboarding**
   - Language profile setup is clear
   - Smart defaults reduce friction

2. **Course Discovery**
   - Browse by language, category, tutor
   - Clear metadata (level, duration, description)

3. **Personalization**
   - Courses filtered by user's level
   - Source language auto-detected

4. **Quick Resume**
   - See enrolled courses on startup
   - One-click continue

### ❌ UX Concerns

1. **Enrollment vs Session Confusion**
   - User mental model: "I want to chat with Maria about Spanish"
   - System model: "Create enrollment → Create session from enrollment"
   - **Mismatch**: Extra step adds friction

2. **Progress Tracking Opacity**
   - "Progress: 20%" - How is this calculated?
   - Not transparent to user
   - **Risk**: Feels arbitrary if not explained

3. **Course Completion**
   - What happens when user completes a course?
   - Can they re-enroll?
   - Does enrollment stay active forever?

4. **Multiple Enrollments in Same Language**
   - User learning Spanish:
     - Enrollment 1: "Conversational Spanish" (with Maria)
     - Enrollment 2: "Spanish Grammar" (with Professor Rodriguez)
   - **Confusion**: Which enrollment to resume?
   - **Risk**: Progress fragmentation

---

## 5. Missing Design Choices

### 🔴 Critical: Enrollment-to-Session Relationship

**Decision Needed:** How do enrollments relate to sessions?

**Option A: Enrollment = Long-lived, Sessions = Transient**
```
UserCourseEnrollment (persistent)
    ├─ ChatSession 1 (yesterday)
    ├─ ChatSession 2 (today)
    └─ ChatSession 3 (tomorrow)
```

- Enrollment is the "learning journey"
- Sessions are individual conversations within that journey
- `ChatSessionEntity.enrollmentId` links them

**Option B: Enrollment = Session (1:1)**
```
UserCourseEnrollment == ChatSession
```

- One enrollment = one continuous conversation
- No separate ChatSessionEntity (or it's denormalized)

**Option C: No Enrollment Entity (Simplest)**
```
ChatSession (with course metadata)
```

- `ChatSessionEntity.courseTemplateId` and `tutorProfileId` are enough
- No separate enrollment entity
- Progress calculated on-the-fly from sessions

**Recommendation:** **Option C** or **Option A with clear documentation**.

---

### 🔴 Critical: Progress Calculation Strategy

**Decision Needed:** How is `UserCourseEnrollment.progressPercentage` calculated?

**Option 1: Message-Based**
```kotlin
progress = (messageCount / estimatedTotalMessages) * 100
```
- Simple but arbitrary (what's "total messages"?)

**Option 2: Topic-Based**
```kotlin
progress = (completedTopics.size / course.topicSequence.size) * 100
```
- Clear but requires topic completion tracking

**Option 3: Time-Based**
```kotlin
val weeksPassed = Duration.between(enrolledAt, now).toDays() / 7
progress = min((weeksPassed / course.estimatedWeeks) * 100, 100)
```
- Automatic but doesn't reflect actual learning

**Option 4: Hybrid**
```kotlin
val topicProgress = completedTopics.size / totalTopics
val vocabularyProgress = vocabularyCount / targetVocabularyCount
val timeProgress = weeksPassed / estimatedWeeks
progress = (topicProgress * 0.4 + vocabularyProgress * 0.3 + timeProgress * 0.3) * 100
```
- Most accurate but complex

**Recommendation:** **Option 2** (topic-based) for transparency, with fallback to message count if topics not defined.

---

### 🟡 Medium: Course Completion and Re-enrollment

**Decision Needed:** What happens when user finishes a course?

1. Does enrollment stay `isActive = true` forever?
2. Can user re-enroll in the same course?
3. Is there a "completed" status?
4. Does progress reset or preserve history?

**Recommendation:**
```kotlin
enum class EnrollmentStatus {
    Active,      // Currently enrolled
    Completed,   // Finished the course
    Paused,      // User paused
    Abandoned    // User stopped attending
}

data class UserCourseEnrollment(
    // ...
    val status: EnrollmentStatus = EnrollmentStatus.Active,
    val completedAt: Instant? = null
)
```

---

### 🟡 Medium: Vocabulary Denormalization

**Decision Needed:** Is `UserCourseEnrollment.vocabularyCount` denormalized?

If yes:
- ✅ Fast queries (no COUNT aggregation)
- ❌ Stale data risk (must update on every vocabulary insert)
- ❌ Extra complexity in VocabularyService

If no:
- ✅ Always accurate (calculate on-the-fly)
- ❌ Slower queries (requires JOIN + COUNT)

**Recommendation:** **Don't denormalize in MVP**. Calculate on-demand:
```kotlin
fun getEnrollmentWithStats(enrollmentId: UUID): EnrollmentDetailResponse {
    val enrollment = enrollmentRepository.findById(enrollmentId)
    val vocabularyCount = vocabularyRepository.countByUserIdAndLanguageCode(
        enrollment.userId,
        enrollment.languageCode
    )
    val sessionCount = sessionRepository.countByEnrollmentId(enrollmentId)

    return EnrollmentDetailResponse(
        // ...
        vocabularyCount = vocabularyCount,
        sessionCount = sessionCount
    )
}
```

Add denormalization only if performance becomes an issue (unlikely with <100k users).

---

### 🟢 Minor: Custom Session Names

**Decision Needed:** Why can users customize enrollment names?

```kotlin
data class UserCourseEnrollment(
    // ...
    val customName: String?,  // User can rename enrollment
    // ...
)
```

**Use Case:** User has 2 enrollments in same course with different tutors:
- "Spanish with Maria"
- "Spanish with Carlos"

**Recommendation:** Keep this feature - it's simple and adds flexibility.

---

### 🟢 Minor: Translation Caching Strategy

**Decision Needed:** Should AI translations be cached back to database?

**Option A: Cache in Database (Persistent)**
```kotlin
// After AI translation, update JSON field
tutorProfile.personaJson = addTranslation(personaJson, languageCode, translatedText)
repository.save(tutorProfile)
```

**Option B: Cache in Memory (Transient)**
```kotlin
@Cacheable("translations")
fun translate(text: String, from: String, to: String): String {
    return openAITranslationService.translate(text, from, to)
}
```

**Option C: No Caching (Always Translate)**
```kotlin
// Translate every time
// Simple but expensive
```

**Recommendation:** **Option B** (memory cache) for MVP, **Option A** (database cache) for production scale.

---

## 6. Simplification Recommendations

### Recommendation 1: Eliminate UserCourseEnrollment

**Rationale:** It's a "session with metadata". Just extend ChatSessionEntity.

**Changes:**
```kotlin
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // ... existing fields ...

    @Column(name = "course_template_id")
    var courseTemplateId: UUID? = null,

    @Column(name = "tutor_profile_id")
    var tutorProfileId: UUID? = null,

    @Column(name = "custom_session_name")
    var customName: String? = null
)
```

**Impact:**
- ✅ Removes 1 entity
- ✅ Removes EnrollmentService
- ✅ Removes SessionFactoryService
- ✅ Removes EnrollmentController
- ✅ Simpler data model
- ✅ Less code to test

**Trade-offs:**
- ❌ ChatSessionEntity becomes slightly larger
- ❌ Less separation of concerns (but still acceptable)

---

### Recommendation 2: Merge CatalogService and SessionFactoryService

**Rationale:** SessionFactoryService is a single-use abstraction.

**Changes:**
```kotlin
// Move session creation into ChatService
@Service
class ChatService(
    private val catalogService: CatalogService,
    // ...
) {
    fun createSessionFromCourse(
        userId: UUID,
        courseTemplateId: UUID,
        tutorProfileId: UUID,
        sourceLanguageCode: String
    ): SessionResponse {
        val course = catalogService.getCourse(courseTemplateId)
        val tutor = catalogService.getTutorProfile(tutorProfileId)

        return createSession(/* ... */)
    }
}
```

**Impact:**
- ✅ Removes SessionFactoryService
- ✅ Simpler service layer
- ✅ Logic lives in ChatService where it belongs

---

### Recommendation 3: Simplify Progress Tracking (MVP)

**Rationale:** Don't calculate complex progress scores initially.

**Changes:**
```kotlin
// In ChatSession (no denormalized fields)
@Entity
class ChatSessionEntity(
    // ... existing fields ...
    // NO: progressPercentage, vocabularyCount, sessionCount
)

// Calculate on-demand in service
fun getSessionProgress(sessionId: UUID): SessionProgressResponse {
    val session = sessionRepository.findById(sessionId)
    val messageCount = messageRepository.countBySessionId(sessionId)
    val vocabularyCount = vocabularyRepository.countByUserIdAndLanguageCode(
        session.userId,
        session.targetLanguageCode
    )

    return SessionProgressResponse(
        messageCount = messageCount,
        vocabularyCount = vocabularyCount,
        daysActive = calculateDaysActive(session.createdAt)
    )
}
```

**Impact:**
- ✅ No denormalized fields
- ✅ Always accurate data
- ✅ Simpler entity model

---

### Recommendation 4: LanguagePack - Do We Need This?

**Rationale:** Language metadata can be hardcoded or configuration-based.

**Alternative:**
```kotlin
// In application.yml or LanguageConfig.kt
val supportedLanguages = mapOf(
    "es" to LanguageMetadata(
        code = "es",
        nameJson = """{"en": "Spanish", "es": "Español", "de": "Spanisch"}""",
        flagEmoji = "🇪🇸",
        nativeName = "Español",
        difficulty = Difficulty.Easy
    ),
    "fr" to LanguageMetadata(/* ... */),
    // ...
)
```

**Benefits:**
- ✅ No database table for relatively static data
- ✅ Faster queries (in-memory)
- ✅ Easier to version control
- ❌ Less dynamic (requires code change to add language)

**Recommendation:** Keep LanguagePack entity if you want non-developers to add languages. Otherwise, use configuration.

---

## 7. Simplified Architecture Proposal

### Before (Current Design)

**New Entities:** 5
- UserLanguageProficiency
- LanguagePack
- TutorProfile
- CourseTemplate
- UserCourseEnrollment

**New Services:** 6
- UserLanguageService
- CatalogService
- EnrollmentService
- SessionFactoryService
- LocalizationService
- TranslationService

**New Controllers:** 3
- UserLanguageController
- CatalogController
- EnrollmentController

**Total New Code:** ~2,500 lines

---

### After (Simplified Design)

**New Entities:** 3
- UserLanguageProficiency
- TutorProfile
- CourseTemplate

**Extended Entities:** 1
- ChatSessionEntity (add course fields)

**New Services:** 4
- UserLanguageService
- CatalogService
- LocalizationService
- TranslationService

**New Controllers:** 2
- UserLanguageController
- CatalogController

**Updated Controllers:** 1
- ChatController (add course-based session creation)

**Removed:**
- LanguagePack entity (use configuration)
- UserCourseEnrollment entity (merged into ChatSessionEntity)
- EnrollmentService (logic moved to ChatService)
- SessionFactoryService (logic moved to ChatService)
- EnrollmentController (endpoints moved to ChatController)
- LocalizedTutorProfile (use DTO directly)

**Total New Code:** ~1,500 lines (40% reduction)

---

## 8. Final Recommendations

### 🔴 Must Fix (Architectural)

1. **Clarify Enrollment-to-Session Relationship**
   - Choose Option A (1:N) or Option C (No enrollment entity)
   - Document clearly in requirements

2. **Define Progress Calculation Strategy**
   - Use topic-based or message-based (pick one)
   - Don't denormalize in MVP

3. **Merge or Remove SessionFactoryService**
   - Move logic to ChatService
   - Eliminate unnecessary abstraction

### 🟡 Should Fix (DX/UX)

1. **Simplify Progress Tracking**
   - Remove denormalized fields (progressPercentage, vocabularyCount, sessionCount)
   - Calculate on-demand

2. **Remove LocalizedTutorProfile Wrapper**
   - Use TutorProfileResponse directly
   - One less object to maintain

3. **Consider Removing LanguagePack Entity**
   - Use configuration for relatively static language metadata
   - Only keep if non-developers need to add languages

### 🟢 Nice to Have (Optimization)

1. **Reduce LanguageProficiencyType States**
   - Merge "Interested" into "Learning with no CEFR level"
   - Keep if you want explicit journey tracking

2. **Add EnrollmentStatus Enum**
   - Support Active, Completed, Paused, Abandoned
   - Better lifecycle management

3. **Clarify Topic Localization**
   - Use language-neutral keys or localized strings?
   - Document in requirements

---

## 9. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Entity explosion** | HIGH | Reduce to 3 entities, extend ChatSessionEntity |
| **Service proliferation** | MEDIUM | Merge SessionFactoryService into ChatService |
| **Unclear enrollment model** | HIGH | Clarify relationship in design choice doc |
| **Progress tracking complexity** | MEDIUM | Simplify to message/topic-based calculation |
| **Testing complexity** | MEDIUM | Fewer entities = simpler test setup |
| **Migration risk** | MEDIUM | Document migration path for existing data |
| **Translation costs** | LOW | Memory caching mitigates AI translation costs |

---

## 10. Action Items

### Before Implementation

1. **Decision Required**: Choose enrollment model (1:N vs no enrollment)
2. **Decision Required**: Define progress calculation strategy
3. **Refactor Requirements**: Update task-0002-requirements.md based on simplifications
4. **Create Design Choice Doc**: Document enrollment-to-session relationship

### During Implementation

1. **Start Simple**: Implement core entities first (TutorProfile, CourseTemplate, UserLanguageProficiency)
2. **Defer Complexity**: Skip denormalized progress fields in MVP
3. **Test Early**: Create test data factories early to validate model
4. **Document Flow**: Add sequence diagrams for "Create Session from Course" flow

### After Implementation

1. **Performance Testing**: Measure progress calculation query times
2. **UX Testing**: Validate course selection flow with real users
3. **Refactoring**: Add denormalization only if performance requires it
4. **Scale Testing**: Test with 1000+ courses/tutors to validate catalog queries

---

## Conclusion

The proposed architecture is **solid but overengineered**. By simplifying from 5 entities to 3, eliminating SessionFactoryService, and deferring progress tracking denormalization, we can achieve:

- ✅ **40% less code** (~1,000 lines saved)
- ✅ **Simpler mental model** (3 concepts instead of 5)
- ✅ **Faster implementation** (4-5 weeks instead of 8 weeks)
- ✅ **Easier testing** (fewer entities, fewer mocks)
- ✅ **Same functionality** (all features preserved)

**Recommended Next Step:** Create a design choice document for "Enrollment vs Session Relationship" and update requirements accordingly.