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

## Part 1: Analysis

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

## Part 2: Solution Design

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
- Stage 1: Implement Alternative 2 (immediate LLM improvements)
- Stage 2: Add persistence layer (Alternative 3) as optional enhancement
- Decouple prompt improvements from data model changes

**Pros:**
- Incremental delivery - quick wins first
- Low-risk rollout path
- Easier testing and validation
- Flexibility to defer persistence if needed

**Cons:**
- Two-stage implementation
- Some code revisited in Stage 2

**Complexity:** Stage 1: 3-4 files (~150 LOC), Stage 2: +4 files (~150 LOC) + migration
**Risk:** Low (Stage 1), Medium (Stage 2)

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
2. **Low risk** - No database changes in Stage 1, easy rollback
3. **Significant impact** - Addresses 5 of 7 identified issues immediately
4. **Flexibility** - Stage 2 can be deferred or cancelled based on Stage 1 results
5. **Testing-friendly** - Can validate Stage 1 LLM improvements before committing to persistence

**Stage 1 Impact:**
- ✅ Fixes system message fragmentation
- ✅ Adds phase transition reasoning (ephemeral)
- ✅ Adds topic history context (ephemeral)
- ✅ Injects language metadata
- ✅ Reduces developer prompt verbosity
- ⚠️ Vocabulary guidance remains (minor issue, low priority)
- ⚠️ Summarization improvements (low risk to refine)

**Stage 2 Impact (optional):**
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

**Impact on Implementation Steps:**
- Step 3 code needs adjustment - remove decision calls from TutorService
- Add metadata fields to ConversationState OR add parameters to `respond()`
- Update ChatService to compute metadata before calling TutorService

---

## Part 3: Critical Review

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

#### ✅ Database Concerns (Part 4.1)
- No migrations required for Part 4.1
- Existing ChatSessionEntity columns sufficient
- No schema conflicts

### Breaking Change Analysis

**Part 4.1 - No Breaking Changes:**
- REST API contracts unchanged
- Database schema unchanged
- Existing tests pass (prompt assembly refactored but behavior equivalent)

**Part 4.2 - Backward Compatible Changes:**
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
   - **Mitigation:** Stage 2 migration to single API with wrapper objects

6. **Test Mock Updates**
   - **Issue:** All existing tests mocking decision services need wrapper object updates
   - **Risk:** Test failures if mocks not updated comprehensively
   - **Mitigation:** Run full test suite before deployment

### Deployment Risks

7. **Progressive Summarization Compatibility**
   - **Issue:** Changing summarization prompt may break existing summary chains
   - **Risk:** Loss of pedagogical context in long conversations
   - **Mitigation:** Defer summarization changes to deferred step, test thoroughly

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
| Duplicate APIs | Medium | Stage 2 consolidation | Planned |
| Test mock updates | High | Comprehensive test suite run | Required |
| Summarization compatibility | High | Defer to separate step | Implemented |
| No token baseline | Medium | Add integration test | Required |

---

## Part 4: Implementation Plan

### Part 4.1: Immediate Improvements (Recommended First)

#### Step 1: Enhance Decision Services to Return Metadata

**File:** `PhaseDecisionService.kt`

**Design Decision:** Create new methods with metadata OR modify existing methods?

**Option A - New parallel methods:**
- Pros: No breaking changes, easier rollout
- Cons: Duplicate code, maintenance burden, confusing API, violates "keep it simple"

**Option B - Modify existing methods (RECOMMENDED):**
- Pros: Single source of truth, cleaner API, no confusion
- Cons: Breaking change, must update all call sites (only ChatService.kt:246)

**Recommendation:** Use Option B - modify existing methods to return wrapper objects. The breaking change is minimal (only 1 call site in ChatService) and avoids API confusion.

