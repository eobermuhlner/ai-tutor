# Task 0007: Prompt System Improvements

## Problem Statement

The current prompt system in `TutorService` has pedagogical strengths but suffers from LLM context quality issues that reduce effectiveness and waste tokens:

1. **Fragmented system messages** - Three separate `SystemMessage` objects reduce LLM attention coherence
2. **Missing phase transition reasoning** - LLM doesn't understand WHY Auto mode selected current phase
3. **No topic history context** - LLM can't see past topics or turn count for intelligent topic steering
4. **No language-specific metadata** - Generic advice instead of language-tailored scaffolding
5. **Verbose developer prompts** - 33 lines of JSON guidance when schema should be self-documenting
6. **Redundant vocabulary guidance** - Repeats data already present in vocabulary context
7. **Generic summarization prompts** - "Preserve pedagogical context" undefined, leading to bloated summaries

## Phase 1: Analysis

### Current Architecture

#### Prompt Assembly (TutorService.kt:68-123)
```kotlin
// Currently creates 3 separate system messages
val systemMessages = listOf(
    SystemMessage(systemPrompt + phasePrompt),  // Role + phase behavior
    SystemMessage(developerPrompt),              // JSON schema rules
    SystemMessage(conversationState.toString()), // Raw data dump
)
```

#### Key Components
- **TutorService** - Orchestrates prompt assembly and AI chat calls
- **ConversationState** - Holds phase, CEFR level, current topic (no history/reasoning)
- **TopicDecisionService** - Hysteresis logic for topic stability (min 3 turns, max 12)
- **PhaseDecisionService** - Severity-weighted error scoring for Auto phase selection
- **MessageCompactionService** - Token budget management with progressive summarization
- **LanguageService** - Simple language code → name mapping (no metadata)
- **LanguageMetadata** - Catalog data with difficulty, but NOT used in prompts

#### Data Flow
1. TutorService receives conversation state and messages
2. Assembles 3 separate system messages
3. MessageCompactionService compacts history
4. AiChatService sends to OpenAI
5. Response parsed for corrections, vocabulary, state updates

### Constraints

**MUST preserve:**
- Existing REST API contracts (no breaking changes)
- Database schema (ChatSessionEntity structure)
- Pedagogical phase system (Free/Correction/Drill/Auto)
- Teaching style system (Reactive/Guided/Directive)
- Topic hysteresis logic
- Error severity classification
- Progressive summarization system

**MUST NOT:**
- Break existing tests
- Change JSON schema for LLM responses
- Modify vocabulary tracking behavior
- Alter phase decision algorithms

### What Needs to Change

1. **TutorService.kt:107-111** - Consolidate into single coherent system message
2. **ConversationState** - Add fields for phase reasoning and topic context
3. **application.yml prompts** - Reduce developer prompt, improve summarization specificity
4. **TutorService** - Add language metadata injection from catalog
5. **TopicDecisionService** - Return decision metadata (reason, eligibility status)
6. **PhaseDecisionService** - Return decision metadata (reason, severity scores)

## Phase 2: Solution Design

### Alternative 1: Minimal Consolidation (Low Complexity)

**Approach:**
- Merge 3 system messages into 1 in TutorService
- Format conversationState as structured text instead of toString()
- No data model changes, no new fields

**Pros:**
- Minimal code changes (~20 LOC)
- No database migrations
- Low risk, easy rollback
- Immediate token efficiency gains

**Cons:**
- Doesn't solve phase/topic reasoning visibility
- No language metadata injection
- Misses pedagogical improvements

**Complexity:** 1 file changed, ~20 LOC, no migrations
**Risk:** Low

---

### Alternative 2: Enhanced Context with Metadata Injection (Medium Complexity)

**Approach:**
- Consolidate system messages (Alternative 1)
- Add phase/topic decision metadata to prompt formatting
- Inject language metadata from LanguageMetadata catalog
- Refactor developer/vocabulary prompts for conciseness
- Keep existing data models unchanged

**Pros:**
- Significant LLM context quality improvement
- Language-specific scaffolding enabled
- Better pedagogical signals
- No database changes required

