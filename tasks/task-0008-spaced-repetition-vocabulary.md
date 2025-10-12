# Task 0008: Spaced Repetition System (SRS) for Vocabulary

## Problem Statement

The current vocabulary tracking system captures vocabulary exposure and context but lacks a critical feature for long-term retention: **scheduled review based on the forgetting curve**. This limits the application's effectiveness as a learning tool:

1. **No review scheduling** - Vocabulary items tracked but never prompted for review
2. **Exposure count doesn't predict retention** - Seeing a word 5x doesn't mean it's mastered
3. **No forgetting curve modeling** - Memory decay over time not considered
4. **Missed pedagogical opportunity** - SRS is proven to increase retention 2-3x vs passive review
5. **No review integration** - No mechanism to surface due vocabulary in natural conversation
6. **Limited vocabulary reinforcement** - Current system only tracks, doesn't actively reinforce

## Part 1: Analysis

### Current Architecture

#### Vocabulary Tracking (VocabularyItemEntity.kt)
```kotlin
@Entity
@Table(name = "vocabulary_items")
class VocabularyItemEntity(
    val id: UUID,
    val userId: UUID,
    val lang: String,
    val lemma: String,
    var exposures: Int = 0,          // Simple counter, no scheduling
    var lastSeenAt: Instant,         // Timestamp but not used for reviews
    var conceptName: String? = null,
    val createdAt: Instant?,
    var updatedAt: Instant?
)
```

**Gaps:**
- No `nextReviewAt` field for scheduling
- No `interval` tracking for spacing calculations
- No `easinessFactor` for personalized difficulty
- No `repetitions` count separate from exposures

#### Current Flow (VocabularyService.kt:22-74)
1. LLM identifies new vocabulary in user's message
2. VocabularyService creates or updates VocabularyItemEntity
3. Exposure count incremented, lastSeenAt updated
4. Context saved to VocabularyContextEntity
5. **No review scheduling occurs**

### Pedagogical Foundation

#### Spaced Repetition Research
**Ebbinghaus Forgetting Curve (1885):**
- Memory retention drops exponentially: 50% forgotten after 1 day, 70% after 1 week
- Reviewing before forgetting solidifies memory

**Spacing Effect (Cepeda et al., 2006):**
- Spaced reviews produce 2-3x better long-term retention vs massed practice
- Optimal intervals expand exponentially: 1 day → 3 days → 7 days → 16 days

**Testing Effect (Roediger & Butler, 2011):**
- Active recall (retrieval practice) strengthens memory traces
- More effective than passive re-reading

#### SRS Algorithm Options

**SuperMemo SM-2 (Wozniak, 1987):**
- Industry standard, used by Anki
- Easiness factor adjusts per-item difficulty
- Intervals: 1 day, 6 days, then multiply by easiness factor (1.3-2.5)

**FSRS (Free Spaced Repetition Scheduler, 2022):**
- Modern algorithm, >20% improvement over SM-2
- Uses retrievability and stability metrics
- Requires more complex calculations but more accurate

**Simple Fibonacci:**
- Intervals follow Fibonacci sequence: 1, 1, 2, 3, 5, 8, 13, 21 days
- No personalization but predictable and simple

### Constraints

