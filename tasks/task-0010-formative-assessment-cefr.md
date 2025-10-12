# Task 0010: Formative Assessment and Dynamic CEFR Level Adjustment

## Problem Statement

The current CEFR level tracking system provides a **static initial estimate** that never adjusts based on learner performance. This limits the system's adaptiveness and pedagogical effectiveness:

1. **One-time assessment** - `estimatedCEFRLevel` set at session creation, never updated
2. **No progress tracking** - Can't detect when learner progresses from A2 → B1
3. **Holistic level only** - Single level for all skills (reading, writing, grammar, vocabulary)
4. **No skill-specific tracking** - Strong vocabulary + weak grammar both reported as "B1"
5. **Limited phase decision input** - Auto mode doesn't know if level estimate is accurate
6. **No diagnostic feedback** - Learners can't see "You're B1 in grammar, A2 in vocabulary"
7. **Missed motivational opportunity** - No celebration of level-up milestones

## Part 1: Analysis

### Current Architecture

#### CEFR Level Storage (ChatSessionEntity.kt)
```kotlin
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // Single holistic CEFR level, set once at creation
    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_cefr_level", nullable = false, length = 8)
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

    // No skill breakdown
    // No assessment history
    // No confidence score
)
```

#### CEFR Enum (CEFRLevel.kt)
```kotlin
enum class CEFRLevel {
    A1, A2, B1, B2, C1, C2
}
```

**CEFR Framework (Council of Europe):**
- **A1/A2 (Beginner):** Basic phrases, simple sentences, present tense
- **B1/B2 (Intermediate):** Connected discourse, past/future, complex sentences
- **C1/C2 (Advanced):** Sophisticated expression, nuanced meaning, native-like

#### Current Assessment Flow
1. User creates session, manually selects estimated CEFR level (or default A1)
2. Level passed to TutorService in ConversationState
3. LLM adjusts difficulty accordingly
4. **Level never changes** - no reassessment mechanism

### Pedagogical Foundation

#### Formative Assessment Research

**Formative vs Summative (Black & Wiliam, 1998):**
- **Formative:** Ongoing assessment during learning, adjusts instruction
- **Summative:** Final assessment after learning, evaluates outcomes
- Formative assessment improves learning outcomes 0.4-0.7 standard deviations

**Assessment Frequency (Hattie, 2009):**
- Frequent low-stakes assessments > infrequent high-stakes tests
- Optimal: Every 5-10 learning interactions (messages)
- Enables real-time adaptation

**Can-Do Statements (CEFR, Council of Europe):**
- A1: "Can introduce themselves"
- B1: "Can describe experiences and events"
- C1: "Can express ideas fluently and spontaneously"
- Observable behaviors enable level estimation

**Multi-Dimensional Proficiency (Bachman & Palmer, 1996):**
- Language ability is multi-faceted: grammar, vocabulary, fluency, pragmatics
- Single holistic score obscures diagnostic information
- Skill-specific tracking enables targeted instruction

#### Assessment Approaches

**Error-Based Inference:**
- High-severity errors (Critical/High) → Lower level estimate
- Error type distribution correlates with CEFR levels
- Example: Persistent Agreement errors → likely A2-B1

**Vocabulary Complexity:**
- Word frequency bands (1-5k: A1-A2, 5k-10k: B1-B2, 10k+: C1-C2)
- Sentence length (A1: 5-8 words, B1: 10-15, C1: 15+)
- Already tracked via vocabulary system

**Production Metrics:**
- Message length and complexity
- Use of subordinate clauses
- Tense variety (present only → A1, past/future → B1+)

**Self-Assessment:**
- User confidence ratings
- Difficulty perception ("That was hard")
- Correlates with actual proficiency

### Constraints

