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

**Implementation Options for Metadata Passing:**

**Option A - Method Parameters:**
```kotlin
// TutorService.respond() with new parameters
fun respond(
    tutor: Tutor,
    conversationState: ConversationState,
    userId: UUID,
    messages: List<Message>,
    sessionId: UUID? = null,
    phaseReason: String? = null,  // NEW
    topicMetadata: String? = null, // NEW
    pastTopics: List<String> = emptyList(), // NEW
    onReplyChunk: (String) -> Unit = {}
)
```

**Option B - Enriched ConversationState (Recommended):**
```kotlin
// Extend ConversationState data class
data class ConversationState(
    val phase: ConversationPhase,
    val estimatedCEFRLevel: CEFRLevel,
    val currentTopic: String?,

    // NEW: Optional metadata fields (backward compatible)
    val phaseReason: String? = null,
    val topicEligibilityStatus: String? = null,
    val pastTopics: List<String> = emptyList()
)

// ChatService enriches state before calling TutorService
val enrichedState = conversationState.copy(
    phaseReason = phaseDecision.reason,
    topicEligibilityStatus = topicDecision.eligibilityStatus,
    pastTopics = topicDecision.pastTopics
)

tutorService.respond(tutor, enrichedState, userId, messages, sessionId, onReplyChunk)
```

**Recommendation:** Use Option B (enriched ConversationState) for cleaner API and better cohesion.

**Pros:**
- Significant LLM context quality improvement
- Language-specific scaffolding enabled
- Better pedagogical signals
- No database changes required
- Clean separation: ChatService computes, TutorService formats

**Cons:**
- Requires coordination between multiple services
- More complex prompt formatting logic
- Needs thorough testing of decision services
- ConversationState modification (low risk, backward compatible)

**Complexity:** 3-4 files changed (~150 LOC), no migrations, 1 data class update
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

---

## Architecture Decision: Where to Compute Decision Metadata

### Problem
Decision metadata (phase reasons, topic eligibility) needs to be formatted for LLM prompts. Should this happen in:
- **Option A:** TutorService (prompt assembly layer)
- **Option B:** ChatService (orchestration layer)

### Analysis

**Option A - TutorService computes metadata:**
```kotlin
// TutorService.respond()
val phaseDecision = phaseDecisionService.decidePhaseWithReason(...)
val topicDecision = topicDecisionService.decideTopicWithMetadata(...)
// Use metadata in prompt assembly
```

Pros:
- Metadata close to usage point
- TutorService fully self-contained

Cons:
- Duplicate phase decision logic (ChatService already calls `decidePhase()`)
- TutorService needs session data it doesn't currently access
- Harder to test metadata formatting independently
- Blurs service responsibilities

**Option B - ChatService computes metadata, passes to TutorService:**
```kotlin
// ChatService.sendMessage()
val phaseDecision = phaseDecisionService.decidePhaseWithReason(...)
val topicDecision = topicDecisionService.decideTopicWithMetadata(...)

// Pass metadata to TutorService via parameters or enriched ConversationState
tutorService.respond(..., phaseReason = phaseDecision.reason, topicMetadata = topicDecision)
```

Pros:
- No duplicate decision logic
- ChatService already has session access
- Clear separation: ChatService orchestrates, TutorService formats
- Easier to test metadata independently

Cons:
- More parameters to TutorService (mitigated by enriched ConversationState)
- Metadata travels through extra layer

### Decision: **Option B - ChatService Computes Metadata**

**Rationale:**
1. ChatService already makes phase/topic decisions (lines 245-278)
2. Avoids duplicate decision calls
3. Cleaner separation of concerns
4. Easier to test and maintain

**Implementation:**
```kotlin
// ChatService enriches ConversationState with metadata
val enrichedState = conversationState.copy(
    phaseReason = phaseDecision.reason,
    topicMetadata = topicDecision.eligibilityStatus,
    pastTopics = topicDecision.pastTopics
)

tutorService.respond(tutor, enrichedState, ...)
```