**MUST preserve:**
- Existing VocabularyItemEntity structure (add fields, don't remove)
- Existing vocabulary tracking flow
- VocabularyContextEntity for example sentences
- REST API backward compatibility

**MUST NOT:**
- Break existing vocabulary endpoints
- Lose existing vocabulary data
- Require vocabulary re-seeding

### What Needs to Change

1. **VocabularyItemEntity** - Add SRS scheduling fields
2. **VocabularyService** - Implement review scheduling logic
3. **New VocabularyReviewService** - SRS algorithm implementation
4. **ChatSessionEntity** - Add `vocabularyReviewMode` flag for user control
5. **ConversationState** - Add `vocabularyReviewMode` field for TutorService integration
6. **ChatService/TutorService** - Integrate due vocabulary into conversation when mode enabled
7. **REST API** - Add endpoints for due vocabulary queries and review mode control
8. **Database migration** - Add new columns to vocabulary_items and chat_sessions tables

## Part 2: Solution Design

### Alternative 1: Simple Fixed Intervals (Low Complexity)

**Approach:**
- Fixed review schedule: 1 day, 3 days, 7 days, 14 days, 30 days, 90 days
- No personalization based on user performance
- Binary success: reviewed → next interval, not reviewed → reset to day 1

**Schema Changes:**
```kotlin
@Entity
class VocabularyItemEntity(
    // existing fields...
    var exposures: Int = 0,
    var lastSeenAt: Instant,

    // NEW: Simple SRS fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "review_stage")
    var reviewStage: Int = 0  // 0-5 corresponding to intervals above
)
```

**Algorithm:**
```kotlin
val intervals = listOf(1, 3, 7, 14, 30, 90) // days
fun scheduleNextReview(item: VocabularyItemEntity, success: Boolean) {
    if (success) {
        item.reviewStage = min(item.reviewStage + 1, intervals.size - 1)
    } else {
        item.reviewStage = 0 // Reset to beginning
    }
    val daysToAdd = intervals[item.reviewStage]
    item.nextReviewAt = Instant.now().plus(daysToAdd, ChronoUnit.DAYS)
}
```

**Pros:**
- Extremely simple implementation (~50 LOC)
- Predictable behavior, easy to explain
- No complex math or personalization needed
- Fast query: `SELECT * WHERE nextReviewAt <= NOW()`

**Cons:**
- Not adaptive to individual learner pace
- Ignores word difficulty variations
- Suboptimal spacing compared to research-backed algorithms
- No early graduation for easy words

**Complexity:** 1 migration, 2 new methods, 1 REST endpoint (~150 LOC)
**Risk:** Low

---

### Alternative 2: SM-2 Algorithm with Easiness Factor (Medium Complexity)

**Approach:**
- Implement SuperMemo SM-2 algorithm (Anki-style)
- Personalized easiness factor per word (1.3-2.5)
- Quality rating: 0 (forgot) to 5 (perfect recall)
- Exponentially expanding intervals with adaptive adjustment

**Schema Changes:**
```kotlin
@Entity
class VocabularyItemEntity(
    // existing fields...

    // NEW: SM-2 fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "interval_days")
    var intervalDays: Double = 0.0,  // Current interval in days

    @Column(name = "easiness_factor")
    var easinessFactor: Double = 2.5,  // Initial EF = 2.5

    @Column(name = "repetitions")
    var repetitions: Int = 0,  // Count of successful reviews

    @Column(name = "last_quality")
    var lastQuality: Int? = null  // 0-5 quality rating from last review
)
```

**Algorithm (SM-2):**
```kotlin
fun scheduleReview(item: VocabularyItemEntity, quality: Int) {
    require(quality in 0..5) { "Quality must be 0-5" }

    // Update easiness factor
    val ef = item.easinessFactor
    val newEF = max(1.3, ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
    item.easinessFactor = newEF
    item.lastQuality = quality

    // Calculate next interval
    val nextInterval = when {
        quality < 3 -> {
            item.repetitions = 0
            1.0 // Restart: review tomorrow
        }
        item.repetitions == 0 -> {
            item.repetitions = 1
            1.0 // First review: 1 day
        }
        item.repetitions == 1 -> {
            item.repetitions = 2
            6.0 // Second review: 6 days
        }
        else -> {
            item.repetitions += 1
            item.intervalDays * newEF // Exponential growth
        }
    }

    item.intervalDays = nextInterval
    item.nextReviewAt = Instant.now().plus(nextInterval.toLong(), ChronoUnit.DAYS)
}
```

**Pros:**
- Research-backed algorithm (40+ years of validation)
- Adaptive to individual word difficulty
- Fast learners progress quickly, slow words get more reviews
- Compatible with existing Anki workflows (users familiar)

**Cons:**
- More complex implementation (~200 LOC)
- Requires quality rating (0-5) which may not fit conversational flow
- Easiness factor can drift over time (stability issues)
- Quality rating UI needed (explicit feedback mechanism)

**Complexity:** 1 migration, 4 new columns, new service class (~300 LOC)
**Risk:** Medium

---

### Alternative 3: FSRS Algorithm with Retrievability Model (High Complexity)

**Approach:**
- Implement FSRS (Free Spaced Repetition Scheduler)
- Models memory stability and retrievability
- Optimized scheduling based on forgetting probability
- State-of-the-art performance (>20% improvement over SM-2)

**Schema Changes:**
```kotlin
@Entity
class VocabularyItemEntity(
    // existing fields...

    // NEW: FSRS fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "stability")
    var stability: Double = 0.0,  // Memory stability (days)

    @Column(name = "difficulty")
    var difficulty: Double = 5.0,  // Item difficulty (1-10)

    @Column(name = "elapsed_days")
    var elapsedDays: Double = 0.0,  // Time since last review

    @Column(name = "scheduled_days")
    var scheduledDays: Double = 0.0,  // Interval of last review

    @Column(name = "reps")
    var reps: Int = 0,  // Successful review count

    @Column(name = "lapses")
    var lapses: Int = 0,  // Forget count

    @Column(name = "state")
    var state: String = "NEW"  // NEW, LEARNING, REVIEW, RELEARNING
)
```

**Algorithm (FSRS - Simplified):**
```kotlin
fun scheduleReview(item: VocabularyItemEntity, rating: Int) {
    // rating: 1=Again, 2=Hard, 3=Good, 4=Easy

    val s = item.stability
    val d = item.difficulty

    // Calculate next stability based on rating and current state
    val nextStability = when (rating) {
        1 -> calculateStabilityAfterLapse(s, d)
        2 -> calculateStabilityHard(s, d)
        3 -> calculateStabilityGood(s, d)
        4 -> calculateStabilityEasy(s, d)
        else -> s
    }

    // Update difficulty (harder if failed, easier if succeeded)
    val nextDifficulty = d + (rating == 1).toInt() * 1.0 - (rating == 4).toInt() * 0.5
    item.difficulty = nextDifficulty.coerceIn(1.0, 10.0)

    // Calculate retrievability and interval
    val retrievability = 0.9 // Target 90% recall probability
    val interval = (nextStability * (1.0 / retrievability - 1)).pow(1.0 / 9.0) * nextStability

    item.stability = nextStability
    item.scheduledDays = interval
    item.nextReviewAt = Instant.now().plus(interval.toLong(), ChronoUnit.DAYS)

    // Update state and counters
    when (rating) {
        1 -> {
            item.lapses += 1
            item.state = "RELEARNING"
        }
        else -> {
            item.reps += 1
            item.state = "REVIEW"
        }
    }
}
```

**Pros:**
- State-of-the-art algorithm (best retention outcomes)
- More accurate forgetting predictions
- Adaptive to both item difficulty and user performance
- Published research validation (2022)

**Cons:**
- Significantly more complex implementation (~500 LOC)
- Requires understanding of FSRS formulas
- More database fields (8 new columns)
- Harder to debug and maintain
- Requires rating scale (1-4) different from traditional 0-5

**Complexity:** 1 migration, 8 new columns, complex math (~600 LOC)
**Risk:** High

---

### Alternative 4: Hybrid Approach - Simple with Upgrade Path (Recommended)

**Approach:**
- Start with Alternative 1 (Simple Fixed Intervals) for MVP
- Design schema extensible for future SM-2/FSRS upgrade
- Add `algorithmVersion` column to support migration
- Phase 2: Upgrade to SM-2 once Simple version validated

**Schema Changes (Extensible):**
```kotlin
@Entity
class VocabularyItemEntity(
    // existing fields...

    // Phase 1: Simple SRS fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "review_stage")
    var reviewStage: Int = 0,

    @Column(name = "srs_algorithm")
    var srsAlgorithm: String = "SIMPLE",  // "SIMPLE", "SM2", "FSRS"

    // Phase 2: SM-2 fields (nullable for backward compat)
    @Column(name = "interval_days")
    var intervalDays: Double? = null,

    @Column(name = "easiness_factor")
    var easinessFactor: Double? = null,

    @Column(name = "repetitions")
    var repetitions: Int? = null
)
```

**Pros:**
- Low initial complexity, quick delivery
- Proven algorithm upgrade path
- Users see value immediately (Phase 1)
- Can A/B test Simple vs SM-2 in Phase 2
- No data migration pain (schema extensible from start)

**Cons:**
- Suboptimal spacing in Phase 1
- Two-phase implementation (more total time)
- Some users may not benefit from upgrade if satisfied

**Complexity:** Phase 1: ~150 LOC, Phase 2: +200 LOC
**Risk:** Low (Phase 1), Medium (Phase 2)

---

## Recommended Solution: Alternative 4 (Hybrid Approach)

**Rationale:**
1. **Quick wins** - Phase 1 deliverable in 2-3 days
2. **Low risk** - Simple algorithm, easy to validate
3. **User value** - Even fixed intervals dramatically improve retention
4. **Upgrade path** - Schema designed for SM-2 migration
5. **Validation opportunity** - Gather usage data before investing in complex algorithms

**Phase 1 Impact:**
- ✅ Vocabulary reviews scheduled
- ✅ Due vocabulary surfaced to users
- ✅ Basic retention improvement (estimated 50-100%)
- ✅ Foundation for conversation integration

**Phase 2 Impact (optional):**
- ✅ Personalized difficulty adaptation
- ✅ Optimal spacing (2-3x retention vs Phase 1)
- ✅ Anki-compatible experience
- ✅ Advanced learner features

---

## Part 3: Critical Review

### Self-Review Checklist

#### ✅ Database Migration Safety
- New columns are nullable or have defaults
- No removal of existing columns
- Backward compatible with existing code
- Migration idempotent (can re-run safely)

#### ✅ Performance Concerns
- Index on `nextReviewAt` for efficient queries
- Query: `SELECT * FROM vocabulary_items WHERE user_id = ? AND next_review_at <= NOW() LIMIT 20`
- No N+1 queries in review fetching
- Batch updates for review scheduling

#### ✅ Backward Compatibility
- Existing vocabulary tracking unchanged
- Exposure counting preserved
- VocabularyContextEntity unaffected
- REST API additive (new endpoints, existing unchanged)

#### ✅ Edge Cases
- What if user never reviews? → nextReviewAt remains, no negative effects
- What if vocabulary added mid-conversation? → Schedule review from createdAt
- What if user deletes session? → Vocabulary persists (user-level, not session)
- Timezone handling → All times in UTC (Instant)

### Breaking Change Analysis

**Phase 1 - No Breaking Changes:**
- New columns with defaults
- Existing endpoints unchanged
- New endpoints are additive
- Vocabulary tracking flow preserved

**Phase 2 (Future SM-2) - Backward Compatible:**
- Algorithm selection via `srsAlgorithm` column
- Default "SIMPLE" for existing items
- Users opt-in to SM-2 via settings

### Consistency Validation

**Review Scheduling:**
- All new vocabulary gets `nextReviewAt = createdAt + 1 day`
- Due vocabulary query filters by `nextReviewAt <= NOW()`
- After review, `nextReviewAt` updated immediately

**Exposure vs Review:**
- `exposures` - Count of times word seen in conversation (automatic)
- `reviewStage` - Current SRS stage (manual review milestone)
- Both metrics tracked independently

---

## Known Limitations and Risks

### Technical Limitations

1. **No Precise Forgetting Curve**
   - **Issue:** Fixed intervals don't model individual forgetting curves
   - **Impact:** Some users need more/less reviews
   - **Mitigation:** Phase 2 SM-2 upgrade addresses this

2. **Binary Success Metric**
   - **Issue:** Phase 1 has no quality rating (0-5 scale)
   - **Impact:** Can't distinguish "barely remembered" from "easy recall"
   - **Mitigation:** Phase 2 adds quality ratings

3. **No Timezone Personalization**
   - **Issue:** Reviews scheduled in UTC, not user's local time
   - **Impact:** "Review in 1 day" might be 25 or 23 hours depending on timezone
   - **Mitigation:** Accept limitation for MVP, address in Phase 3

4. **Review Integration Friction**
   - **Issue:** Interrupting conversation flow with vocabulary drills feels forced
   - **Impact:** Users may skip reviews to continue chatting
   - **Future:** Dedicated review mode OR optional review prompts

### Architectural Risks

5. **Conversation Integration Complexity**
   - **Issue:** When/how to surface due vocabulary in chat?
   - **Risk:** Over-prompting annoys users, under-prompting defeats purpose
   - **Mitigation:** Start with explicit review mode, gather feedback

6. **Migration Rollback**
   - **Issue:** Adding columns is easy, removing is not
   - **Risk:** If feature fails, columns remain orphaned
   - **Mitigation:** Nullable columns allow graceful degradation

### Deployment Risks

7. **User Expectation Mismatch**
   - **Issue:** Users expect Anki-level sophistication (SM-2)
   - **Risk:** Simple algorithm disappoints power users
   - **Mitigation:** Clearly label as "Beta" in Phase 1, promise SM-2 upgrade

8. **Review Burden**
   - **Issue:** Aggressive users may accumulate 100+ due vocabulary
   - **Risk:** Review backlog becomes demotivating
   - **Mitigation:** Cap daily reviews at 20, prioritize by age

### Mitigations Summary

| Risk | Severity | Mitigation | Status |
|------|----------|------------|--------|
| Fixed intervals suboptimal | Low | Phase 2 SM-2 upgrade | Planned |
| Binary success metric | Low | Phase 2 quality ratings | Planned |
| Timezone handling | Low | Accept limitation for MVP | Accepted |
| Conversation integration | Medium | Start with review mode | Required |
| Migration rollback | Medium | Nullable columns | Implemented |
| User expectations | Medium | Beta labeling, roadmap | Required |
| Review backlog | High | 20-item daily cap | Required |

---

## Part 4: Implementation Plan

### Phase 1: Simple Fixed Intervals (Recommended First)

**Important Note on Database Migrations:**
This project uses JPA with `ddl-auto=update` (not Flyway/Liquibase). Schema changes happen automatically via JPA entity annotations when the application starts. The SQL migration files shown below are for documentation purposes only - they illustrate what schema changes will occur, but you do NOT need to create or run these SQL files manually.

#### Step 1: Database Schema Changes

**Illustrative SQL (for documentation only):**
```sql
-- Add SRS fields to vocabulary_items table
ALTER TABLE vocabulary_items
    ADD COLUMN next_review_at TIMESTAMP,
    ADD COLUMN review_stage INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN srs_algorithm VARCHAR(32) DEFAULT 'SIMPLE' NOT NULL;

-- Create index for efficient due vocabulary queries
CREATE INDEX idx_vocab_next_review
    ON vocabulary_items(user_id, next_review_at);

-- Initialize nextReviewAt for existing vocabulary (1 day from creation)
UPDATE vocabulary_items
    SET next_review_at = created_at + INTERVAL '1 day'
    WHERE next_review_at IS NULL;

-- Add vocabulary review mode flag to chat_sessions table
-- Similar to conversationPhase, this is user-controllable via REST API
ALTER TABLE chat_sessions
    ADD COLUMN vocabulary_review_mode BOOLEAN DEFAULT FALSE NOT NULL;
```

**What Actually Happens:**
When you update VocabularyItemEntity (Step 2) and restart the application, JPA will:
1. Detect new columns in the entity
2. Automatically add them to the vocabulary_items table
3. Create the index if specified with `@Index` annotation
4. Set default values for existing rows

**Testing:**
- Start application and verify logs show schema updates
- Check existing vocabulary data preserved
- Validate backward compatibility (existing code still works)

---

#### Step 2: Update VocabularyItemEntity

**File:** `VocabularyItemEntity.kt`

Add new fields:
```kotlin
@Entity
@Table(name = "vocabulary_items")
class VocabularyItemEntity(
    // existing fields...
    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant = Instant.now(),

    // NEW: SRS fields
    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "review_stage", nullable = false)
    var reviewStage: Int = 0,

    @Column(name = "srs_algorithm", nullable = false, length = 32)
    var srsAlgorithm: String = "SIMPLE",

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

**Testing:**
- Update `VocabularyServiceTest` to handle new fields
- Verify entity can be persisted and retrieved
- Test nullable nextReviewAt backward compatibility

---

#### Step 3: Create VocabularyReviewService

**File:** `VocabularyReviewService.kt`

```kotlin
package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.min
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VocabularyReviewService(
    private val vocabularyItemRepository: VocabularyItemRepository
) {
    private val intervals = listOf(1, 3, 7, 14, 30, 90) // days

    /**
     * Get due vocabulary for a user in a specific language.
     * Returns items where nextReviewAt <= now, ordered by nextReviewAt ASC.
     */
    fun getDueVocabulary(
        userId: UUID,
        lang: String,
        limit: Int = 20
    ): List<VocabularyItemEntity> {
        val now = Instant.now()
        return vocabularyItemRepository.findDueForReview(userId, lang, now, PageRequest.of(0, limit))
    }

    /**
     * Get count of due vocabulary items for a user.
     */
    fun getDueCount(userId: UUID, lang: String): Long {
        val now = Instant.now()
        return vocabularyItemRepository.countDueForReview(userId, lang, now)
    }

    /**
     * Mark vocabulary as reviewed and schedule next review.
     * @param success true if user remembered, false if forgot
     */
    @Transactional
    fun recordReview(
        itemId: UUID,
        success: Boolean
    ): VocabularyItemEntity {
        val item = vocabularyItemRepository.findById(itemId)
            .orElseThrow { IllegalArgumentException("Vocabulary item not found: $itemId") }

        if (success) {
            // Advance to next stage
            item.reviewStage = minOf(item.reviewStage + 1, intervals.size - 1)
        } else {
            // Reset to beginning
            item.reviewStage = 0
        }

        // Schedule next review
        val daysToAdd = intervals[item.reviewStage].toLong()
        item.nextReviewAt = Instant.now().plus(daysToAdd, ChronoUnit.DAYS)

        return vocabularyItemRepository.save(item)
    }

    /**
     * Schedule initial review for newly added vocabulary.
     * Called by VocabularyService after creating/updating item.
     * Note: Caller must persist the entity after calling this method.
     */
    fun scheduleInitialReview(item: VocabularyItemEntity) {
        if (item.nextReviewAt == null) {
            // First review in 1 day
            item.nextReviewAt = Instant.now().plus(1, ChronoUnit.DAYS)
            item.reviewStage = 0
        }
    }
}
```

**Testing:**
```kotlin
class VocabularyReviewServiceTest {
    @Test
    fun `getDueVocabulary returns items with nextReviewAt in past`() {
        // Create test items with past nextReviewAt
        // Verify returned by getDueVocabulary
    }

    @Test
    fun `recordReview advances stage on success`() {
        val item = createTestItem(reviewStage = 2)
        service.recordReview(item.id, success = true)
        assertEquals(3, item.reviewStage)
    }

    @Test
    fun `recordReview resets stage on failure`() {
        val item = createTestItem(reviewStage = 4)
        service.recordReview(item.id, success = false)
        assertEquals(0, item.reviewStage)
    }

    @Test
    fun `scheduleInitialReview sets nextReviewAt to 1 day`() {
        val item = createTestItem(nextReviewAt = null)
        service.scheduleInitialReview(item)

        val expected = Instant.now().plus(1, ChronoUnit.DAYS)
        assertTrue(item.nextReviewAt!!.isAfter(Instant.now()))
        assertTrue(item.nextReviewAt!!.isBefore(expected.plus(1, ChronoUnit.MINUTES)))
    }
}
```

---

#### Step 4: Update VocabularyItemRepository

**File:** `VocabularyItemRepository.kt`

Add query methods:
```kotlin
interface VocabularyItemRepository : JpaRepository<VocabularyItemEntity, UUID> {
    // existing methods...

    @Query("""
        SELECT v FROM VocabularyItemEntity v
        WHERE v.userId = :userId
          AND v.lang = :lang
          AND v.nextReviewAt <= :now
        ORDER BY v.nextReviewAt ASC
    """)
    fun findDueForReview(
        userId: UUID,
        lang: String,
        now: Instant,
        pageable: org.springframework.data.domain.Pageable
    ): List<VocabularyItemEntity>

    @Query("""
        SELECT COUNT(v) FROM VocabularyItemEntity v
        WHERE v.userId = :userId
          AND v.lang = :lang
          AND v.nextReviewAt <= :now
    """)
    fun countDueForReview(
        userId: UUID,
        lang: String,
        now: Instant
    ): Long
}
```

---

#### Step 5: Integrate with VocabularyService

**File:** `VocabularyService.kt`

Update `addNewVocabulary` to schedule reviews:
```kotlin
package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyContextEntity
import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyContextRepository
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import ch.obermuhlner.aitutor.core.model.NewVocabularyDTO
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VocabularyService(
    private val vocabularyItemRepository: VocabularyItemRepository,
    private val vocabularyContextRepository: VocabularyContextRepository,
    private val vocabularyReviewService: VocabularyReviewService  // NEW dependency
) {
    @Transactional
    fun addNewVocabulary(
        userId: UUID,
        lang: String,
        items: List<NewVocabularyDTO>,
        turnId: UUID? = null
    ): List<VocabularyItemEntity> {
        val saved = mutableListOf<VocabularyItemEntity>()

        for (nv in items) {
            // Find or create vocabulary item
            val item = vocabularyItemRepository.findByUserIdAndLangAndLemma(userId, lang, nv.lemma)
                ?: VocabularyItemEntity(
                    id = UUID.randomUUID(),
                    userId = userId,
                    lang = lang,
                    lemma = nv.lemma,
                    conceptName = nv.concept,
                    exposures = 0
                )

            // Increment exposure count
            item.exposures++
            item.lastSeenAt = java.time.Instant.now()

            var persisted = vocabularyItemRepository.save(item)

            // NEW: Schedule initial review for new vocabulary
            vocabularyReviewService.scheduleInitialReview(persisted)
            // Save again after scheduling review
            persisted = vocabularyItemRepository.save(persisted)

            // Save context
            vocabularyContextRepository.save(
                VocabularyContextEntity(
                    vocabItem = persisted,
                    context = nv.context.take(512),
                    turnId = turnId
                )
            )
            saved += persisted
        }

        return saved
    }
}
```

---

#### Step 6: Add REST API Endpoints

**File:** `VocabularyController.kt`

Add new endpoints:
```kotlin
package ch.obermuhlner.aitutor.vocabulary.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.vocabulary.dto.VocabularyItemResponse
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyQueryService
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService
import java.util.UUID
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/vocabulary")
class VocabularyController(
    private val vocabularyQueryService: VocabularyQueryService,
    private val vocabularyReviewService: VocabularyReviewService,
    private val authorizationService: AuthorizationService
) {
    // existing endpoints...

    /**
     * Get due vocabulary items for review in a specific language.
     * GET /api/v1/vocabulary/due?lang=es&limit=20
     */
    @GetMapping("/due")
    fun getDueVocabulary(
        @RequestParam lang: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<VocabularyItemResponse> {
        val userId = authorizationService.getCurrentUserId()
        return vocabularyReviewService.getDueVocabulary(userId, lang, limit)
            .map { it.toResponse() }
    }

    /**
     * Get count of due vocabulary items in a specific language.
     * GET /api/v1/vocabulary/due/count?lang=es
     */
    @GetMapping("/due/count")
    fun getDueCount(
        @RequestParam lang: String
    ): DueCountResponse {
        val userId = authorizationService.getCurrentUserId()
        val count = vocabularyReviewService.getDueCount(userId, lang)
        return DueCountResponse(count)
    }

    /**
     * Record vocabulary review result.
     * POST /api/v1/vocabulary/{itemId}/review
     * Body: { "success": true }
     */
    @PostMapping("/{itemId}/review")
    fun recordReview(
        @PathVariable itemId: UUID,
        @RequestBody request: RecordReviewRequest
    ): VocabularyItemResponse {
        val userId = authorizationService.getCurrentUserId()

        // Verify ownership
        val item = vocabularyQueryService.getVocabularyItemById(itemId)
        authorizationService.requireOwnership(userId, item.userId)

        val updated = vocabularyReviewService.recordReview(itemId, request.success)
        return updated.toResponse()
    }
}

data class RecordReviewRequest(val success: Boolean)
data class DueCountResponse(val count: Long)
```

---

#### Step 6.5: Add Method to VocabularyQueryService

**File:** `VocabularyQueryService.kt`

Add method for retrieving individual vocabulary items:
```kotlin
package ch.obermuhlner.aitutor.vocabulary.service

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import ch.obermuhlner.aitutor.vocabulary.repository.VocabularyItemRepository
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class VocabularyQueryService(
    private val vocabularyItemRepository: VocabularyItemRepository
    // existing dependencies...
) {
    // existing methods...

    /**
     * Get vocabulary item by ID.
     * Used for ownership verification before recording reviews.
     */
    fun getVocabularyItemById(itemId: UUID): VocabularyItemEntity {
        return vocabularyItemRepository.findById(itemId)
            .orElseThrow { IllegalArgumentException("Vocabulary item not found: $itemId") }
    }
}
```

---

#### Step 7: Update DTOs

**File:** `VocabularyItemResponse.kt`

Add SRS fields to response:
```kotlin
package ch.obermuhlner.aitutor.vocabulary.dto

import ch.obermuhlner.aitutor.vocabulary.domain.VocabularyItemEntity
import java.time.Instant
import java.util.UUID

data class VocabularyItemResponse(
    val id: UUID,
    val lang: String,
    val lemma: String,
    val exposures: Int,
    val lastSeenAt: Instant,
    val conceptName: String?,
    val createdAt: Instant,

    // NEW: SRS fields
    val nextReviewAt: Instant?,
    val reviewStage: Int,
    val isDue: Boolean  // Computed: nextReviewAt <= now
)

fun VocabularyItemEntity.toResponse(): VocabularyItemResponse {
    val now = Instant.now()
    return VocabularyItemResponse(
        id = id,
        lang = lang,
        lemma = lemma,
        exposures = exposures,
        lastSeenAt = lastSeenAt,
        conceptName = conceptName,
        createdAt = createdAt!!,
        nextReviewAt = nextReviewAt,
        reviewStage = reviewStage,
        isDue = nextReviewAt?.let { it <= now } ?: false
    )
}
```

---

#### Step 8: Update ChatSessionEntity and ConversationState

**File:** `ChatSessionEntity.kt`

Add vocabulary review mode flag:
```kotlin
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // existing fields...

    // NEW: Vocabulary review mode control (similar to conversationPhase)
    @Column(name = "vocabulary_review_mode", nullable = false)
    var vocabularyReviewMode: Boolean = false,

    // existing fields...
)
```

**File:** `ConversationState.kt`

Add to ConversationState for LLM prompt integration:
```kotlin
data class ConversationState(
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
    @field:JsonPropertyDescription("The current topic of conversation, or null if no specific topic.")
    val currentTopic: String? = null,

    // existing metadata fields...

    // NEW: Vocabulary review mode flag
    @field:JsonPropertyDescription("Whether vocabulary review mode is enabled for this session. When true, the tutor should naturally integrate due vocabulary review into conversation.")
    val vocabularyReviewMode: Boolean = false,
    @field:JsonPropertyDescription("Count of vocabulary items due for review (for context).")
    val dueVocabularyCount: Int? = null
)
```

**Testing:**
- Update `ChatSessionEntityTest` to handle new field
- Verify backward compatibility (default = false)
- Test ConversationState serialization with new field

---

#### Step 9: Add Session Review Mode REST Endpoint

**File:** `ChatController.kt`

Add endpoint to toggle vocabulary review mode:
```kotlin
package ch.obermuhlner.aitutor.chat.controller

import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.dto.SessionResponse
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatSessionRepository: ChatSessionRepository,
    private val authorizationService: AuthorizationService
    // existing dependencies...
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // existing endpoints...

    /**
     * Update vocabulary review mode for a session.
     * PATCH /api/v1/chat/sessions/{id}/vocabulary-review-mode
     * Body: { "enabled": true }
     */
    @PatchMapping("/sessions/{sessionId}/vocabulary-review-mode")
    fun updateVocabularyReviewMode(
        @PathVariable sessionId: UUID,
        @RequestBody request: UpdateVocabularyReviewModeRequest
    ): SessionResponse {
        val userId = authorizationService.getCurrentUserId()
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        authorizationService.requireOwnership(userId, session.userId)

        session.vocabularyReviewMode = request.enabled
        chatSessionRepository.save(session)

        logger.info("Vocabulary review mode ${if (request.enabled) "enabled" else "disabled"} for session $sessionId")

        return session.toResponse()
    }
}