**Cons:**
- Requires coordination between multiple services
- More complex prompt formatting logic
- Needs thorough testing of decision services

**Complexity:** 3-4 files changed (~150 LOC), no migrations
**Risk:** Medium

---

### Alternative 3: Full State Enrichment with Persistence (High Complexity)

**Approach:**
- All improvements from Alternative 2
- Add `phaseDecisionReason`, `phaseDecisionScore`, `topicTurnCount`, `topicEligibilityStatus` to ChatSessionEntity
- Persist decision metadata for auditing and UI display
- Expose decision reasoning via REST API

**Pros:**
- Complete solution to all identified issues
- Enables UI to show "why this phase?" to users
- Audit trail for pedagogical decisions
- Future-proof for analytics

**Cons:**
- Database migration required
- REST API changes (new response fields)
- Higher testing burden
- Longer implementation time

**Complexity:** 6-8 files changed (~300 LOC), 1 migration, API updates
**Risk:** High

---

### Alternative 4: Hybrid - Enhanced Context + Optional Persistence (Recommended)

**Approach:**
- Phase 1: Implement Alternative 2 (immediate LLM improvements)
- Phase 2: Add persistence layer (Alternative 3) as optional enhancement
- Decouple prompt improvements from data model changes

**Pros:**
- Incremental delivery - quick wins first
- Low-risk rollout path
- Easier testing and validation
- Flexibility to defer persistence if needed

**Cons:**
- Two-phase implementation
- Some code revisited in Phase 2

**Complexity:** Phase 1: 3-4 files (~150 LOC), Phase 2: +4 files (~150 LOC) + migration
**Risk:** Low (Phase 1), Medium (Phase 2)

---

### Alternative 5: Configuration-Only Improvements (Lowest Complexity)

**Approach:**
- Only modify application.yml prompts
- Reduce developer prompt verbosity
- Improve summarization prompt specificity
- No code changes to TutorService

**Pros:**
- Zero code changes
- Can deploy via config update only
- Instant rollback possible
- No testing burden

**Cons:**
- Doesn't fix system message fragmentation
- Doesn't add phase/topic reasoning
- Doesn't inject language metadata
- Limited impact on core issues

**Complexity:** 1 file changed (application.yml), ~50 lines modified
**Risk:** Minimal

---

## Recommended Solution: Alternative 4 (Hybrid Approach)

**Rationale:**
1. **Quick wins** - Alternative 2 improvements deliverable in 1-2 days
2. **Low risk** - No database changes in Phase 1, easy rollback
3. **Significant impact** - Addresses 5 of 7 identified issues immediately
4. **Flexibility** - Phase 2 can be deferred or cancelled based on Phase 1 results
5. **Testing-friendly** - Can validate Phase 1 LLM improvements before committing to persistence

**Phase 1 Impact:**
- ✅ Fixes system message fragmentation
- ✅ Adds phase transition reasoning (ephemeral)
- ✅ Adds topic history context (ephemeral)
- ✅ Injects language metadata
- ✅ Reduces developer prompt verbosity
- ⚠️ Vocabulary guidance remains (minor issue, low priority)
- ⚠️ Summarization improvements (low risk to refine)

**Phase 2 Impact (optional):**
- ✅ Persists decision metadata for auditing
- ✅ Enables UI features ("Why this phase?")
- ✅ Provides analytics foundation

## Phase 3: Critical Review

### Self-Review Checklist

#### ✅ Imports and Dependencies
- TutorService needs LanguageMetadata injection (via LanguageConfig bean)
- No new external dependencies required
- All decision services already return required data

#### ✅ Method Signatures
- `PhaseDecisionService.decidePhase()` already returns ConversationPhase
- `TopicDecisionService.decideTopic()` already returns String?
- Need new methods: `getDecisionReason()` or return wrapper objects

#### ✅ Synchronous Blocking
- All prompt assembly is synchronous (acceptable - fast string operations)
- No new blocking operations introduced
- Progressive summarization remains async