**Impact on Implementation Plan:**
- Step 3 code needs adjustment - remove decision calls from TutorService
- Add metadata fields to ConversationState OR add parameters to `respond()`
- Update ChatService to compute metadata before calling TutorService

---

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

---

## Known Limitations and Risks

### Technical Limitations

1. **Token Estimation Imprecision**
   - **Issue:** No precise token counter available for prompt measurement
   - **Workaround:** Using `length / 4` heuristic (rough approximation)
   - **Impact:** Token reduction metrics are estimates, not exact measurements
   - **Mitigation:** Validate against actual OpenAI API token usage in production

2. **Language Metadata Hardcoding**
   - **Issue:** Key challenge descriptions hardcoded in `extractKeyChallenges()` method
   - **Better:** Externalize to `application.yml` or LanguageMetadata JSON
   - **Impact:** Adding new languages requires code changes, not just config
   - **Future:** Move to catalog-based metadata system

3. **Reason String Localization**
   - **Issue:** Phase/topic decision reasons always in English
   - **Impact:** Doesn't match multilingual UI/API expectations
   - **Future:** Localize reason strings based on user's sourceLanguage

4. **Summarization Prompt Placeholders**
   - **Issue:** `{targetWords}` and `{targetTokens}` placeholders may not exist
   - **Risk:** Prompt rendering failure if placeholders undefined
   - **Mitigation:** Verify before deployment OR use static values

### Architectural Risks

5. **Duplicate Method APIs**
   - **Issue:** `decidePhaseWithReason()` and `decideTopicWithMetadata()` parallel existing methods
   - **Risk:** Code duplication, maintenance burden, API confusion
   - **Mitigation:** Phase 2 migration to single API with wrapper objects

6. **Test Mock Updates**
   - **Issue:** All existing tests mocking decision services need wrapper object updates
   - **Risk:** Test failures if mocks not updated comprehensively
   - **Mitigation:** Run full test suite before deployment

### Deployment Risks

7. **Progressive Summarization Compatibility**
   - **Issue:** Changing summarization prompt may break existing summary chains
   - **Risk:** Loss of pedagogical context in long conversations
   - **Mitigation:** Defer summarization changes to Phase 1.5, test thoroughly

8. **No Baseline Token Measurements**
   - **Issue:** No automated baseline capture before changes
   - **Risk:** Cannot definitively prove 10-15% reduction claim
   - **Mitigation:** Add token measurement integration test

### Mitigations Summary

| Risk | Severity | Mitigation | Status |
|------|----------|------------|--------|
| Token estimation imprecision | Low | Use heuristic + production validation | Accepted |
| Hardcoded language metadata | Low | Future: move to catalog | Deferred |
| Reason string localization | Low | Future: i18n support | Deferred |
| Summarization placeholders | Medium | Verify before deploy OR use static | Required |
| Duplicate APIs | Medium | Phase 2 consolidation | Planned |
| Test mock updates | High | Comprehensive test suite run | Required |
| Summarization compatibility | High | Defer to Phase 1.5 | Implemented |
| No token baseline | Medium | Add integration test | Required |

---

## Phase 4: Implementation Plan

### Phase 4.1: Immediate Improvements (Recommended First)

#### Step 1: Enhance Decision Services to Return Metadata

**File:** `PhaseDecisionService.kt`

**Design Decision:** Create new methods with metadata OR modify existing methods?

**Option A - New parallel methods (recommended for Phase 1):**
- Pros: No breaking changes, easier rollout, can test side-by-side
- Cons: Duplicate code, maintenance burden, confusing API

**Option B - Modify existing methods (recommended for long-term):**
- Pros: Single source of truth, cleaner API, forces migration
- Cons: Breaking change, must update all call sites immediately