data class UpdateVocabularyReviewModeRequest(val enabled: Boolean)
```

**File:** `SessionResponse.kt`

Add field to response DTO:
```kotlin
package ch.obermuhlner.aitutor.chat.dto

import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import java.time.Instant
import java.util.UUID

data class SessionResponse(
    val id: UUID,
    val userId: UUID,
    // existing fields...
    val conversationPhase: String,
    val effectivePhase: String?,
    val estimatedCEFRLevel: String,

    // NEW: Vocabulary review mode
    val vocabularyReviewMode: Boolean,

    // existing fields...
)

fun ChatSessionEntity.toResponse(): SessionResponse {
    return SessionResponse(
        id = id,
        userId = userId,
        // existing mappings...
        conversationPhase = conversationPhase.name,
        effectivePhase = effectivePhase?.name,
        estimatedCEFRLevel = estimatedCEFRLevel.name,
        vocabularyReviewMode = vocabularyReviewMode,
        // existing mappings...
    )
}
```

**Testing:**
- Test enabling review mode for a session
- Test disabling review mode
- Verify authorization checks
- Test backward compatibility of SessionResponse

---

#### Step 10: Integrate Review Mode with TutorService

**File:** `ChatService.kt`

Pass review mode to TutorService via ConversationState:
```kotlin
@Service
class ChatService(
    // existing dependencies...
    private val vocabularyReviewService: VocabularyReviewService  // NEW
) {
    fun sendMessage(
        sessionId: UUID,
        content: String,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        // existing logic...

        // NEW: Get due vocabulary count if review mode enabled
        val dueCount = if (session.vocabularyReviewMode) {
            vocabularyReviewService.getDueCount(session.userId, session.targetLanguageCode)
        } else {
            null
        }

        // Pass review mode to LLM via enriched ConversationState
        val conversationState = ConversationState(
            phase = phaseDecision.phase,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            currentTopic = session.currentTopic,
            phaseReason = phaseDecision.reason,
            topicEligibilityStatus = topicMetadata.eligibilityStatus,
            pastTopics = topicMetadata.pastTopics,
            vocabularyReviewMode = session.vocabularyReviewMode,  // NEW
            dueVocabularyCount = dueCount  // NEW
        )

        // existing TutorService call...
    }
}
```

**File:** `TutorService.kt`

Update system prompt to include review mode guidance:
```kotlin
internal fun buildConsolidatedSystemPrompt(
    tutor: Tutor,
    conversationState: ConversationState,
    // existing parameters...
): String = buildString {
    // existing prompt sections...

    append("\n\n")

    // Session Context (structured, not toString())
    append("=== Current Session Context ===\n")
    append("Phase: ${conversationState.phase.name} ($phaseReason)\n")
    append("CEFR Level: ${conversationState.estimatedCEFRLevel.name}\n")
    append("Topic: ${conversationState.currentTopic ?: "Free conversation"} ($topicEligibilityStatus)\n")
    if (pastTopics.isNotEmpty()) {
        append("Recent Topics: ${pastTopics.takeLast(3).joinToString(", ")}\n")
    }

    // NEW: Vocabulary review mode guidance
    if (conversationState.vocabularyReviewMode && conversationState.dueVocabularyCount != null && conversationState.dueVocabularyCount > 0) {
        append("\nVocabulary Review Mode: ACTIVE\n")
        append("Due for Review: ${conversationState.dueVocabularyCount} words\n")
        append("Guidance: Naturally integrate 2-3 due vocabulary words into the conversation. Ask the learner to use them, or prompt recall (e.g., 'Do you remember the word for...'). Keep it conversational, not quiz-like.\n")
    }

    append("\n")

    // existing language metadata...
}
```

**Testing:**
- Test prompt includes review guidance when mode enabled
- Test prompt excludes guidance when mode disabled
- Test with due vocabulary count = 0 (no guidance)
- Integration test: full conversation with review mode

---

### Phase 2: SM-2 Algorithm (Optional Future Enhancement)

Defer until Phase 1 validated. Implementation details in separate task document.

---

## Part 5: REST API Changes

### New Endpoints

#### GET /api/v1/vocabulary/due
**Description:** Get vocabulary items due for review in a specific language

**Query Parameters:**
- `lang` (required) - Language code (e.g., "es", "fr")
- `limit` (optional) - Max items to return (default: 20)

**Response:**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "lang": "es",
    "lemma": "hablar",
    "exposures": 3,
    "lastSeenAt": "2025-01-10T14:30:00Z",
    "conceptName": "speaking",
    "createdAt": "2025-01-05T10:00:00Z",
    "nextReviewAt": "2025-01-09T10:00:00Z",
    "reviewStage": 2,
    "isDue": true
  }
]
```