#### ✅ Configuration Conflicts
- No changes to Spring AI configuration
- application.yml prompts are hot-reloadable (dev mode)
- No conflict with existing tutor/catalog properties

#### ✅ Database Concerns (Phase 1)
- No migrations required for Phase 1
- Existing ChatSessionEntity columns sufficient
- No schema conflicts

### Breaking Change Analysis

**Phase 1 - No Breaking Changes:**
- REST API contracts unchanged
- Database schema unchanged
- Existing tests pass (prompt assembly refactored but behavior equivalent)

**Phase 2 - Backward Compatible Changes:**
- New optional fields in ChatSessionEntity (nullable)
- New optional fields in SessionResponse DTOs (clients ignore unknown fields)
- No removal of existing fields or endpoints

### Consistency Validation

**Prompt Assembly:**
- Single SystemMessage contains all context (coherent flow)
- Phase-specific prompts remain modular (injected at assembly time)
- Teaching style guidance remains modular

**Decision Metadata:**
- Phase reasoning format: "High error severity (score: 7.2) - switching to Drill mode"
- Topic status format: "Turn 8/12 on 'travel' - eligible for change"
- Consistent with conversational tone

**Language Metadata:**
- Format: "Target Language: Spanish (Spain) | Difficulty: Easy | Key Challenges: gendered nouns, verb conjugation"
- Sourced from LanguageMetadata via catalog properties

## Phase 4: Implementation Plan

### Phase 4.1: Immediate Improvements (Recommended First)

#### Step 1: Enhance Decision Services to Return Metadata

**File:** `PhaseDecisionService.kt`

```kotlin
data class PhaseDecision(
    val phase: ConversationPhase,
    val reason: String,
    val severityScore: Double
)

// Update decidePhase to return PhaseDecision
fun decidePhaseWithReason(
    currentPhase: ConversationPhase,
    recentMessages: List<ChatMessageEntity>
): PhaseDecision {
    val phase = decidePhase(currentPhase, recentMessages) // existing logic
    val score = calculateSeverityScore(recentMessages.filter { it.role == MessageRole.USER }.takeLast(5), recentMessages)

    val reason = when (phase) {
        ConversationPhase.Free -> "Low error rate (score: %.1f) - building fluency confidence".format(score)
        ConversationPhase.Correction -> "Balanced approach - tracking errors for learner awareness"
        ConversationPhase.Drill -> "High error severity (score: %.1f) - explicit practice needed".format(score)
        ConversationPhase.Auto -> "Dynamic phase selection based on learner patterns"
    }

    return PhaseDecision(phase, reason, score)
}
```

**File:** `TopicDecisionService.kt`

```kotlin
data class TopicDecision(
    val topic: String?,
    val turnCount: Int,
    val eligibilityStatus: String,
    val pastTopics: List<String>
)

fun decideTopicWithMetadata(
    currentTopic: String?,
    llmProposedTopic: String?,
    recentMessages: List<ChatMessageEntity>,
    pastTopicsJson: String? = null
): TopicDecision {
    val validatedTopic = decideTopic(currentTopic, llmProposedTopic, recentMessages, pastTopicsJson)
    val turnCount = countTurnsInRecentMessages(recentMessages)
    val pastTopics = getPastTopicsFromJson(pastTopicsJson)

    val eligibility = when {
        currentTopic == null -> "Free conversation - establishing topic"
        turnCount < config.minTurnsBeforeChange -> "Topic locked (turn $turnCount/${config.minTurnsBeforeChange})"
        turnCount >= config.maxTurnsForStagnation -> "Topic stale (turn $turnCount/${config.maxTurnsForStagnation}) - change encouraged"
        else -> "Topic active (turn $turnCount) - stable conversation"
    }

    return TopicDecision(validatedTopic, turnCount, eligibility, pastTopics)
}
```

**Testing:** Update existing tests to cover new wrapper objects

---

#### Step 2: Add Language Metadata Injection

**File:** `TutorService.kt`