```kotlin
data class PhaseDecision(
    val phase: ConversationPhase,
    val reason: String,
    val severityScore: Double
)

// MODIFIED existing method - now returns metadata wrapper
fun decidePhase(
    currentPhase: ConversationPhase,
    recentMessages: List<ChatMessageEntity>
): PhaseDecision {
    // Existing phase decision logic (lines 40-83 from PhaseDecisionService.kt)
    val phase = if (currentPhase != ConversationPhase.Auto) {
        currentPhase
    } else {
        // ... existing logic ...
        ConversationPhase.Correction // placeholder for brevity
    }
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

// MODIFIED existing method - now returns metadata wrapper
fun decideTopic(
    currentTopic: String?,
    llmProposedTopic: String?,
    recentMessages: List<ChatMessageEntity>,
    pastTopicsJson: String? = null
): TopicDecision {
    // Existing topic decision logic (lines 49-99 from TopicDecisionService.kt)
    val validatedTopic = applyTopicHysteresis(currentTopic, llmProposedTopic, recentMessages, pastTopicsJson)
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

#### Step 1.5: Update ConversationState Data Class

**File:** `ConversationState.kt`

Add optional metadata fields for passing decision context from ChatService to TutorService:

```kotlin
package ch.obermuhlner.aitutor.tutor.domain

import ch.obermuhlner.aitutor.core.model.CEFRLevel
import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ConversationState(
    @field:JsonPropertyDescription("The current phase of the conversation.")
    val phase: ConversationPhase,
    @field:JsonPropertyDescription("The estimated CEFR level of the learner.")
    val estimatedCEFRLevel: CEFRLevel,
    @field:JsonPropertyDescription("The current topic of conversation, or null if no specific topic.")
    val currentTopic: String? = null,

    // NEW: Optional metadata fields (backward compatible with default values)
    @field:JsonPropertyDescription("Explanation for why this phase was selected (for prompt context).")
    val phaseReason: String? = null,
    @field:JsonPropertyDescription("Topic eligibility status for hysteresis tracking.")
    val topicEligibilityStatus: String? = null,
    @field:JsonPropertyDescription("List of recently discussed topics to prevent repetition.")
    val pastTopics: List<String> = emptyList()
)
```

**Backward Compatibility Notes:**
- All new fields have default values (null or empty list)
- Existing code creating ConversationState without new fields will continue to work
- toString() output will include new fields (minimal prompt impact if null/empty)
- No database schema changes required (ConversationState not persisted directly)

**Testing:**
- Verify existing ConversationState creation code compiles without changes
- Test that `.copy()` operations work with and without new fields
- Verify toString() output format with null metadata

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
        "Target Language Difficulty: ${metadata.difficulty.name}"
    } else {
        "" // Fallback for languages not in catalog
    }
}
```

**Design Note:** This simplified approach uses only the `difficulty` field from LanguageMetadata. Writing systems and key challenges are deferred to future work where they can be properly catalog-driven (see "Future Enhancements" below).

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

#### Step 2.5: Update ChatService to Compute and Pass Decision Metadata

**File:** `ChatService.kt`

**Location:** Update `sendMessage()` method around lines 244-258 where ConversationState is created.

**Current code (lines 244-256):**
```kotlin
// Resolve effective phase
val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
val effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
    phaseDecisionService.decidePhase(session.conversationPhase, allMessages)
} else {
    session.conversationPhase
}

// Pass effective phase to LLM
val conversationState = ConversationState(
    phase = effectivePhase,
    estimatedCEFRLevel = session.estimatedCEFRLevel,
    currentTopic = session.currentTopic
)
```

**Replace with:**
```kotlin
// Resolve effective phase and compute decision metadata
val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)

// Compute phase decision with metadata (now using modified decidePhase method)
val phaseDecision = if (session.conversationPhase == ConversationPhase.Auto) {
    phaseDecisionService.decidePhase(session.conversationPhase, allMessages)
} else {
    PhaseDecision(
        phase = session.conversationPhase,
        reason = "User-selected phase",
        severityScore = 0.0
    )
}

// Compute topic decision with metadata (now using modified decideTopic method)
val topicDecision = topicDecisionService.decideTopic(
    currentTopic = session.currentTopic,
    llmProposedTopic = session.currentTopic, // LLM hasn't responded yet, use current
    recentMessages = allMessages,
    pastTopicsJson = session.pastTopicsJson
)

// Pass effective phase and metadata to LLM via enriched ConversationState
val conversationState = ConversationState(
    phase = phaseDecision.phase,
    estimatedCEFRLevel = session.estimatedCEFRLevel,
    currentTopic = session.currentTopic,
    phaseReason = phaseDecision.reason,
    topicEligibilityStatus = topicDecision.eligibilityStatus,
    pastTopics = topicDecision.pastTopics
)
```