---

#### GET /api/v1/vocabulary/due/count
**Description:** Get count of due vocabulary items in a specific language

**Query Parameters:**
- `lang` (required) - Language code (e.g., "es", "fr")

**Response:**
```json
{
  "count": 15
}
```

**Note:** Count is returned as a number (can be larger than Int max value)

---

#### POST /api/v1/vocabulary/{itemId}/review
**Description:** Record review result and schedule next review

**Request Body:**
```json
{
  "success": true
}
```

**Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "lang": "es",
  "lemma": "hablar",
  "exposures": 3,
  "nextReviewAt": "2025-01-16T14:30:00Z",
  "reviewStage": 3,
  "isDue": false
}
```

---

#### PATCH /api/v1/chat/sessions/{id}/vocabulary-review-mode
**Description:** Enable or disable vocabulary review mode for a session

**Path Parameters:**
- `id` (required) - Session UUID

**Request Body:**
```json
{
  "enabled": true
}
```

**Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user-uuid",
  "tutorName": "María",
  "conversationPhase": "Correction",
  "estimatedCEFRLevel": "B1",
  "vocabularyReviewMode": true,
  "createdAt": "2025-01-10T14:30:00Z"
}
```

**Behavior:**
- When `vocabularyReviewMode = true`: TutorService receives due vocabulary count in ConversationState
- LLM system prompt includes guidance to naturally integrate 2-3 due vocabulary words into conversation
- Similar to `conversationPhase`, this is user-controlled and persisted per session