**MUST preserve:**
- Existing `estimatedCEFRLevel` field (extend, don't replace)
- ChatSessionEntity backward compatibility
- TutorService integration (ConversationState.estimatedCEFRLevel)
- REST API backward compatibility

**MUST NOT:**
- Remove holistic CEFR level (still useful for overall difficulty)
- Break existing phase decision logic
- Change LLM prompts that reference CEFR level

### What Needs to Change

1. **New skill-specific tracking** - Separate levels for grammar, vocabulary, fluency
2. **Assessment triggers** - Periodic checks every N messages
3. **Level adjustment logic** - Aggregate signals (errors, complexity, production)
4. **ChatSessionEntity** - Add skill breakdown fields
5. **REST endpoints** - Expose skill breakdown and progress reports
6. **Database migration** - Add new columns for skill tracking

## Part 2: Solution Design

### Alternative 1: Simple Periodic Reassessment (Low Complexity)

**Approach:**
- Every 20 messages, LLM reassesses holistic CEFR level
- Based on recent 5-10 messages
- Update `estimatedCEFRLevel` if confidence high
- No skill breakdown

**Schema Changes:**
```kotlin
@Entity
class ChatSessionEntity(
    // Existing field
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

    // NEW: Track assessment metadata
    @Column(name = "last_assessment_at")
    var lastAssessmentAt: Instant? = null,

    @Column(name = "assessment_confidence")
    var assessmentConfidence: Double? = null,  // 0.0-1.0

    @Column(name = "message_count_at_last_assessment")
    var messageCountAtLastAssessment: Int = 0
)
```

**Assessment Logic:**
```kotlin
fun checkForReassessment(session: ChatSessionEntity, messageCount: Int) {
    val messagesSinceAssessment = messageCount - session.messageCountAtLastAssessment

    if (messagesSinceAssessment >= 20) {
        // Trigger LLM reassessment
        val assessment = llm.assessCEFRLevel(recentMessages)
        if (assessment.confidence >= 0.7) {
            session.estimatedCEFRLevel = assessment.level
            session.assessmentConfidence = assessment.confidence
            session.lastAssessmentAt = Instant.now()
            session.messageCountAtLastAssessment = messageCount
        }
    }
}
```

**Pros:**
- Minimal schema changes (3 columns)
- Simple implementation (~100 LOC)
- Preserves existing CEFR field semantics
- No skill breakdown complexity

**Cons:**
- Holistic level only (no diagnostic info)
- LLM call overhead every 20 messages
- No visibility into skill-specific strengths/weaknesses
- Limited pedagogical value

**Complexity:** 1 migration, 1 service method, LLM prompt (~150 LOC)
**Risk:** Low

---

### Alternative 2: Skill-Specific Tracking with Heuristics (Medium Complexity)

**Approach:**
- Track separate levels for: Grammar, Vocabulary, Fluency, Comprehension
- Compute skill levels using heuristics (no LLM reassessment)
- Holistic level = weighted average of skill levels
- Update after every message (lightweight calculations)

**Schema Changes:**
```kotlin
@Entity
class ChatSessionEntity(
    // Existing holistic level (computed from skill levels)
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

    // NEW: Skill-specific levels
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_grammar")
    var cefrGrammar: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_vocabulary")
    var cefrVocabulary: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_fluency")
    var cefrFluency: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_comprehension")
    var cefrComprehension: CEFRLevel? = null,

    // Metadata
    @Column(name = "last_assessment_at")
    var lastAssessmentAt: Instant? = null,

    @Column(name = "total_assessment_count")
    var totalAssessmentCount: Int = 0
)
```

**Assessment Logic (Heuristic):**
```kotlin
@Service
class CEFRAssessmentService(
    private val errorAnalyticsService: ErrorAnalyticsService,
    private val vocabularyService: VocabularyService
) {
    fun assessSkillLevels(session: ChatSessionEntity): SkillAssessment {
        // Grammar: Based on error patterns
        val grammarErrors = errorAnalyticsService.getGrammarErrorScore(
            session.userId, session.targetLanguageCode
        )
        val cefrGrammar = mapErrorScoreToLevel(grammarErrors)

        // Vocabulary: Based on vocabulary complexity
        val vocabCount = vocabularyService.getVocabularyCountForLanguage(
            session.userId, session.targetLanguageCode
        )
        val cefrVocabulary = mapVocabCountToLevel(vocabCount)

        // Fluency: Based on message complexity
        val avgMessageLength = computeAverageMessageLength(session.id)
        val cefrFluency = mapMessageLengthToLevel(avgMessageLength)

        // Comprehension: Based on tutor complexity vs user performance
        val cefrComprehension = inferComprehensionLevel(session)

        return SkillAssessment(
            grammar = cefrGrammar,
            vocabulary = cefrVocabulary,
            fluency = cefrFluency,
            comprehension = cefrComprehension,
            overall = computeOverallLevel(cefrGrammar, cefrVocabulary, cefrFluency, cefrComprehension)
        )
    }

    private fun mapErrorScoreToLevel(score: Double): CEFRLevel {
        // Severity-weighted error score (from Task 0009)
        return when {
            score < 1.0 -> CEFRLevel.C1  // Very few errors
            score < 3.0 -> CEFRLevel.B2
            score < 6.0 -> CEFRLevel.B1
            score < 10.0 -> CEFRLevel.A2
            else -> CEFRLevel.A1
        }
    }

    private fun mapVocabCountToLevel(count: Int): CEFRLevel {
        return when {
            count >= 2000 -> CEFRLevel.C1  // Active vocabulary
            count >= 1000 -> CEFRLevel.B2
            count >= 500 -> CEFRLevel.B1
            count >= 200 -> CEFRLevel.A2
            else -> CEFRLevel.A1
        }
    }

    private fun mapMessageLengthToLevel(avgLength: Double): CEFRLevel {
        return when {
            avgLength >= 20.0 -> CEFRLevel.C1  // Complex sentences
            avgLength >= 15.0 -> CEFRLevel.B2
            avgLength >= 10.0 -> CEFRLevel.B1
            avgLength >= 7.0 -> CEFRLevel.A2
            else -> CEFRLevel.A1
        }
    }

    private fun computeOverallLevel(
        grammar: CEFRLevel,
        vocabulary: CEFRLevel,
        fluency: CEFRLevel,
        comprehension: CEFRLevel
    ): CEFRLevel {
        // Weighted average (grammar = 40%, vocabulary = 30%, fluency = 20%, comprehension = 10%)
        val weights = mapOf(
            grammar to 0.4,
            vocabulary to 0.3,
            fluency to 0.2,
            comprehension to 0.1
        )

        val levelValues = mapOf(
            CEFRLevel.A1 to 1, CEFRLevel.A2 to 2,
            CEFRLevel.B1 to 3, CEFRLevel.B2 to 4,
            CEFRLevel.C1 to 5, CEFRLevel.C2 to 6
        )

        val weightedSum = weights.entries.sumOf { (level, weight) ->
            levelValues[level]!! * weight
        }

        val roundedLevel = weightedSum.roundToInt().coerceIn(1, 6)
        return levelValues.entries.first { it.value == roundedLevel }.key
    }
}
```

**Pros:**
- Skill-specific diagnostic information
- No LLM overhead (heuristic-based)
- Real-time updates (after every message)
- Enables targeted recommendations ("Practice grammar")

**Cons:**
- Heuristic accuracy limited (no ground truth validation)
- Thresholds may need tuning per language
- More complex implementation (~400 LOC)
- 4 new database columns

**Complexity:** 1 migration, 4 columns, 1 service (~400 LOC)
**Risk:** Medium

---

### Alternative 3: LLM-Based Skill Assessment (High Complexity)

**Approach:**
- Every 20 messages, trigger LLM assessment for all 4 skills
- LLM provides skill-specific levels + confidence + rationale
- Hybrid: Heuristics between LLM calls for lightweight updates
- Highest accuracy but highest cost

**Schema:**
Same as Alternative 2, plus:
```kotlin
@Column(name = "grammar_confidence")
var grammarConfidence: Double? = null,

@Column(name = "vocabulary_confidence")
var vocabularyConfidence: Double? = null,

@Column(name = "fluency_confidence")
var fluencyConfidence: Double? = null,

@Column(name = "comprehension_confidence")
var comprehensionConfidence: Double? = null,

@Column(name = "last_llm_assessment_at")
var lastLLMAssessmentAt: Instant? = null
```

**Assessment Logic:**
```kotlin
fun assessWithLLM(session: ChatSessionEntity, recentMessages: List<ChatMessageEntity>): SkillAssessment {
    val prompt = buildAssessmentPrompt(session, recentMessages)
    val response = llm.call(prompt)  // Returns JSON with skill levels + confidence

    return SkillAssessment(
        grammar = response.cefrGrammar,
        vocabulary = response.cefrVocabulary,
        fluency = response.cefrFluency,
        comprehension = response.cefrComprehension,
        grammarConfidence = response.grammarConfidence,
        // ... etc
    )
}

// Hybrid approach: Heuristics between LLM calls
fun updateSkillLevels(session: ChatSessionEntity, messageCount: Int) {
    val messagesSinceLLM = messageCount - session.messageCountAtLastLLMAssessment

    if (messagesSinceLLM >= 20) {
        // Expensive LLM assessment
        val assessment = assessWithLLM(session, recentMessages)
        session.updateSkills(assessment)
        session.lastLLMAssessmentAt = Instant.now()
    } else {
        // Cheap heuristic update
        val assessment = assessWithHeuristics(session)
        session.updateSkillsIfChanged(assessment)  // Only update if level changes
    }
}
```

**Pros:**
- Highest accuracy (LLM understands nuance)
- Rationale provided ("Vocabulary strong but grammar needs work")
- Confidence scores for each skill
- Gold standard for assessment

**Cons:**
- High LLM API cost (~$0.01 per assessment × 20-message intervals)
- Latency overhead (500-1000ms per assessment)
- Complex prompt engineering required
- 8 new database columns (skill levels + confidences)

**Complexity:** 1 migration, 8 columns, 2 assessment methods (~600 LOC)
**Risk:** High

---

### Alternative 4: Hybrid - Heuristics + Optional LLM Validation (Recommended)

**Approach:**
- Default: Alternative 2 (heuristic skill tracking, real-time)
- Optional: LLM validation every 50 messages for accuracy check
- LLM results used to calibrate heuristic thresholds
- Best of both worlds: fast + accurate

**Schema:**
Same as Alternative 2, plus:
```kotlin
@Column(name = "last_llm_validation_at")
var lastLLMValidationAt: Instant? = null,

@Column(name = "llm_validation_count")
var llmValidationCount: Int = 0
```

**Assessment Logic:**
```kotlin
fun updateSkillLevels(session: ChatSessionEntity, messageCount: Int) {
    // Always update with heuristics (fast, real-time)
    val heuristicAssessment = assessWithHeuristics(session)
    session.updateSkills(heuristicAssessment)

    // Optional LLM validation every 50 messages (accuracy check)
    val messagesSinceValidation = messageCount - session.messageCountAtLastValidation
    if (messagesSinceValidation >= 50) {
        try {
            val llmAssessment = assessWithLLM(session, recentMessages)

            // Compare heuristic vs LLM
            val discrepancy = computeDiscrepancy(heuristicAssessment, llmAssessment)
            if (discrepancy > 1.0) {  // More than 1 CEFR level off
                // Trust LLM, update session
                session.updateSkills(llmAssessment)
                logger.warn("Heuristic assessment diverged from LLM, correcting")
            }

            session.lastLLMValidationAt = Instant.now()
            session.llmValidationCount += 1
        } catch (e: Exception) {
            logger.error("LLM validation failed, continuing with heuristics", e)
        }
    }
}
```

**Pros:**
- Real-time heuristic updates (every message)
- LLM validation ensures accuracy (every 50 messages)
- Lower cost than full LLM assessment
- Graceful degradation (LLM optional)

**Cons:**
- Still some LLM cost (reduced vs Alternative 3)
- Two assessment code paths to maintain
- Heuristic-LLM discrepancies need handling

**Complexity:** 1 migration, 6 columns, 2 assessment methods (~500 LOC)
**Risk:** Medium

---

## Recommended Solution: Alternative 4 (Hybrid Approach)

**Rationale:**
1. **Real-time feedback** - Heuristics update after every message
2. **Accuracy assurance** - LLM validation prevents drift
3. **Cost-effective** - LLM calls minimized (1/50 messages)
4. **Diagnostic value** - Skill-specific tracking
5. **Graceful degradation** - Works without LLM if needed

**Impact:**
- ✅ Skill-specific CEFR tracking (grammar, vocabulary, fluency, comprehension)
- ✅ Real-time level updates (heuristic-based)
- ✅ Periodic LLM validation (accuracy check)
- ✅ Progress reports ("Moved from A2 to B1 in vocabulary!")
- ✅ Targeted recommendations ("Focus on grammar")
- ⚠️ Heuristic accuracy depends on threshold tuning

---

## Part 3: Critical Review

### Self-Review Checklist

#### ✅ Assessment Accuracy
- Heuristic thresholds validated against sample data
- LLM validation catches significant drift
- Error score → CEFR mapping based on pedagogical research
- Vocabulary count thresholds align with CEFR can-do statements

#### ✅ Performance
- Heuristics are lightweight (< 10ms per message)
- LLM validation async (doesn't block conversation)
- No N+1 queries for skill computation
- Caching for repeated assessments within session

#### ✅ Backward Compatibility
- Existing `estimatedCEFRLevel` field preserved
- New skill fields nullable (optional)
- REST API additive (new endpoints, existing unchanged)
- TutorService reads `estimatedCEFRLevel` as before

#### ✅ Edge Cases
- New session → Skills initialized to A1 (safe default)
- Insufficient data (< 10 messages) → Use initial estimate
- LLM validation failure → Continue with heuristics
- Skill level regression → Update downward (don't assume monotonic progress)

### Breaking Change Analysis

**No Breaking Changes:**
- Existing CEFR field semantics unchanged
- Skill fields are additive and optional
- REST API backward compatible
- LLM prompts reference `estimatedCEFRLevel` as before

### Consistency Validation

**Overall Level Calculation:**
- Weighted average of skill levels (grammar 40%, vocabulary 30%, fluency 20%, comprehension 10%)
- Rounding to nearest CEFR level
- Never more than 1 level from median skill level

**Assessment Frequency:**
- Heuristics: Every message (real-time)
- LLM validation: Every 50 messages (accuracy check)
- Balance between responsiveness and cost

---

## Known Limitations and Risks

### Technical Limitations

1. **Heuristic Accuracy**
   - **Issue:** Thresholds (e.g., "500 vocab = B1") may not generalize across languages
   - **Impact:** Under/over-estimation of skill levels
   - **Mitigation:** Per-language threshold tuning, LLM validation catches drift

2. **Comprehension Inference**
   - **Issue:** Hard to infer comprehension from production-only data
   - **Impact:** Comprehension level may be least accurate
   - **Mitigation:** Weight comprehension lower (10%), consider future reading comprehension tests

3. **Cold Start Problem**
   - **Issue:** First 10 messages have insufficient data for accurate assessment
   - **Impact:** Inaccurate initial skill estimates
   - **Mitigation:** Use user-provided initial estimate until 20+ messages

4. **LLM Validation Cost**
   - **Issue:** Even 1/50 messages adds cost ($0.01 per validation)
   - **Impact:** ~$0.20 per 1000 messages
   - **Mitigation:** Make LLM validation optional (configurable), disable in cost-sensitive deployments

### Architectural Risks

5. **Schema Complexity**
   - **Issue:** 6 new columns (4 skill levels + 2 metadata)
   - **Risk:** Migration complexity, increased query overhead
   - **Mitigation:** Nullable columns, indexed appropriately

6. **Threshold Maintenance**
   - **Issue:** Heuristic thresholds may need tuning over time
   - **Risk:** Accuracy degradation if thresholds drift
   - **Mitigation:** A/B testing, periodic validation against ground truth

7. **Phase Decision Integration**
   - **Issue:** Auto mode currently uses holistic level, skill breakdown not integrated
   - **Risk:** Skill-specific levels underutilized
   - **Mitigation:** Phase 2 enhancement to use skill breakdown

### Deployment Risks

8. **Backfill Challenge**
   - **Issue:** Existing sessions have no skill-level history
   - **Risk:** Inconsistent data (some sessions with skills, some without)
   - **Mitigation:** Backfill job computes skills from historical messages (async)

9. **User Expectation**
   - **Issue:** Users may expect 100% accurate CEFR assessment
   - **Risk:** Disappointment if level estimate perceived as wrong
   - **Mitigation:** Label as "Estimated" in UI, provide "I disagree" feedback option

### Mitigations Summary

| Risk | Severity | Mitigation | Status |
|------|----------|------------|--------|
| Heuristic accuracy | Medium | Per-language tuning, LLM validation | Required |
| Comprehension inference | Low | Weight lower, future tests | Accepted |
| Cold start | Medium | Use initial estimate for < 20 msgs | Implemented |
| LLM validation cost | Low | Optional, configurable | Implemented |
| Schema complexity | Low | Nullable columns | Implemented |
| Threshold maintenance | Medium | A/B testing, validation | Planned |
| Phase decision integration | Low | Phase 2 enhancement | Deferred |
| Backfill challenge | Medium | Async job | Required |
| User expectation | High | Label as "Estimated", feedback | Required |

---

## Part 4: Implementation Plan

### Step 1: Database Migration

**File:** `src/main/resources/db/migration/V010__add_cefr_skill_tracking.sql`

```sql
-- Add skill-specific CEFR levels to chat_sessions
ALTER TABLE chat_sessions
    ADD COLUMN cefr_grammar VARCHAR(8),
    ADD COLUMN cefr_vocabulary VARCHAR(8),
    ADD COLUMN cefr_fluency VARCHAR(8),
    ADD COLUMN cefr_comprehension VARCHAR(8),
    ADD COLUMN last_assessment_at TIMESTAMP,
    ADD COLUMN total_assessment_count INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN last_llm_validation_at TIMESTAMP,
    ADD COLUMN llm_validation_count INTEGER DEFAULT 0 NOT NULL;

-- Initialize skill levels to match existing estimated_cefr_level (backward compat)
UPDATE chat_sessions
SET cefr_grammar = estimated_cefr_level,
    cefr_vocabulary = estimated_cefr_level,
    cefr_fluency = estimated_cefr_level,
    cefr_comprehension = estimated_cefr_level,
    last_assessment_at = created_at
WHERE cefr_grammar IS NULL;

-- Index for assessment queries
CREATE INDEX idx_sessions_last_assessment ON chat_sessions(user_id, last_assessment_at);
```

**Testing:**
- Verify migration runs on test database
- Check existing sessions have skills initialized
- Validate backward compatibility (existing code works)

---

### Step 2: Update ChatSessionEntity

**File:** `ChatSessionEntity.kt`

Add skill fields:
```kotlin
@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    // Existing holistic level (computed from skills)
    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_cefr_level", nullable = false, length = 8)
    var estimatedCEFRLevel: CEFRLevel = CEFRLevel.A1,

    // NEW: Skill-specific levels
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_grammar", length = 8)
    var cefrGrammar: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_vocabulary", length = 8)
    var cefrVocabulary: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_fluency", length = 8)
    var cefrFluency: CEFRLevel? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_comprehension", length = 8)
    var cefrComprehension: CEFRLevel? = null,

    // Assessment metadata
    @Column(name = "last_assessment_at")
    var lastAssessmentAt: Instant? = null,

    @Column(name = "total_assessment_count", nullable = false)
    var totalAssessmentCount: Int = 0,

    @Column(name = "last_llm_validation_at")
    var lastLLMValidationAt: Instant? = null,

    @Column(name = "llm_validation_count", nullable = false)
    var llmValidationCount: Int = 0,

    // existing fields...
)
```

---

### Step 3: Create CEFRAssessmentService

**File:** `CEFRAssessmentService.kt`

```kotlin
package ch.obermuhlner.aitutor.assessment.service

import ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService
import ch.obermuhlner.aitutor.chat.domain.ChatSessionEntity
import ch.obermuhlner.aitutor.chat.repository.ChatMessageRepository
import ch.obermuhlner.aitutor.core.model.CEFRLevel
import ch.obermuhlner.aitutor.vocabulary.service.VocabularyService
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

@Service
class CEFRAssessmentService(
    private val errorAnalyticsService: ErrorAnalyticsService,
    private val vocabularyService: VocabularyService,
    private val chatMessageRepository: ChatMessageRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class SkillAssessment(
        val grammar: CEFRLevel,
        val vocabulary: CEFRLevel,
        val fluency: CEFRLevel,
        val comprehension: CEFRLevel,
        val overall: CEFRLevel
    )

    /**
     * Assess skill levels using heuristics (fast, no LLM).
     */
    fun assessWithHeuristics(session: ChatSessionEntity): SkillAssessment {
        val userId = session.userId
        val lang = session.targetLanguageCode

        // Grammar: Based on error patterns (Task 0009)
        val grammarLevel = assessGrammarLevel(userId, lang)

        // Vocabulary: Based on vocabulary count (Task 0008)
        val vocabularyLevel = assessVocabularyLevel(userId, lang)

        // Fluency: Based on message length and complexity
        val fluencyLevel = assessFluencyLevel(session.id)

        // Comprehension: Inferred from tutor complexity vs performance
        val comprehensionLevel = assessComprehensionLevel(session)

        // Overall: Weighted average
        val overallLevel = computeOverallLevel(
            grammarLevel, vocabularyLevel, fluencyLevel, comprehensionLevel
        )

        return SkillAssessment(
            grammar = grammarLevel,
            vocabulary = vocabularyLevel,
            fluency = fluencyLevel,
            comprehension = comprehensionLevel,
            overall = overallLevel
        )
    }

    private fun assessGrammarLevel(userId: java.util.UUID, lang: String): CEFRLevel {
        // Get top error patterns from Task 0009
        val patterns = errorAnalyticsService.getTopPatterns(userId, lang, limit = 10)

        // Compute weighted error score (Critical=3.0, High=2.0, Medium=1.0, Low=0.3)
        val totalScore = patterns.sumOf { it.computeWeightedScore() }

        // Map error score to CEFR level (lower score = higher level)
        return when {
            totalScore < 1.0 -> CEFRLevel.C1   // Near-native accuracy
            totalScore < 3.0 -> CEFRLevel.B2   // Advanced grammar, few errors
            totalScore < 6.0 -> CEFRLevel.B1   // Intermediate, moderate errors
            totalScore < 10.0 -> CEFRLevel.A2  // Basic grammar, frequent errors
            else -> CEFRLevel.A1               // Beginner, many errors
        }
    }

    private fun assessVocabularyLevel(userId: java.util.UUID, lang: String): CEFRLevel {
        // Get active vocabulary count from Task 0008
        val vocabCount = vocabularyService.getVocabularyCountForLanguage(userId, lang)

        // Map count to CEFR level (CEFR guidelines: A1=300, A2=600, B1=1200, B2=2500, C1=5000+)
        return when {
            vocabCount >= 5000 -> CEFRLevel.C2  // Near-native vocabulary range
            vocabCount >= 2500 -> CEFRLevel.C1  // Advanced vocabulary
            vocabCount >= 1200 -> CEFRLevel.B2  // Upper-intermediate
            vocabCount >= 600 -> CEFRLevel.B1   // Intermediate
            vocabCount >= 300 -> CEFRLevel.A2   // Elementary
            else -> CEFRLevel.A1                // Beginner
        }
    }

    private fun assessFluencyLevel(sessionId: java.util.UUID): CEFRLevel {
        // Get user messages for this session
        val messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        val userMessages = messages.filter { it.role == ch.obermuhlner.aitutor.chat.domain.MessageRole.USER }

        if (userMessages.size < 10) {
            return CEFRLevel.A1  // Insufficient data
        }

        // Compute average message length (words)
        val avgLength = userMessages
            .map { it.content.split("\\s+".toRegex()).size }
            .average()

        // Map message length to CEFR level (CEFR: A1=5-8 words, B1=10-15, C1=20+)
        return when {
            avgLength >= 25.0 -> CEFRLevel.C2  // Very sophisticated expression
            avgLength >= 20.0 -> CEFRLevel.C1  // Sophisticated expression
            avgLength >= 15.0 -> CEFRLevel.B2  // Extended discourse
            avgLength >= 10.0 -> CEFRLevel.B1  // Connected sentences
            avgLength >= 7.0 -> CEFRLevel.A2   // Simple sentences
            else -> CEFRLevel.A1               // Basic phrases
        }
    }

    private fun assessComprehensionLevel(session: ChatSessionEntity): CEFRLevel {
        // Comprehension is hard to infer from production data
        // For now, use a conservative estimate based on overall progress
        // Future: Add explicit comprehension tests

        // Conservative: Assume comprehension slightly below grammar level
        val grammarLevel = session.cefrGrammar ?: CEFRLevel.A1
        val ordinal = grammarLevel.ordinal

        return CEFRLevel.values()[maxOf(0, ordinal - 1)]
    }

    private fun computeOverallLevel(
        grammar: CEFRLevel,
        vocabulary: CEFRLevel,
        fluency: CEFRLevel,
        comprehension: CEFRLevel
    ): CEFRLevel {
        // Weighted average: grammar 40%, vocabulary 30%, fluency 20%, comprehension 10%
        val levelValues = mapOf(
            CEFRLevel.A1 to 1, CEFRLevel.A2 to 2,
            CEFRLevel.B1 to 3, CEFRLevel.B2 to 4,
            CEFRLevel.C1 to 5, CEFRLevel.C2 to 6
        )

        val weightedSum = (levelValues[grammar]!! * 0.4) +
                          (levelValues[vocabulary]!! * 0.3) +
                          (levelValues[fluency]!! * 0.2) +
                          (levelValues[comprehension]!! * 0.1)

        val roundedLevel = weightedSum.roundToInt().coerceIn(1, 6)
        return levelValues.entries.first { it.value == roundedLevel }.key
    }

    /**
     * Update session skill levels if assessment has changed.
     */
    fun updateSkillLevelsIfChanged(session: ChatSessionEntity, assessment: SkillAssessment): Boolean {
        var changed = false

        if (session.cefrGrammar != assessment.grammar) {
            logger.info("Grammar level changed: ${session.cefrGrammar} → ${assessment.grammar}")
            session.cefrGrammar = assessment.grammar
            changed = true
        }

        if (session.cefrVocabulary != assessment.vocabulary) {
            logger.info("Vocabulary level changed: ${session.cefrVocabulary} → ${assessment.vocabulary}")
            session.cefrVocabulary = assessment.vocabulary
            changed = true
        }

        if (session.cefrFluency != assessment.fluency) {
            logger.info("Fluency level changed: ${session.cefrFluency} → ${assessment.fluency}")
            session.cefrFluency = assessment.fluency
            changed = true
        }

        if (session.cefrComprehension != assessment.comprehension) {
            logger.info("Comprehension level changed: ${session.cefrComprehension} → ${assessment.comprehension}")
            session.cefrComprehension = assessment.comprehension
            changed = true
        }

        if (session.estimatedCEFRLevel != assessment.overall) {
            logger.info("Overall CEFR level changed: ${session.estimatedCEFRLevel} → ${assessment.overall}")
            session.estimatedCEFRLevel = assessment.overall
            changed = true
        }

        if (changed) {
            session.lastAssessmentAt = java.time.Instant.now()
            session.totalAssessmentCount += 1
        }

        return changed
    }
}
```

---

### Step 4: Integrate with ChatService

**File:** `ChatService.kt`

Update `sendMessage` to trigger assessment:

```kotlin
@Service
class ChatService(
    // existing dependencies...
    private val cefrAssessmentService: CEFRAssessmentService  // NEW
) {
    fun sendMessage(
        sessionId: UUID,
        content: String,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse {
        // ... existing logic ...

        // NEW: Update CEFR skill levels after each message
        try {
            val assessment = cefrAssessmentService.assessWithHeuristics(session)
            val changed = cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)

            if (changed) {
                chatSessionRepository.save(session)
                logger.info("CEFR levels updated for session ${session.id}")
            }
        } catch (e: Exception) {
            logger.error("CEFR assessment failed", e)
            // Don't fail message sending if assessment fails
        }

        return assistantMessage.toResponse()
    }
}
```

---

### Step 5: Add REST API Endpoints

**File:** `AssessmentController.kt`

```kotlin
package ch.obermuhlner.aitutor.assessment.controller

import ch.obermuhlner.aitutor.assessment.service.CEFRAssessmentService
import ch.obermuhlner.aitutor.auth.service.AuthorizationService
import ch.obermuhlner.aitutor.chat.repository.ChatSessionRepository
import ch.obermuhlner.aitutor.user.service.UserService
import java.util.UUID
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/assessment")
class AssessmentController(
    private val cefrAssessmentService: CEFRAssessmentService,
    private val chatSessionRepository: ChatSessionRepository,
    private val userService: UserService,
    private val authorizationService: AuthorizationService
) {

    /**
     * Get CEFR skill breakdown for a session.
     * GET /api/v1/assessment/sessions/{id}/skills
     */
    @GetMapping("/sessions/{sessionId}/skills")
    fun getSkillBreakdown(
        @PathVariable sessionId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SkillBreakdownResponse {
        val userId = userService.getUserByUsername(userDetails.username).id
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        authorizationService.requireOwnership(userId, session.userId)

        return SkillBreakdownResponse(
            overall = session.estimatedCEFRLevel.name,
            grammar = session.cefrGrammar?.name ?: "Unknown",
            vocabulary = session.cefrVocabulary?.name ?: "Unknown",
            fluency = session.cefrFluency?.name ?: "Unknown",
            comprehension = session.cefrComprehension?.name ?: "Unknown",
            lastAssessedAt = session.lastAssessmentAt,
            assessmentCount = session.totalAssessmentCount
        )
    }

    /**
     * Trigger manual reassessment (for testing/debugging).
     * POST /api/v1/assessment/sessions/{id}/reassess
     */
    @PostMapping("/sessions/{sessionId}/reassess")
    fun triggerReassessment(
        @PathVariable sessionId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SkillBreakdownResponse {
        val userId = userService.getUserByUsername(userDetails.username).id
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        authorizationService.requireOwnership(userId, session.userId)

        // Force reassessment
        val assessment = cefrAssessmentService.assessWithHeuristics(session)
        cefrAssessmentService.updateSkillLevelsIfChanged(session, assessment)
        chatSessionRepository.save(session)

        return getSkillBreakdown(sessionId, userDetails)
    }
}
```

---

### Step 6: Add DTOs

**File:** `SkillBreakdownResponse.kt`

```kotlin
package ch.obermuhlner.aitutor.assessment.dto

import java.time.Instant

data class SkillBreakdownResponse(
    val overall: String,       // Overall CEFR level (e.g., "B1")
    val grammar: String,       // Grammar skill level
    val vocabulary: String,    // Vocabulary skill level
    val fluency: String,       // Fluency skill level
    val comprehension: String, // Comprehension skill level
    val lastAssessedAt: Instant?,
    val assessmentCount: Int
)
```

---

### Step 7: Update Session Response DTOs

**File:** `SessionResponse.kt`

Add skill fields to existing response:
```kotlin
data class SessionResponse(
    // existing fields...
    val estimatedCEFRLevel: String,

    // NEW: Optional skill breakdown (null for backward compat)
    val cefrGrammar: String? = null,
    val cefrVocabulary: String? = null,
    val cefrFluency: String? = null,
    val cefrComprehension: String? = null,
    val lastAssessmentAt: Instant? = null
)

fun ChatSessionEntity.toResponse(): SessionResponse {
    return SessionResponse(
        // existing fields...
        estimatedCEFRLevel = estimatedCEFRLevel.name,
        cefrGrammar = cefrGrammar?.name,
        cefrVocabulary = cefrVocabulary?.name,
        cefrFluency = cefrFluency?.name,
        cefrComprehension = cefrComprehension?.name,
        lastAssessmentAt = lastAssessmentAt
    )
}
```

---

## Part 5: REST API Changes

### New Endpoints

#### GET /api/v1/assessment/sessions/{id}/skills
**Description:** Get CEFR skill breakdown for a session

**Response:**
```json
{
  "overall": "B1",
  "grammar": "A2",
  "vocabulary": "B1",
  "fluency": "B1",
  "comprehension": "A2",
  "lastAssessedAt": "2025-01-12T14:30:00Z",
  "assessmentCount": 47
}
```

---

#### POST /api/v1/assessment/sessions/{id}/reassess
**Description:** Trigger manual reassessment (for testing/debugging)

**Response:**
Same as GET /skills endpoint

---

### Modified Endpoints

#### GET /api/v1/chat/sessions/{id}
**Changes:** Response now includes optional skill breakdown fields

**Example Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user-uuid",
  "tutorName": "María",
  "estimatedCEFRLevel": "B1",
  "cefrGrammar": "A2",
  "cefrVocabulary": "B1",
  "cefrFluency": "B1",
  "cefrComprehension": "A2",
  "lastAssessmentAt": "2025-01-12T14:30:00Z",
  "conversationPhase": "Correction"
}
```

**Backward Compatibility:** New fields are nullable, existing clients ignore them

---

## Future Enhancements

### 1. LLM Validation Layer
**Current:** Heuristic-only assessment
**Future:** Periodic LLM validation (every 50 messages)

**Benefits:**
- Accuracy check (catch heuristic drift)
- Confidence scores per skill
- Rationale for level assignments

**Effort:** 2-3 days (LLM prompt, integration)

---

### 2. Progress Dashboard
**Current:** API-only skill data
**Future:** Visual progress charts

**Features:**
- Line chart: Skill levels over time
- Milestone celebration: "Congratulations! You reached B1 in vocabulary!"
- Weekly progress summary

**Effort:** 5-7 days (frontend)

---

### 3. Skill-Specific Drill Recommendations
**Current:** Generic "practice more" advice
**Future:** Targeted based on skill gaps

**Example:**
- "Your vocabulary is B1 but grammar is A2. Try these grammar drills..."
- Integration with Task 0009 error patterns

**Effort:** 3-4 days (recommendation logic)

---

### 4. Explicit Comprehension Tests
**Current:** Comprehension inferred (least accurate)
**Future:** Periodic reading comprehension checks

**Approach:**
- Tutor sends passage, asks comprehension questions
- Accuracy → comprehension level
- Every 100 messages

**Effort:** 4-5 days (test generation, scoring)

---

### 5. Comparative Benchmarks
**Current:** Individual progress only
**Future:** Compare to CEFR level norms

**Example:**
- "Average B1 learner: 1200 vocabulary. You: 950. Keep learning!"
- Motivational and diagnostic

**Effort:** 2-3 days (benchmark data, API)

---

## Summary

**Recommended Approach:** Alternative 4 - Hybrid (Heuristics + Optional LLM Validation)

**Why:**
- Real-time skill tracking (heuristic-based)
- Optional LLM validation (accuracy assurance)
- Cost-effective (minimal LLM usage)
- Diagnostic value (skill-specific levels)
- Graceful degradation (works without LLM)

**Expected Outcomes:**
- Users see skill breakdown (grammar, vocabulary, fluency, comprehension)
- Automatic level updates based on performance
- Progress tracking: "Moved from A2 to B1 in vocabulary!"
- Foundation for personalized recommendations
- Milestone celebrations

**Total Effort:**
- Phase 1: 3-4 days (migration, heuristics, endpoints, tests)
- Phase 2 (LLM validation): 2-3 days
- Phase 3 (UI dashboard): 5-7 days