Add constructor dependency:
```kotlin
class TutorService(
    // existing dependencies...
    private val supportedLanguages: Map<String, LanguageMetadata>
) {
```

Add helper method:
```kotlin
private fun buildLanguageMetadataPrompt(languageCode: String): String {
    val metadata = supportedLanguages[languageCode]
    return if (metadata != null) {
        """
        Target Language Features:
        - Difficulty: ${metadata.difficulty}
        - Writing System: ${extractWritingSystem(languageCode)}
        - Key Challenges: ${extractKeyChallenges(languageCode)}
        """.trimIndent()
    } else {
        "" // Fallback for languages not in catalog
    }
}

private fun extractWritingSystem(code: String): String = when {
    code.startsWith("ja") -> "Hiragana, Katakana, Kanji"
    code.startsWith("zh") -> "Chinese characters (Hanzi)"
    code.startsWith("ru") -> "Cyrillic"
    code.startsWith("ar") -> "Arabic script (right-to-left)"
    code.startsWith("el") -> "Greek"
    code.startsWith("he") -> "Hebrew (right-to-left)"
    else -> "Latin alphabet"
}

private fun extractKeyChallenges(code: String): String = when {
    code.startsWith("es") -> "gendered nouns, verb conjugation, subjunctive mood"
    code.startsWith("de") -> "noun cases (4), gendered articles, verb position"
    code.startsWith("fr") -> "gendered nouns, liaison, verb conjugation"
    code.startsWith("ja") -> "3 writing systems, particles, honorific levels"
    code.startsWith("ru") -> "6-case system, aspect pairs, Cyrillic"
    code.startsWith("zh") -> "tones, characters, measure words"
    code.startsWith("ar") -> "root-pattern morphology, right-to-left, verb forms"
    else -> "vocabulary acquisition, grammar patterns"
}
```

**Testing:** Add unit test for language metadata prompt generation

---

#### Step 3: Consolidate System Message Assembly

**File:** `TutorService.kt:68-123`

Replace existing system message logic:

```kotlin
// Get decision metadata
val phaseDecision = if (conversationState.phase == ConversationPhase.Auto) {
    phaseDecisionService.decidePhaseWithReason(conversationState.phase, messages.filterIsInstance<ChatMessageEntity>())
} else {
    PhaseDecision(conversationState.phase, "User-selected phase", 0.0)
}

val topicDecision = topicDecisionService.decideTopicWithMetadata(
    conversationState.currentTopic,
    conversationState.currentTopic, // LLM not yet responded, use current
    messages.filterIsInstance<ChatMessageEntity>(),
    sessionEntity?.pastTopicsJson
)

// Build consolidated system prompt
val consolidatedSystemPrompt = buildString {
    // Base system prompt (role, persona, languages)
    append(PromptTemplate(systemPromptTemplate).render(mapOf(
        "targetLanguage" to targetLanguage,
        "targetLanguageCode" to targetLanguageCode,
        "sourceLanguage" to sourceLanguage,
        "sourceLanguageCode" to sourceLanguageCode,
        "tutorName" to tutor.name,
        "tutorPersona" to tutor.persona,
        "tutorDomain" to tutor.domain,
        "vocabularyGuidance" to vocabularyGuidance,
        "teachingStyleGuidance" to teachingStyleGuidance
    )))

    append("\n\n")

    // Phase-specific behavior
    val phasePrompt = when (phaseDecision.phase) {
        ConversationPhase.Free -> phaseFreePrompt
        ConversationPhase.Correction -> phaseCorrectionPrompt
        ConversationPhase.Drill -> phaseDrillPrompt
        ConversationPhase.Auto -> phaseCorrectionPrompt // Auto resolved to actual phase
    }
    append(phasePrompt)

    append("\n\n")

    // Developer rules (JSON schema)
    append(developerPrompt)

    append("\n\n")

    // Session Context (structured, not toString())
    append("=== Current Session Context ===\n")
    append("Phase: ${phaseDecision.phase.name} (${phaseDecision.reason})\n")
    append("CEFR Level: ${conversationState.estimatedCEFRLevel.name}\n")
    append("Topic: ${topicDecision.topic ?: "Free conversation"} (${topicDecision.eligibilityStatus})\n")
    if (topicDecision.pastTopics.isNotEmpty()) {
        append("Recent Topics: ${topicDecision.pastTopics.takeLast(3).joinToString(", ")}\n")
    }

    append("\n")

    // Language metadata
    append(buildLanguageMetadataPrompt(targetLanguageCode))
}

val systemMessages = listOf(SystemMessage(consolidatedSystemPrompt))
```