**Add import statements:**
```kotlin
import ch.obermuhlner.aitutor.tutor.service.PhaseDecision
import ch.obermuhlner.aitutor.tutor.service.TopicDecision
```

**Testing:**
- Update ChatServiceTest mocks to return PhaseDecision and TopicDecision wrapper objects
- Verify ConversationState created with all metadata fields populated
- Test backward compatibility with null metadata (shouldn't crash if fields not set)

**Mock updates for tests:**
```kotlin
// ChatServiceTest.kt - Update mocks for modified methods
every {
    phaseDecisionService.decidePhase(any(), any())
} returns PhaseDecision(ConversationPhase.Correction, "Balanced approach", 2.5)

every {
    topicDecisionService.decideTopic(any(), any(), any(), any())
} returns TopicDecision("travel", 5, "Topic active (turn 5)", listOf("food", "weather"))
```

---

#### Step 3: Consolidate System Message Assembly

**File:** `TutorService.kt:68-123`

Replace existing system message logic:

```kotlin
// Decision metadata comes from enriched ConversationState (computed in ChatService)
// Extract metadata with safe defaults for backward compatibility
val phaseReason = conversationState.phaseReason ?: "Balanced default phase"
val topicEligibilityStatus = conversationState.topicEligibilityStatus ?: "Active conversation"
val pastTopics = conversationState.pastTopics

// Build consolidated system prompt
val consolidatedSystemPrompt = buildConsolidatedSystemPrompt(
    tutor = tutor,
    conversationState = conversationState,
    phaseReason = phaseReason,
    topicEligibilityStatus = topicEligibilityStatus,
    pastTopics = pastTopics,
    targetLanguage = targetLanguage,
    targetLanguageCode = targetLanguageCode,
    sourceLanguage = sourceLanguage,
    sourceLanguageCode = sourceLanguageCode,
    vocabularyGuidance = vocabularyGuidance,
    teachingStyleGuidance = teachingStyleGuidance
)

val systemMessages = listOf(SystemMessage(consolidatedSystemPrompt))

// Log metrics for monitoring
val estimatedTokens = consolidatedSystemPrompt.length / 4
logger.info("System prompt assembled: 1 message, ~$estimatedTokens tokens (estimated)")
logger.debug("Phase: ${conversationState.phase.name} ($phaseReason)")
logger.debug("Topic: ${conversationState.currentTopic ?: "free conversation"} ($topicEligibilityStatus)")
```

**Extract consolidation logic into testable method:**

Add new internal method in TutorService for prompt building (internal visibility allows testing):

```kotlin
internal fun buildConsolidatedSystemPrompt(
    tutor: Tutor,
    conversationState: ConversationState,
    phaseReason: String,
    topicEligibilityStatus: String,
    pastTopics: List<String>,
    targetLanguage: String,
    targetLanguageCode: String,
    sourceLanguage: String,
    sourceLanguageCode: String,
    vocabularyGuidance: String,
    teachingStyleGuidance: String
): String = buildString {
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
    val phasePrompt = when (conversationState.phase) {
        ConversationPhase.Free -> phaseFreePromptTemplate
        ConversationPhase.Correction -> phaseCorrectionPromptTemplate
        ConversationPhase.Drill -> phaseDrillPromptTemplate
        ConversationPhase.Auto -> phaseCorrectionPromptTemplate // Should never happen (resolved in ChatService)
    }
    append(PromptTemplate(phasePrompt).render(mapOf(
        "targetLanguage" to targetLanguage,
        "sourceLanguage" to sourceLanguage
    )))

    append("\n\n")

    // Developer rules (JSON schema)
    append(PromptTemplate(developerPromptTemplate).render(mapOf(
        "targetLanguage" to targetLanguage,
        "sourceLanguage" to sourceLanguage
    )))

    append("\n\n")

    // Session Context (structured, not toString())
    append("=== Current Session Context ===\n")
    append("Phase: ${conversationState.phase.name} ($phaseReason)\n")
    append("CEFR Level: ${conversationState.estimatedCEFRLevel.name}\n")
    append("Topic: ${conversationState.currentTopic ?: "Free conversation"} ($topicEligibilityStatus)\n")
    if (pastTopics.isNotEmpty()) {
        append("Recent Topics: ${pastTopics.takeLast(3).joinToString(", ")}\n")
    }

    append("\n")

    // Language metadata
    append(buildLanguageMetadataPrompt(targetLanguageCode))
}
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

**Summarization Prompt Changes - Deferred:**

The summarization prompt improvements are **OUT OF SCOPE** for Part 4.1 and should be implemented separately after core consolidation is validated. This reduces implementation risk and allows focused testing.

**Rationale for deferral:**
- Progressive summarization is complex with multi-level hierarchy
- Changes could break existing summary chains in long conversations
- Requires separate validation of compression quality
- Part 4.1 delivers 80% of value without this change

**Deferred Summarization Improvements (Optional Future Work):**
```yaml
progressive:
  prompt: |
    Summarize this {targetLanguage} tutoring segment for context retention.

    Include ONLY:
    - Topics discussed (bullet list, no details)
    - Learner errors by type (e.g., "Tense: 3x past/present confusion")
    - New vocabulary (word pairs: {targetLanguage} = {sourceLanguage})

    Exclude: tutor behavior, explanations, example sentences, emotional tone
    Format: Bullets or short phrases. Target: {targetWords} words ({targetTokens} tokens).
```

**Note:** Placeholders `{targetWords}` and `{targetTokens}` are already implemented in:
- `ConversationSummarizationService.kt:92-93`
- `ProgressiveSummarizationService.kt:164-165, 218-219`

No placeholder implementation work needed - only prompt text changes when this deferred work is executed.

**Testing:** Manual review of generated summaries for conciseness

---

### Part 4.2: Optional Persistence Layer (Future Enhancement)

**Decision Criteria:** Implement Part 4.2 persistence ONLY IF ALL of the following are true:

**Required Pre-Conditions:**
1. ✅ **Part 4.1 Success Validated**
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

**If ANY condition fails, DEFER Part 4.2 indefinitely.**

**Use Cases Enabled by Part 4.2:**
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
fun `decidePhase returns correct reason for Free phase`() {
    val messages = createMessagesWithLowSeverityErrors()
    val result = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

    assertEquals(ConversationPhase.Free, result.phase)
    assertTrue(result.reason.contains("Low error rate"))
    assertTrue(result.severityScore < 1.0)
}

@Test
fun `decidePhase returns correct reason for Drill phase`() {
    val messages = createMessagesWithHighSeverityErrors()
    val result = phaseDecisionService.decidePhase(ConversationPhase.Auto, messages)

    assertEquals(ConversationPhase.Drill, result.phase)
    assertTrue(result.reason.contains("High error severity"))
    assertTrue(result.severityScore >= 6.0)
}

@Test
fun `decidePhase returns user-selected phase unchanged`() {
    val messages = emptyList<ChatMessageEntity>()
    val result = phaseDecisionService.decidePhase(ConversationPhase.Correction, messages)

    assertEquals(ConversationPhase.Correction, result.phase)
    // Note: Reason will reflect it's user-selected, not Auto-computed
}
```

**TopicDecisionServiceTest:**
```kotlin
@Test
fun `decideTopic returns correct eligibility for locked topic`() {
    val messages = createMessages(turnCount = 2)
    val result = topicDecisionService.decideTopic("travel", "food", messages, null)

    assertEquals("travel", result.topic) // LLM proposal rejected
    assertTrue(result.eligibilityStatus.contains("locked"))
    assertEquals(2, result.turnCount)
}

@Test
fun `decideTopic returns correct eligibility for stale topic`() {
    val messages = createMessages(turnCount = 13)
    val result = topicDecisionService.decideTopic("travel", "food", messages, null)

    assertEquals("food", result.topic) // LLM proposal accepted
    assertTrue(result.eligibilityStatus.contains("stale"))
    assertEquals(13, result.turnCount)
}

@Test
fun `decideTopic includes past topics in result`() {
    val pastTopicsJson = """["sports", "weather", "travel"]"""
    val result = topicDecisionService.decideTopic("current", null, emptyList(), pastTopicsJson)

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
    val conversationState = ConversationState(
        phase = ConversationPhase.Correction,
        estimatedCEFRLevel = CEFRLevel.B1,
        currentTopic = "travel",
        phaseReason = "Balanced approach - tracking errors",
        topicEligibilityStatus = "Topic active (turn 5)",
        pastTopics = listOf("food", "weather")
    )
    val userId = UUID.randomUUID()

    // Measure OLD approach (3 separate SystemMessages)
    val oldSystemPrompt = buildOldStyleSystemPrompt(tutor, conversationState)
    val oldDeveloperPrompt = buildOldStyleDeveloperPrompt()
    val oldStateString = conversationState.toString()

    val oldSystemMessages = listOf(
        SystemMessage(oldSystemPrompt),
        SystemMessage(oldDeveloperPrompt),
        SystemMessage(oldStateString)
    )
    val oldCharCount = oldSystemMessages.sumOf { it.content.length }
    val oldEstimatedTokens = oldCharCount / 4

    // Measure NEW approach (1 consolidated SystemMessage)
    // Use the new buildConsolidatedSystemPrompt() method
    val consolidatedPrompt = tutorService.buildConsolidatedSystemPrompt(
        tutor = tutor,
        conversationState = conversationState,
        phaseReason = conversationState.phaseReason ?: "Balanced default",
        topicEligibilityStatus = conversationState.topicEligibilityStatus ?: "Active",
        pastTopics = conversationState.pastTopics,
        targetLanguage = "Spanish",
        targetLanguageCode = "es",
        sourceLanguage = "English",
        sourceLanguageCode = "en",
        vocabularyGuidance = buildTestVocabularyGuidance(),
        teachingStyleGuidance = buildTestTeachingStyleGuidance()
    )

    val newSystemMessages = listOf(SystemMessage(consolidatedPrompt))
    val newCharCount = newSystemMessages.sumOf { it.content.length }
    val newEstimatedTokens = newCharCount / 4

    // Verify reduction
    val reduction = (oldEstimatedTokens - newEstimatedTokens).toDouble() / oldEstimatedTokens
    assertTrue(
        reduction >= 0.08,  // Minimum 8% reduction (slightly relaxed target)
        "Expected ≥8% reduction, got ${(reduction * 100).roundToInt()}%"
    )
    assertTrue(
        reduction <= 0.25,  // Maximum 25% reduction (ensure not losing context)
        "Reduction too aggressive (${(reduction * 100).roundToInt()}%), may lose context"
    )

    println("Token reduction: ${(reduction * 100).roundToInt()}% (${oldEstimatedTokens} → ${newEstimatedTokens})")
}
```

**Note:** The `buildConsolidatedSystemPrompt()` method needs to have `internal` visibility (not `private`) to be testable from tests. Update the method signature in Step 3:

```kotlin
// Change from:
private fun buildConsolidatedSystemPrompt(...)

// To:
internal fun buildConsolidatedSystemPrompt(...)
```

**End-to-End Tests:**
- `ChatServiceTest` - verify end-to-end session creation still works
- `MessageCompactionServiceTest` - verify compatibility with new prompt structure

**Existing Test Updates:**
```kotlin
// Update mocks in ChatServiceTest to handle modified method signatures
every {
    phaseDecisionService.decidePhase(any(), any())
} returns PhaseDecision(ConversationPhase.Correction, "Balanced approach", 2.5)

every {
    topicDecisionService.decideTopic(any(), any(), any(), any())
} returns TopicDecision("travel", 5, "Topic active (turn 5)", listOf("food", "weather"))
```

#### Manual Testing
- Create test session in each phase (Free/Correction/Drill/Auto)
- Verify LLM responses show improved context awareness
- Check vocabulary reinforcement behavior unchanged
- Test topic change suggestions respect hysteresis

---

### Rollout Plan

#### Rollout Stage 1: Non-Production Testing (Week 1)

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

#### Rollout Stage 2: Canary Release (Week 2)
1. Deploy to 10% of production sessions
2. Monitor error rates and response times
3. Compare token usage metrics vs baseline
4. Gather qualitative feedback from demo users

#### Rollout Stage 3: Full Rollout (Week 3)
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

**Part 4.1 Rollback (if issues detected):**
1. Revert `TutorService.kt` changes
2. Revert `PhaseDecisionService.kt` changes
3. Revert `TopicDecisionService.kt` changes
4. Revert `application.yml` prompt changes
5. Deploy previous version

**No data loss risk** - Part 4.1 has no database changes

**Rollback Triggers:**
- Error rate increase >5%
- Token usage increase (regression)
- LLM response quality degradation
- Test failures in production

---

## Part 5: REST API Changes

**Part 4.1: No API Changes**

All improvements in Part 4.1 are internal to prompt assembly. REST API contracts remain identical.

**Part 4.2 (Optional Future Enhancement):**

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

## Future Enhancements

These improvements are explicitly deferred from Phase 1 to keep implementation simple and focused:

### 1. Language-Specific Challenge Metadata (Catalog-Driven)
**Deferred from:** Step 2 (Language Metadata Injection)

**Current:** Only `difficulty` field from LanguageMetadata used
**Future:** Add comprehensive language-specific metadata to catalog:

```kotlin
// LanguageMetadata.kt - Future enhancement
data class LanguageMetadata(
    val code: String,
    val nameJson: String,
    val flagEmoji: String,
    val nativeName: String,
    val difficulty: Difficulty,
    val descriptionJson: String,

    // NEW: Language-specific challenge metadata
    val writingSystem: String? = null,  // e.g., "Hiragana, Katakana, Kanji"
    val keyChallengesJson: String? = null  // Multilingual JSON: {"en": "...", "es": "..."}
)
```

**Benefits:**
- No code deployment for new languages
- Multilingual challenge descriptions
- Catalog-driven content management

**Effort:** 1-2 days (catalog schema update, migration, TutorService integration)

---

### 2. Progressive Summarization Prompt Refinement (Phase 1.5)
**Deferred from:** Step 4 (application.yml changes)

**Current:** Existing summarization prompt preserved
**Future:** Implement more specific compression guidance

**Rationale for deferral:**
- Progressive summarization is complex (multi-level hierarchy)
- Changes could break existing summary chains
- Requires separate validation of compression quality
- Part 4.1 delivers 80% of value without this change

**Acceptance Criteria:**
- Baseline quality measurement of current summaries
- A/B test of new vs old prompts
- Compression ratio maintains 3:1 minimum
- No loss of pedagogical context

**Effort:** 2-3 days (testing, validation, rollout)

---

### 3. Decision Metadata Localization
**Current:** Phase/topic decision reasons in English only
**Future:** Localize reason strings based on user's sourceLanguage

**Example:**
```kotlin
// Current (English only):
"Low error rate (score: 2.1) - building fluency confidence"

// Future (Spanish sourceLanguage):
"Tasa de errores baja (puntuación: 2.1) - construyendo confianza en la fluidez"
```

**Effort:** 1 day (i18n infrastructure integration)

---

## Summary

**Recommended Approach:** Alternative 4 - Stage 1 Only (Immediate Improvements)

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

**Stage 2 (Optional):**
- Defer until Stage 1 validated
- Only implement if user-facing features require decision metadata
- Clear separation of concerns