**Authorization:** Requires session ownership

---

### Modified Endpoints

#### GET /api/v1/vocabulary
**Changes:** Response now includes SRS fields (`nextReviewAt`, `reviewStage`, `isDue`)

**Backward Compatibility:** New fields are additive, existing clients ignore them

---

#### GET /api/v1/chat/sessions/{id}
**Changes:** SessionResponse now includes `vocabularyReviewMode` boolean field

**Example Response:**
```json
{
  "id": "session-uuid",
  "userId": "user-uuid",
  "tutorName": "María",
  "conversationPhase": "Correction",
  "effectivePhase": "Correction",
  "estimatedCEFRLevel": "B1",
  "vocabularyReviewMode": false,
  "currentTopic": "travel",
  "createdAt": "2025-01-10T14:30:00Z"
}
```

**Backward Compatibility:** New field has default value (false), existing clients ignore it

---

## Future Enhancements

### 1. Conversation-Integrated Reviews
**Current:** Dedicated review mode (separate from chat)
**Future:** Surface due vocabulary naturally in conversation

**Example:**
```
User: "I want to talk about travel"
Tutor: "¡Perfecto! By the way, do you remember how to say 'airport'?"
User: "Aeropuerto?"
Tutor: "¡Exacto! Now, let's talk about your travel plans..."
```