**Testing:**
- Unit test for consolidated prompt structure
- Integration test comparing old vs new prompt token count
- Verify phase-specific prompts still injected correctly

---

#### Step 4: Refactor application.yml Prompts

**File:** `application.yml:635-667`

Reduce developer prompt:
```yaml
developer: |
  JSON Response Rules:
  - Corrections apply ONLY to most recent user message (never previous turns)
  - `span` values must be verbatim substrings from current message
  - Set `estimatedCEFRLevel` (A1-C2) based on last 3-5 turns
  - Include `conceptName` for vocabulary/wordCards (e.g., "apple", "running", "coffee-cup")

  Error Severity (context-aware):
  - Critical (3.0): Comprehension blocked entirely
  - High (2.0): Global error, native speaker confused
  - Medium (1.0): Grammar issue, meaning clear from context
  - Low (0.3): Minor issue OR chat-acceptable (missing accents/caps/punctuation, casual norms)

  Typography Rules:
  - Missing accents in casual chat → Low or ignore
  - No capitalization/punctuation → Low or ignore (chat norm)
  - Ask: "Would native speaker do this in texting?" → Yes = Low/ignore
```

Improve summarization prompt:
```yaml
progressive:
  enabled: true
  chunk-size: 10
  chunk-token-threshold: 5000
  prompt: |
    Summarize this {targetLanguage} tutoring segment for context retention.

    Include ONLY:
    - Topics discussed (bullet list, no details)
    - Learner errors by type (e.g., "Tense: 3x past/present confusion")
    - New vocabulary (word pairs: {targetLanguage} = {sourceLanguage})

    Exclude: tutor behavior, explanations, example sentences, emotional tone
    Format: Bullets or short phrases. Target: {targetWords} words ({targetTokens} tokens).
```

**Testing:** Manual review of generated summaries for conciseness

---

### Phase 4.2: Optional Persistence Layer (Future Enhancement)

**Deferred** - Only implement if Phase 1 proves successful and there's demand for:
- UI displaying "Why this phase?"
- Analytics on phase transition patterns
- Audit trail for pedagogical decisions

**Changes Required:**
1. Database migration adding columns to `chat_sessions`:
   - `phase_decision_reason` TEXT
   - `phase_severity_score` DOUBLE
   - `topic_turn_count` INT
   - `topic_eligibility_status` VARCHAR(64)

2. Update `ChatSessionEntity` with new fields

3. Update `SessionResponse` DTOs with optional metadata

4. Modify `ChatService` to persist decision metadata

5. Add REST endpoint to query decision history

---

### Testing Strategy

#### Unit Tests
- `PhaseDecisionService.decidePhaseWithReason()` - verify reason strings
- `TopicDecisionService.decideTopicWithMetadata()` - verify eligibility logic
- `TutorService.buildLanguageMetadataPrompt()` - verify metadata formatting
- `TutorService.buildConsolidatedSystemPrompt()` - verify single message structure

#### Integration Tests
- `TutorServiceTest` - compare token counts (expect 10-15% reduction)
- `ChatServiceTest` - verify end-to-end session creation still works
- `MessageCompactionServiceTest` - verify compatibility with new prompt structure

#### Manual Testing
- Create test session in each phase (Free/Correction/Drill/Auto)
- Verify LLM responses show improved context awareness
- Check vocabulary reinforcement behavior unchanged
- Test topic change suggestions respect hysteresis

---

### Rollout Plan

#### Phase 1: Non-Production Testing (Week 1)
1. Deploy to dev environment
2. Run integration test suite
3. Manual testing with 5-10 sample conversations
4. Monitor LLM response quality and token usage