**Recommendation:** Use Option A for Phase 1, migrate to Option B in Phase 2.

```kotlin
data class PhaseDecision(
    val phase: ConversationPhase,
    val reason: String,
    val severityScore: Double
)

// New method - returns metadata wrapper (Option A)
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

Add import at top of file:
```kotlin
import ch.obermuhlner.aitutor.core.model.catalog.LanguageMetadata
```

Add constructor dependency:
```kotlin
class TutorService(
    // existing dependencies...
    private val supportedLanguages: Map<String, LanguageMetadata>
) {
```

**Note:** The `supportedLanguages` bean is provided by `LanguageConfig.kt:14`:
```kotlin
@Bean
fun supportedLanguages(): Map<String, LanguageMetadata>
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

**Test Updates Required:**
```kotlin
// TutorServiceTest.kt - Update setup to mock supportedLanguages
private val supportedLanguages = mapOf(
    "es" to LanguageMetadata(
        code = "es",
        nameJson = """{"en": "Spanish"}""",
        flagEmoji = "🇪🇸",
        nativeName = "Español",
        difficulty = Difficulty.Easy,
        descriptionJson = """{"en": "Romance language"}"""
    )
)

// Pass to TutorService constructor in test setup
val tutorService = TutorService(
    aiChatService,
    languageService,
    vocabularyContextService,
    messageCompactionService,
    systemPromptTemplate,
    phaseFreePromptTemplate,
    phaseCorrectionPromptTemplate,
    phaseDrillPromptTemplate,
    developerPromptTemplate,
    vocabularyNoTrackingTemplate,
    vocabularyWithTrackingTemplate,
    teachingStyleReactiveTemplate,
    teachingStyleGuidedTemplate,
    teachingStyleDirectiveTemplate,
    supportedLanguages // NEW
)
```

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

// Note: pastTopicsJson must be passed from ChatService, not accessed here
// TutorService does not have access to sessionEntity
// This decision metadata should be computed in ChatService BEFORE calling TutorService
val topicDecision = topicDecisionService.decideTopicWithMetadata(
    conversationState.currentTopic,
    conversationState.currentTopic, // LLM not yet responded, use current
    messages.filterIsInstance<ChatMessageEntity>(),
    null // TODO: Pass pastTopicsJson from ChatService as parameter
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

**⚠️ IMPORTANT - Placeholder Verification:**
Before deploying this prompt, verify that `{targetWords}` and `{targetTokens}` placeholders are defined in `ProgressiveSummarizationService.kt` or `ConversationSummarizationService.kt`.

**To verify:**
```bash
grep -r "targetWords\|targetTokens" src/main/kotlin/ch/obermuhlner/aitutor/tutor/service/
```

**If placeholders NOT found:**
- Option A: Add placeholder substitution logic to summarization services
- Option B: Use static values: "Target: 50-100 words (200-400 tokens)"
- Option C: Remove target specification entirely

**Recommendation:** Defer summarization prompt changes to Phase 1.5 (separate from core consolidation work) due to complexity of progressive summarization system.

**Testing:** Manual review of generated summaries for conciseness

---

### Phase 4.2: Optional Persistence Layer (Future Enhancement)

**Decision Criteria:** Implement Phase 2 persistence ONLY IF ALL of the following are true:

**Required Pre-Conditions:**
1. ✅ **Phase 1 Success Validated**
   - Token reduction ≥8% confirmed (target 10-15%)
   - No increase in API error rates
   - LLM response quality maintained or improved
   - Zero production incidents related to prompt changes

2. ✅ **Business Justification Exists**
   - Product roadmap includes "Why this phase?" UI feature, OR
   - User feedback explicitly requests phase transition explanations, OR
   - Analytics team requires pedagogical decision tracking

3. ✅ **Engineering Capacity Available**
   - 2-3 days of engineering time available
   - Database migration window scheduled
   - QA resources for API contract testing

**If ANY condition fails, DEFER Phase 2 indefinitely.**

**Use Cases Enabled by Phase 2:**
- UI displaying "Why this phase?" to learners
- Analytics on phase transition patterns over time
- Audit trail for pedagogical decisions
- A/B testing different phase decision thresholds

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

**PhaseDecisionServiceTest:**
```kotlin
@Test
fun `decidePhaseWithReason returns correct reason for Free phase`() {
    val messages = createMessagesWithLowSeverityErrors()
    val result = phaseDecisionService.decidePhaseWithReason(ConversationPhase.Auto, messages)

    assertEquals(ConversationPhase.Free, result.phase)
    assertTrue(result.reason.contains("Low error rate"))
    assertTrue(result.severityScore < 1.0)
}

@Test
fun `decidePhaseWithReason returns correct reason for Drill phase`() {
    val messages = createMessagesWithHighSeverityErrors()
    val result = phaseDecisionService.decidePhaseWithReason(ConversationPhase.Auto, messages)

    assertEquals(ConversationPhase.Drill, result.phase)
    assertTrue(result.reason.contains("High error severity"))
    assertTrue(result.severityScore >= 6.0)
}

@Test
fun `decidePhaseWithReason returns user-selected reason for manual phase`() {
    val messages = emptyList<ChatMessageEntity>()
    val result = phaseDecisionService.decidePhaseWithReason(ConversationPhase.Correction, messages)

    assertEquals(ConversationPhase.Correction, result.phase)
    assertEquals("User-selected phase", result.reason)
}
```

**TopicDecisionServiceTest:**
```kotlin
@Test
fun `decideTopicWithMetadata returns correct eligibility for locked topic`() {
    val messages = createMessages(turnCount = 2)
    val result = topicDecisionService.decideTopicWithMetadata("travel", "food", messages, null)

    assertEquals("travel", result.topic) // LLM proposal rejected
    assertTrue(result.eligibilityStatus.contains("locked"))
    assertEquals(2, result.turnCount)
}

@Test
fun `decideTopicWithMetadata returns correct eligibility for stale topic`() {
    val messages = createMessages(turnCount = 13)
    val result = topicDecisionService.decideTopicWithMetadata("travel", "food", messages, null)

    assertEquals("food", result.topic) // LLM proposal accepted
    assertTrue(result.eligibilityStatus.contains("stale"))
    assertEquals(13, result.turnCount)
}

@Test
fun `decideTopicWithMetadata includes past topics in result`() {
    val pastTopicsJson = """["sports", "weather", "travel"]"""
    val result = topicDecisionService.decideTopicWithMetadata("current", null, emptyList(), pastTopicsJson)

    assertEquals(listOf("sports", "weather", "travel"), result.pastTopics)
}
```

**TutorServiceTest:**
```kotlin
@Test
fun `buildLanguageMetadataPrompt handles missing language gracefully`() {
    val result = tutorService.buildLanguageMetadataPrompt("unknown-language-code")
    assertEquals("", result) // Fallback to empty string
}

@Test
fun `buildLanguageMetadataPrompt formats Spanish metadata correctly`() {
    val result = tutorService.buildLanguageMetadataPrompt("es")

    assertTrue(result.contains("Difficulty: Easy"))
    assertTrue(result.contains("gendered nouns"))
    assertTrue(result.contains("verb conjugation"))
}

@Test
fun `consolidated prompt contains all required sections`() {
    // Test that single SystemMessage includes:
    // - Base system prompt (role, persona, languages)
    // - Phase-specific behavior
    // - Developer rules
    // - Session context
    // - Language metadata
    val messages = tutorService.respond(tutor, conversationState, userId, emptyList())

    // Verify prompt structure (implementation-specific assertions)
}
```

#### Integration Tests

**Token Reduction Validation:**
```kotlin
@Test
fun `consolidated prompt reduces tokens by 10-15% vs fragmented`() {
    // Setup test data
    val tutor = createTestTutor()
    val conversationState = createTestConversationState()
    val userId = UUID.randomUUID()

    // Measure OLD approach (3 separate SystemMessages)
    val oldSystemMessages = listOf(
        SystemMessage(systemPrompt + phasePrompt),
        SystemMessage(developerPrompt),
        SystemMessage(conversationState.toString())
    )
    val oldCharCount = oldSystemMessages.sumOf { it.content.length }
    val oldEstimatedTokens = oldCharCount / 4

    // Measure NEW approach (1 consolidated SystemMessage)
    // Build consolidated prompt using new buildConsolidatedSystemPrompt() logic
    val newSystemMessages = listOf(SystemMessage(buildConsolidatedSystemPrompt(...)))
    val newCharCount = newSystemMessages.sumOf { it.content.length }
    val newEstimatedTokens = newCharCount / 4

    // Verify reduction
    val reduction = (oldEstimatedTokens - newEstimatedTokens).toDouble() / oldEstimatedTokens
    assertTrue(
        reduction >= 0.10,
        "Expected ≥10% reduction, got ${(reduction * 100).roundToInt()}%"
    )
    assertTrue(
        reduction <= 0.20,
        "Reduction too aggressive (${(reduction * 100).roundToInt()}%), may lose context"
    )

    println("Token reduction: ${(reduction * 100).roundToInt()}% (${oldEstimatedTokens} → ${newEstimatedTokens})")
}
```

**End-to-End Tests:**
- `ChatServiceTest` - verify end-to-end session creation still works
- `MessageCompactionServiceTest` - verify compatibility with new prompt structure

**Existing Test Updates:**
```kotlin
// Update mocks in ChatServiceTest to handle new wrapper objects
every {
    phaseDecisionService.decidePhaseWithReason(any(), any())
} returns PhaseDecision(ConversationPhase.Correction, "Balanced approach", 2.5)

every {
    topicDecisionService.decideTopicWithMetadata(any(), any(), any(), any())
} returns TopicDecision("travel", 5, "active", emptyList())
```

#### Manual Testing
- Create test session in each phase (Free/Correction/Drill/Auto)
- Verify LLM responses show improved context awareness
- Check vocabulary reinforcement behavior unchanged
- Test topic change suggestions respect hysteresis

---

### Rollout Plan

#### Phase 1: Non-Production Testing (Week 1)

**Pre-Deployment Validation:**
1. Verify all unit tests pass: `./gradlew test`
2. Check for compilation errors: `./gradlew build`
3. Review consolidated prompt in logs (enable DEBUG level for TutorService)

**YAML Configuration Validation:**
1. Deploy to dev environment
2. Send 10 test messages covering all phases (Free/Correction/Drill/Auto)
3. Verify LLM responses parse correctly (check for JSON schema errors)
4. Monitor logs for exceptions during prompt assembly
5. Confirm metadata (phase reason, topic status) appears in logs

**Functional Testing:**
1. Run integration test suite
2. Manual testing with 5-10 sample conversations
3. Monitor LLM response quality and token usage
4. Compare token usage vs baseline (expect 10-15% reduction)

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
// Note: No precise token counter available, using character length as proxy (rough estimate: chars/4)
val estimatedTokens = systemMessages.sumOf { it.content.length } / 4
logger.info("System prompt assembled: ${systemMessages.size} message(s), ~$estimatedTokens tokens (estimated)")
logger.debug("Phase decision: ${phaseDecision.phase.name} (${phaseDecision.reason}, score: ${phaseDecision.severityScore})")
logger.debug("Topic decision: ${topicDecision.topic} (${topicDecision.eligibilityStatus})")
```

**Alternative (simpler):** Remove token estimation if precision not critical:
```kotlin
logger.info("System prompt assembled: ${systemMessages.size} message(s)")
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