**Effort:** 2-3 days (prompt engineering, conversation flow)

---

### 2. SM-2 Algorithm Upgrade (Phase 2)
**Current:** Fixed intervals (1, 3, 7, 14, 30, 90 days)
**Future:** Adaptive intervals with easiness factor

**Benefits:**
- 2-3x better retention vs fixed intervals
- Personalized difficulty per word
- Anki-compatible experience

**Effort:** 3-4 days (algorithm, testing, migration)

---

### 3. Review Session Mode
**Current:** Review via REST API only
**Future:** Dedicated UI review session

**Features:**
- Flashcard-style interface
- Progress tracking (5/20 reviewed)
- Keyboard shortcuts (space = flip, 1-4 = rate)
- Session statistics (accuracy, time spent)

**Effort:** 5-7 days (frontend work)

---

### 4. Streak Tracking and Gamification
**Current:** No motivational features
**Future:** Daily streak counter, badges, milestones

**Features:**
- "7-day review streak!"
- Badges: "100 words reviewed", "Perfect week"
- Progress visualization

**Effort:** 3-4 days (backend + frontend)

---

## Summary

**Recommended Approach:** Phase 1 - Simple Fixed Intervals

**Why:**
- Low risk, high impact
- Quick delivery (2-3 days)
- Proven pedagogical benefit (2-3x retention improvement)
- Foundation for SM-2 upgrade
- Schema designed for future extensibility

**Expected Outcomes:**
- Vocabulary retention improvement: 50-100% vs no SRS
- Users see due vocabulary via REST API
- Review workflow established
- Data collection for SM-2 optimization

**Phase 2 (Optional):**
- Defer SM-2 until Phase 1 validated
- Estimated 2-3x additional retention improvement
- Requires quality rating UI
- 3-4 days implementation

**Total Effort:**
- Phase 1: 2-3 days (migration, service, endpoints, tests)
- Phase 2: 3-4 days (SM-2 algorithm, UI, migration)