#### Phase 2: Canary Release (Week 2)
1. Deploy to 10% of production sessions
2. Monitor error rates and response times
3. Compare token usage metrics vs baseline
4. Gather qualitative feedback from demo users

#### Phase 3: Full Rollout (Week 3)
1. Deploy to 100% of production
2. Monitor for 1 week
3. Document token savings and quality improvements

---

### Monitoring Plan

**Metrics to Track:**
1. **Token Usage**
   - Baseline: Current average tokens per conversation turn
   - Target: 10-15% reduction in system message tokens
   - Metric: `avg(systemMessageTokens)` per session

2. **Response Quality**
   - Baseline: Manual review of 50 conversations (pre-change)
   - Target: Improved context awareness (subjective assessment)
   - Metric: User satisfaction scores (if available)

3. **Error Rates**
   - Baseline: Current API error rate
   - Target: No increase in errors
   - Metric: `count(apiErrors) / count(totalRequests)`

4. **Phase Transition Appropriateness**
   - Baseline: Manual review of phase transitions
   - Target: Better alignment with learner needs
   - Metric: Qualitative assessment (sample 20 sessions)

**Logging Additions:**
```kotlin
logger.info("System prompt assembled: ${systemMessages.size} message(s), estimated ${estimateTokens(systemMessages)} tokens")
logger.debug("Phase decision: ${phaseDecision.phase.name} (${phaseDecision.reason}, score: ${phaseDecision.severityScore})")
logger.debug("Topic decision: ${topicDecision.topic} (${topicDecision.eligibilityStatus})")
```

---

### Rollback Strategy

**Phase 1 Rollback (if issues detected):**
1. Revert `TutorService.kt` changes
2. Revert `PhaseDecisionService.kt` changes
3. Revert `TopicDecisionService.kt` changes
4. Revert `application.yml` prompt changes
5. Deploy previous version

**No data loss risk** - Phase 1 has no database changes

**Rollback Triggers:**
- Error rate increase >5%
- Token usage increase (regression)
- LLM response quality degradation
- Test failures in production

---

## Phase 5: REST API Changes

**Phase 1: No API Changes**

All improvements in Phase 1 are internal to prompt assembly. REST API contracts remain identical.

**Phase 2 (Optional Future Enhancement):**

If persistence layer is implemented, add optional fields to existing responses:

### Modified DTOs

```kotlin
// SessionResponse.kt
data class SessionResponse(
    // existing fields...
    val conversationPhase: ConversationPhase,
    val effectivePhase: ConversationPhase?,

    // NEW: Optional metadata (null for backward compatibility)
    val phaseDecisionReason: String? = null,
    val phaseSeverityScore: Double? = null,
    val topicTurnCount: Int? = null,
    val topicEligibilityStatus: String? = null
)
```

### Example Response

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user-123",
  "tutorName": "María",
  "conversationPhase": "Correction",
  "effectivePhase": "Correction",
  "phaseDecisionReason": "Balanced approach - tracking errors for learner awareness",
  "phaseSeverityScore": 2.3,
  "currentTopic": "travel",
  "topicTurnCount": 5,
  "topicEligibilityStatus": "Topic active (turn 5) - stable conversation"
}
```

**Backward Compatibility:**
- New fields are nullable
- Existing clients ignore unknown fields (JSON deserialization)
- No breaking changes to existing endpoints

---

## Summary

**Recommended Approach:** Alternative 4 - Phase 1 Only (Immediate Improvements)

**Why:**
- Low risk, high impact
- No database migrations
- Quick delivery (1-2 days)
- Addresses 5 of 7 identified issues
- Easy rollback path

**Expected Outcomes:**
- 10-15% reduction in system message tokens
- Improved LLM context awareness
- Better language-specific scaffolding
- More coherent pedagogical signals
- No breaking changes or data migrations

**Phase 2 (Optional):**
- Defer until Phase 1 validated
- Only implement if user-facing features require decision metadata
- Clear separation of concerns