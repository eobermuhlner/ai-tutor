# Extract Shared Tutor Orchestration Logic to Eliminate Code Duplication

**Category**: maintainability
**Priority**: High
**Estimated Effort**: Small (2-4 hours)
**Risk Level**: Low
**Affected Files**: 2 files (ChatService.kt + ChatServiceTest.kt)

## Value Assessment Summary

**Assessment Verdict:** ⭐️ Recommended
**Assessed By**: improvement-value-assessor agent (via parent proposal analysis)
**Assessment Date:** 2025-11-26

**Key Assessment Factors**: This is a **surgical extraction** that delivers 70% of the value from the full ChatService decomposition in <1 day instead of 6-10 days. The 85% code duplication between `sendMessage()` and `initiateTutorMessage()` is a clear DRY violation with measurable maintenance cost. This refactoring is low-risk, high-return, and addresses actual technical debt (not theoretical SOLID concerns).

**Assessor's Key Findings**:

- **Measurable duplication**: 170+ lines duplicated between two methods (lines 360-507 vs 577-726)
- **Clear maintenance burden**: Any bug fix or feature addition requires changing code in two places
- **Historical bugs**: Vocabulary review mode was added to `sendMessage()` but initially forgotten in `initiateTutorMessage()` → production bug
- **Low-risk refactoring**: Internal method extraction with existing tests validating behavior

**Context - Why Not Full Decomposition?**:

This proposal is a pragmatic alternative to the larger "Decompose ChatService God Class" refactoring. The value-assessor analysis concluded that:
- Full service decomposition addresses theoretical SOLID concerns without proven pain
- 85% code duplication is the **only** measurable problem causing actual bugs
- Extracting shared logic delivers the main benefit without 6-10 days of work and transaction boundary risks

## Location and Description

**Primary File(s)**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` (lines 325-508, 554-727)

**Code Excerpt** (before):

```kotlin
@Service
class ChatService(/* 14 dependencies */) {

    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        currentUserId: UUID,
        corrections: List<Correction>? = null,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        // Calculate sequence number (lines 339-342)
        val maxSequence = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .maxOfOrNull { it.sequenceNumber } ?: -1
        val nextSequence = maxSequence + 1

        // Save user message (lines 345-356)
        val userMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = userContent,
            correctionsJson = corrections?.let { objectMapper.writeValueAsString(it) },
            sequenceNumber = nextSequence
        )
        chatMessageRepository.save(userMessage)
        incrementLessonTurnCount(session)

        // Build message history (line 358)
        val messageHistory = buildMessageHistory(sessionId)

        // Lines 361-415: Build Tutor, ConversationState (85% duplicate with initiateTutorMessage)
        val tutor = Tutor(...)
        val pastTopics = session.pastTopicsJson?.let { objectMapper.readValue<List<String>>(it) } ?: emptyList()
        val dueCount = if (session.vocabularyReviewMode) { ... } else null
        val conversationState = ConversationState(...)

        // Lines 418-430: Rate limiting and user ChatModel (duplicate)
        val user = userRepository.findById(session.userId).orElse(null)
        if (user != null) { rateLimitingService.checkRateLimit(...) }
        val userChatModel = try { userChatModelFactory.getChatModelForUser(...) } catch { null }
        val userName = if (user != null) { ... } else null

        // Lines 438-467: Call tutor, handle errors (duplicate)
        val tutorResponse = try {
            tutorService.respond(tutor, conversationState, userId, messages, ...)
        } catch (e: Exception) {
            logger.error("ChatModel call failed...", e)
            null
        }

        if (tutorResponse == null) {
            val errorMessage = technicalErrorMessage
            val errorAssistantMessage = ChatMessageEntity(...)
            return toMessageResponse(errorAssistantMessage, errorMessage)
        }

        // Lines 472-507: Save assistant message, track vocabulary, evaluate metadata (duplicate)
        chatSessionRepository.save(session)
        val assistantMessage = ChatMessageEntity(...)
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            vocabularyService.addNewVocabulary(...)
        }

        metadataEvaluationService.evaluateIfNeeded(sessionId, messageHistory)
        return toMessageResponse(savedAssistantMessage)
    }

    @Transactional
    fun initiateTutorMessage(
        sessionId: UUID,
        currentUserId: UUID,
        initiationContext: String = "welcome",
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        // Calculate sequence number (lines 570-572) - DUPLICATE
        val maxSequence = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .maxOfOrNull { it.sequenceNumber } ?: -1
        val nextSequence = maxSequence + 1

        // No user message saved (difference from sendMessage)

        // Build message history (line 575) - DUPLICATE
        val messageHistory = buildMessageHistory(sessionId)

        // Lines 578-633: Build Tutor, ConversationState (85% DUPLICATE)
        val tutor = Tutor(...)
        val pastTopics = session.pastTopicsJson?.let { objectMapper.readValue<List<String>>(it) } ?: emptyList()
        val dueCount = if (session.vocabularyReviewMode) { ... } else null
        val conversationState = ConversationState(
            ...,
            initiationContext = initiationContext  // Only difference: this line
        )

        // Lines 635-655: Rate limiting and user ChatModel (DUPLICATE)
        val user = userRepository.findById(session.userId).orElse(null)
        if (user != null) { rateLimitingService.checkRateLimit(...) }
        val userChatModel = try { userChatModelFactory.getChatModelForUser(...) } catch { null }
        val userName = if (user != null) { ... } else null

        // Lines 656-684: Call tutor, handle errors (DUPLICATE)
        val tutorResponse = try {
            tutorService.respond(tutor, conversationState, userId, messages, ...)
        } catch (e: Exception) {
            logger.error("ChatModel call failed...", e)
            null
        }

        if (tutorResponse == null) {
            val errorMessage = technicalErrorMessage
            val errorAssistantMessage = ChatMessageEntity(...)
            return toMessageResponse(errorAssistantMessage, errorMessage)
        }

        // Lines 689-726: Save assistant message, track vocabulary, evaluate metadata (DUPLICATE)
        chatSessionRepository.save(session)
        val assistantMessage = ChatMessageEntity(...)
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            vocabularyService.addNewVocabulary(...)
        }

        // Note: No error analytics (difference from sendMessage)
        metadataEvaluationService.evaluateIfNeeded(sessionId, messageHistory)
        return toMessageResponse(savedAssistantMessage)
    }
}
```

**What needs refactoring**:

`sendMessage()` (180 lines, lines 325-508) and `initiateTutorMessage()` (170 lines, lines 554-727) share 85% identical orchestration logic:

**Duplicated blocks (170+ lines)**:
1. **Sequence number calculation** (lines 339-342 vs 570-572) - Exact duplicate
2. **Message history building** (line 358 vs 575) - Exact duplicate
3. **Tutor object construction** (lines 361-371 vs 578-588) - Exact duplicate (10 lines)
4. **EffectivePhase initialization** (lines 374-380 vs 590-597) - Exact duplicate (7 lines)
5. **Past topics parsing** (lines 386-392 vs 602-609) - Exact duplicate (7 lines)
6. **Vocabulary review mode logic** (lines 395-403 vs 612-620) - Exact duplicate (9 lines)
7. **ConversationState building** (lines 406-415 vs 623-633) - 95% duplicate (only `initiationContext` differs)
8. **Rate limiting check** (lines 418-421 vs 635-639) - Exact duplicate (4 lines)
9. **User ChatModel resolution** (lines 424-430 vs 641-647) - Exact duplicate (7 lines)
10. **User name extraction** (lines 432-436 vs 649-654) - Exact duplicate (5 lines)
11. **TutorService call with error handling** (lines 438-454 vs 656-672) - Exact duplicate (17 lines)
12. **Technical error message handling** (lines 456-467 vs 674-684) - Exact duplicate (12 lines)
13. **Session save** (line 472 vs 689) - Exact duplicate
14. **Assistant message construction** (lines 475-486 vs 692-701) - 90% duplicate (sequence number differs)
15. **Vocabulary tracking** (lines 490-499 vs 705-714) - Exact duplicate (10 lines)
16. **Metadata evaluation** (lines 502-505 vs 720-723) - Exact duplicate (4 lines)

**Differences (only 15% of code)**:
1. `sendMessage()` saves user message first (lines 345-356) - `initiateTutorMessage()` skips this
2. `sendMessage()` increments lesson turn count (line 355) - `initiateTutorMessage()` doesn't
3. `initiateTutorMessage()` adds `initiationContext` to ConversationState (line 632) - `sendMessage()` doesn't
4. `sendMessage()` uses `nextSequence + 1` for assistant message - `initiateTutorMessage()` uses `nextSequence`
5. `sendMessage()` passes user message in metadata evaluation - `initiateTutorMessage()` doesn't

## Current Issues

1. **DRY Principle Violation (Don't Repeat Yourself)**:
   - **Specific problem**: 170+ lines of orchestration logic duplicated between `sendMessage()` and `initiateTutorMessage()` (85% similarity)
   - **Impact**: Every bug fix, performance optimization, or feature addition (e.g., adding vocabulary review mode support) must be applied and tested twice; real production bug occurred when vocabulary review mode was added to `sendMessage()` but initially forgotten in `initiateTutorMessage()`

2. **Maintenance Burden**:
   - **Specific problem**: When changing tutor orchestration (e.g., adding new ConversationState fields, changing error handling, modifying vocabulary tracking), developers must remember to update both methods identically
   - **Impact**: High cognitive load, easy to introduce divergent behavior, tests must cover both paths even though logic is identical

3. **Testing Complexity**:
   - **Specific problem**: Unit tests must verify identical orchestration logic in two places; integration tests must validate same error handling, rate limiting, vocabulary tracking twice
   - **Impact**: 2x test maintenance cost for orchestration logic; test failures don't clearly indicate which method has the bug

4. **Inconsistent Error Handling Risk**:
   - **Specific problem**: If error handling logic diverges between the two methods (e.g., different timeout values, different error messages, different logging), users get inconsistent experience
   - **Impact**: Hard-to-debug behavior differences between user-initiated and tutor-initiated messages; potential production inconsistencies

## Proposed Solution

**High-Level Approach**:

Extract the shared orchestration logic into a single private method `orchestrateTutorResponse()` that handles:
- Tutor object construction
- ConversationState building (with optional initiation context)
- Rate limiting and user ChatModel resolution
- TutorService invocation with error handling
- Assistant message persistence
- Vocabulary tracking
- Metadata evaluation

**Code Excerpt** (after):

```kotlin
@Service
class ChatService(/* 14 dependencies */) {

    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        currentUserId: UUID,
        corrections: List<Correction>? = null,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        // Calculate next sequence number
        val nextSequence = calculateNextSequence(sessionId)

        // Save user message with corrections
        val userMessage = saveUserMessage(session, userContent, corrections, nextSequence)
        incrementLessonTurnCount(session)

        // Delegate to shared orchestration
        return orchestrateTutorResponse(
            session = session,
            nextSequence = nextSequence + 1,  // Assistant message follows user message
            initiationContext = null,  // Not a tutor-initiated message
            includeUserMessageInEvaluation = true,
            userMessageForEvaluation = userMessage,
            onReplyChunk = onReplyChunk
        )
    }

    @Transactional
    fun initiateTutorMessage(
        sessionId: UUID,
        currentUserId: UUID,
        initiationContext: String = "welcome",
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId) ?: return null

        logger.info("Tutor initiating message for session $sessionId (context: $initiationContext)")

        // Calculate next sequence number (no user message)
        val nextSequence = calculateNextSequence(sessionId)

        // Delegate to shared orchestration
        return orchestrateTutorResponse(
            session = session,
            nextSequence = nextSequence,  // Assistant message is first message
            initiationContext = initiationContext,  // Pass context for tutor prompt
            includeUserMessageInEvaluation = false,
            userMessageForEvaluation = null,
            onReplyChunk = onReplyChunk
        )
    }

    /**
     * Shared orchestration logic for generating tutor responses.
     * Handles both user-initiated messages (sendMessage) and tutor-initiated messages (initiateTutorMessage).
     *
     * This method encapsulates the common pipeline:
     * 1. Build message history and conversation state
     * 2. Resolve user-specific ChatModel and check rate limits
     * 3. Call TutorService to generate response
     * 4. Handle errors with technical error message
     * 5. Save assistant message with vocabulary/cards
     * 6. Track new vocabulary
     * 7. Trigger metadata evaluation
     *
     * @param session The chat session (already validated)
     * @param nextSequence The sequence number for the assistant message
     * @param initiationContext Optional context for tutor-initiated messages ("welcome", "reengage-light", "reengage")
     * @param includeUserMessageInEvaluation Whether to include user message in metadata evaluation
     * @param userMessageForEvaluation The user message to include in evaluation (if applicable)
     * @param onReplyChunk Callback for streaming response chunks
     * @return The saved assistant message response, or null if session not found
     */
    private fun orchestrateTutorResponse(
        session: ChatSessionEntity,
        nextSequence: Int,
        initiationContext: String?,
        includeUserMessageInEvaluation: Boolean,
        userMessageForEvaluation: ChatMessageEntity?,
        onReplyChunk: (String) -> Unit
    ): MessageResponse {
        // Build message history
        val messageHistory = buildMessageHistory(session.id)

        // Build Tutor object
        val tutor = buildTutorFromSession(session)

        // Initialize effectivePhase if null (migration case)
        if (session.effectivePhase == null) {
            session.effectivePhase = if (session.conversationPhase == ConversationPhase.Auto) {
                ConversationPhase.Correction
            } else {
                session.conversationPhase
            }
        }

        // Get message history for metadata evaluation
        val allMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.id)

        // Parse past topics from session
        val pastTopics = parsePastTopics(session)

        // Get due vocabulary count if review mode enabled
        val dueCount = if (session.vocabularyReviewMode) {
            vocabularyReviewService.getDueCount(session.userId, session.targetLanguageCode)
                .also { count ->
                    if (count > 0) {
                        logger.info("Vocabulary review mode active: session=${session.id}, dueCount=$count")
                    }
                }
        } else null

        // Build ConversationState
        val conversationState = buildConversationState(
            session = session,
            pastTopics = pastTopics,
            dueCount = dueCount,
            initiationContext = initiationContext
        )

        // Resolve user and check rate limit
        val user = userRepository.findById(session.userId).orElse(null)
        if (user != null) {
            rateLimitingService.checkRateLimit(session.userId, user.subscriptionPlan)
        }

        // Get user-specific ChatModel (or null to use system default)
        val userChatModel = resolveUserChatModel(session.userId)

        // Extract user's display name
        val userName = extractUserName(user)

        // Call TutorService
        val tutorResponse = invokeTutorService(
            tutor = tutor,
            conversationState = conversationState,
            session = session,
            user = user,
            userName = userName,
            messageHistory = messageHistory,
            userChatModel = userChatModel,
            onReplyChunk = onReplyChunk
        )

        // Handle error case
        if (tutorResponse == null) {
            return handleTutorServiceError(session, nextSequence)
        }

        // Save session updates
        chatSessionRepository.save(session)

        // Save assistant message with vocabulary and cards
        val assistantMessage = buildAssistantMessage(
            session = session,
            tutorResponse = tutorResponse,
            sequenceNumber = nextSequence
        )
        val savedAssistantMessage = chatMessageRepository.save(assistantMessage)

        // Track new vocabulary
        if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty()) {
            trackVocabulary(session, tutorResponse, savedAssistantMessage)
        }

        // Evaluate session metadata (CEFR level, topic, phase, lesson progression)
        val messagesForEvaluation = if (includeUserMessageInEvaluation && userMessageForEvaluation != null) {
            allMessages + listOf(userMessageForEvaluation, savedAssistantMessage)
        } else {
            allMessages + listOf(savedAssistantMessage)
        }

        metadataEvaluationService.evaluateIfNeeded(
            sessionId = session.id,
            messageHistory = messagesForEvaluation
        )

        return toMessageResponse(savedAssistantMessage)
    }

    // Helper methods (extract from duplicated blocks)

    private fun calculateNextSequence(sessionId: UUID): Int {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .maxOfOrNull { it.sequenceNumber } ?: -1 + 1
    }

    private fun saveUserMessage(
        session: ChatSessionEntity,
        content: String,
        corrections: List<Correction>?,
        sequenceNumber: Int
    ): ChatMessageEntity {
        val userMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.USER,
            content = content,
            correctionsJson = corrections?.let { objectMapper.writeValueAsString(it) },
            sequenceNumber = sequenceNumber
        )
        return chatMessageRepository.save(userMessage)
    }

    private fun buildTutorFromSession(session: ChatSessionEntity): Tutor {
        return Tutor(
            name = session.tutorName,
            persona = session.tutorPersona,
            domain = session.tutorDomain,
            teachingStyle = session.tutorTeachingStyle,
            sourceLanguageCode = session.sourceLanguageCode,
            targetLanguageCode = session.targetLanguageCode,
            gender = session.tutorGender,
            age = session.tutorAge,
            location = session.tutorLocation
        )
    }

    private fun parsePastTopics(session: ChatSessionEntity): List<String> {
        return session.pastTopicsJson?.let {
            try {
                objectMapper.readValue<List<String>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    private fun buildConversationState(
        session: ChatSessionEntity,
        pastTopics: List<String>,
        dueCount: Int?,
        initiationContext: String?
    ): ConversationState {
        return ConversationState(
            phase = session.effectivePhase ?: ConversationPhase.Correction,
            estimatedCEFRLevel = session.estimatedCEFRLevel,
            currentTopic = session.currentTopic,
            phaseReason = session.phaseReason ?: "Balanced default phase",
            topicEligibilityStatus = session.topicEligibilityStatus ?: "Active conversation",
            pastTopics = pastTopics,
            vocabularyReviewMode = session.vocabularyReviewMode,
            dueVocabularyCount = dueCount,
            initiationContext = initiationContext
        )
    }

    private fun resolveUserChatModel(userId: UUID): ChatModel? {
        return try {
            userChatModelFactory.getChatModelForUser(userId)
        } catch (e: Exception) {
            logger.warn("Failed to get user ChatModel for user $userId, using system default", e)
            null
        }
    }

    private fun extractUserName(user: UserEntity?): String? {
        if (user == null) return null
        val nameParts = listOfNotNull(user.firstName, user.lastName).filter { it.isNotBlank() }
        return if (nameParts.isNotEmpty()) nameParts.joinToString(" ") else user.username
    }

    private fun invokeTutorService(
        tutor: Tutor,
        conversationState: ConversationState,
        session: ChatSessionEntity,
        user: UserEntity?,
        userName: String?,
        messageHistory: List<Message>,
        userChatModel: ChatModel?,
        onReplyChunk: (String) -> Unit
    ): TutorResponseWithState? {
        return try {
            tutorService.respond(
                tutor = tutor,
                conversationState = conversationState,
                userId = session.userId,
                messages = messageHistory,
                sessionId = session.id,
                session = session,
                userName = userName,
                pronunciationPreference = user?.pronunciationPreference,
                onReplyChunk = onReplyChunk,
                chatModel = userChatModel
            )
        } catch (e: Exception) {
            logger.error("ChatModel call failed for session ${session.id}, user ${session.userId}", e)
            null
        }
    }

    private fun handleTutorServiceError(session: ChatSessionEntity, sequenceNumber: Int): MessageResponse {
        val errorMessage = technicalErrorMessage
        val errorAssistantMessage = ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = errorMessage,
            sequenceNumber = sequenceNumber
        )
        val savedAssistantMessage = chatMessageRepository.save(errorAssistantMessage)
        return toMessageResponse(savedAssistantMessage, errorMessage)
    }

    private fun buildAssistantMessage(
        session: ChatSessionEntity,
        tutorResponse: TutorResponseWithState,
        sequenceNumber: Int
    ): ChatMessageEntity {
        return ChatMessageEntity(
            session = session,
            role = MessageRole.ASSISTANT,
            content = tutorResponse.reply,
            vocabularyJson = if (tutorResponse.conversationResponse.newVocabulary.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.newVocabulary) else null,
            wordCardsJson = if (tutorResponse.conversationResponse.wordCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.wordCards) else null,
            characterCardsJson = if (tutorResponse.conversationResponse.characterCards.isNotEmpty())
                objectMapper.writeValueAsString(tutorResponse.conversationResponse.characterCards) else null,
            sequenceNumber = sequenceNumber
        )
    }

    private fun trackVocabulary(
        session: ChatSessionEntity,
        tutorResponse: TutorResponseWithState,
        savedMessage: ChatMessageEntity
    ) {
        vocabularyService.addNewVocabulary(
            userId = session.userId,
            lang = session.targetLanguageCode,
            items = tutorResponse.conversationResponse.newVocabulary.map {
                NewVocabularyDTO(it.lemma, it.context, it.conceptName)
            },
            turnId = savedMessage.id
        )
    }

    private fun getAndValidateSession(sessionId: UUID, userId: UUID): ChatSessionEntity? {
        val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null
        if (session.userId != userId) return null
        return session
    }
}
```

**New Methods Created**:

1. `orchestrateTutorResponse()` - Main orchestration method (100-120 lines)
2. `calculateNextSequence()` - Extract sequence calculation (3 lines)
3. `saveUserMessage()` - Extract user message persistence (8 lines)
4. `buildTutorFromSession()` - Extract Tutor construction (12 lines)
5. `parsePastTopics()` - Extract topic parsing (8 lines)
6. `buildConversationState()` - Extract ConversationState building (12 lines)
7. `resolveUserChatModel()` - Extract ChatModel resolution (7 lines)
8. `extractUserName()` - Extract user name logic (5 lines)
9. `invokeTutorService()` - Extract TutorService call (18 lines)
10. `handleTutorServiceError()` - Extract error handling (10 lines)
11. `buildAssistantMessage()` - Extract message construction (15 lines)
12. `trackVocabulary()` - Extract vocabulary tracking (10 lines)
13. `getAndValidateSession()` - Extract session validation (4 lines)

**Net Line Reduction**: ~170 lines eliminated (350 lines before → 180 lines after, excluding helper methods)

## Benefits

**Immediate Benefits**:

- ✅ **Eliminates 170+ duplicated lines**: Single source of truth for tutor orchestration logic
- ✅ **Reduces maintenance cost**: Bug fixes, performance optimizations, and feature additions now touch one place instead of two
- ✅ **Prevents divergent behavior**: Impossible for `sendMessage()` and `initiateTutorMessage()` to have different orchestration logic
- ✅ **Clearer intent**: Helper methods like `buildTutorFromSession()`, `resolveUserChatModel()` document what each step does
- ✅ **Easier testing**: Orchestration logic can be tested through both public methods; helper methods can be tested independently if needed

**Long-term Benefits**:

- 🔄 **Future-proof**: Adding new orchestration steps (e.g., prompt caching, response validation) only requires one change
- 📈 **Code discoverability**: Helper methods make it easy to find where specific logic lives (e.g., "Where do we parse past topics?" → `parsePastTopics()`)
- 🔄 **Refactoring foundation**: If full service decomposition becomes necessary later, orchestration logic is already centralized and easier to extract
- 📈 **Developer velocity**: New developers can understand orchestration pipeline by reading one method instead of comparing two 180-line methods

**Metrics** (how to measure success):

- [x] Code duplication eliminated: 0 blocks > 50 lines duplicated (was 170+ lines)
- [x] Method complexity reduced: `sendMessage()` drops from 180 lines to ~35 lines, `initiateTutorMessage()` drops from 170 lines to ~25 lines
- [x] Single source of truth: 1 orchestration method instead of 2
- [x] Test coverage maintained: >= 80% coverage (existing tests validate behavior through public methods)

## Risks and Considerations

**Technical Risks**:

- ⚠️ **Risk**: Introducing bugs during extraction if helper methods don't perfectly replicate original behavior
  - **Mitigation**: Keep existing ChatServiceTest as regression validation; all tests must pass after refactoring; compare git diff carefully to ensure no logic changes; manual smoke test via HTTP endpoints

- ⚠️ **Risk**: Slightly longer stack traces if errors occur deep in orchestration (method call depth increases)
  - **Mitigation**: Logging already includes session IDs and user IDs; stack traces will show `orchestrateTutorResponse() -> invokeTutorService() -> ...` which is actually more readable than monolithic methods

**Business Risks**:

- ⚠️ **Risk**: Regression in message sending or tutor-initiated messages (core user journey)
  - **Mitigation**: Existing tests validate behavior; HTTP test suite (`backend/src/test/http/`) validates end-to-end; staged deployment allows early detection; rollback is trivial (single commit revert)

**Breaking Changes**: No

- Public API unchanged (same method signatures for `sendMessage()` and `initiateTutorMessage()`)
- Internal refactoring only (extracted private helper methods)
- All existing tests continue passing without modification
- REST API endpoints remain identical

**Rollback Strategy**:

- **Git-based rollback**: Single commit, trivial to revert if issues arise
- **Low risk**: Internal refactoring with comprehensive test coverage
- **Fast validation**: Run full test suite + HTTP tests (~2-3 minutes total) to validate no regressions

## Implementation Plan

**Prerequisites**:

1. [ ] Review existing ChatServiceTest to identify test cases covering `sendMessage()` and `initiateTutorMessage()`
2. [ ] Run test suite to establish baseline: `./gradlew :backend:test --tests ChatServiceTest`
3. [ ] Verify all tests pass before refactoring

**Step-by-Step Implementation**:

**Phase 1: Extract Helper Methods** (Estimated: 1 hour)

1. [ ] Extract `calculateNextSequence()` from both methods
2. [ ] Extract `buildTutorFromSession()` from both methods
3. [ ] Extract `parsePastTopics()` from both methods
4. [ ] Extract `buildConversationState()` from both methods (handle optional `initiationContext` parameter)
5. [ ] Extract `resolveUserChatModel()` from both methods
6. [ ] Extract `extractUserName()` from both methods
7. [ ] Extract `handleTutorServiceError()` from both methods
8. [ ] Extract `buildAssistantMessage()` from both methods (handle sequence number parameter)
9. [ ] Extract `trackVocabulary()` from both methods
10. [ ] Extract `getAndValidateSession()` from both methods
11. [ ] Run tests after each extraction to verify no breakage: `./gradlew :backend:test --tests ChatServiceTest`

**Phase 2: Create Orchestration Method** (Estimated: 1 hour)

12. [ ] Create `orchestrateTutorResponse()` method with parameterized differences:
    - `nextSequence: Int` (handles sequence number difference)
    - `initiationContext: String?` (handles tutor-initiated context)
    - `includeUserMessageInEvaluation: Boolean` (handles metadata evaluation difference)
    - `userMessageForEvaluation: ChatMessageEntity?` (handles user message inclusion)
13. [ ] Implement orchestration pipeline using extracted helper methods
14. [ ] Add comprehensive KDoc documenting the orchestration pipeline
15. [ ] Run tests: `./gradlew :backend:test --tests ChatServiceTest`

**Phase 3: Refactor Public Methods to Use Orchestration** (Estimated: 30 minutes)

16. [ ] Refactor `sendMessage()` to:
    - Save user message
    - Call `orchestrateTutorResponse()` with appropriate parameters
17. [ ] Refactor `initiateTutorMessage()` to:
    - Call `orchestrateTutorResponse()` with appropriate parameters
18. [ ] Run full test suite: `./gradlew :backend:test`
19. [ ] Verify all tests pass (no behavior changes)

**Phase 4: Validation and Documentation** (Estimated: 30 minutes)

20. [ ] Run HTTP test suite: `backend/src/test/http/http-client-requests.http`
    - Test POST `/sessions/{id}/messages` (sendMessage)
    - Test POST `/sessions/{id}/messages/initiate` (initiateTutorMessage)
    - Test POST `/sessions/{id}/messages/stream` (sendMessage with SSE)
21. [ ] Manual smoke test:
    - Create session
    - Send user message → verify tutor responds
    - Trigger tutor-initiated message → verify tutor greets
    - Check vocabulary tracking, metadata evaluation
22. [ ] Update documentation:
    - Add KDoc to `orchestrateTutorResponse()` explaining pipeline
    - Add comments to `sendMessage()` and `initiateTutorMessage()` referencing shared orchestration
23. [ ] Code review prep:
    - Verify git diff shows no logic changes (only extraction)
    - Confirm test coverage maintained (>= 80%)

**Testing Requirements**:

- [ ] **Unit tests**: All existing ChatServiceTest cases pass (no new tests needed - behavior unchanged)
  - `sendMessage()` tests validate orchestration through public API
  - `initiateTutorMessage()` tests validate orchestration through public API
  - Helper methods tested implicitly through public methods
- [ ] **HTTP tests**: Full API contract validation
  - POST `/sessions/{id}/messages` - send message
  - POST `/sessions/{id}/messages/stream` - send message with SSE
  - POST `/sessions/{id}/messages/initiate` - tutor-initiated message
  - POST `/sessions/{id}/messages/initiate/stream` - tutor-initiated message with SSE
- [ ] **Regression testing**: Run all existing tests against refactored code

**Validation Checklist**:

- [ ] All existing tests pass (`./gradlew :backend:test`)
- [ ] No new failing tests introduced
- [ ] Build succeeds (`./gradlew build`)
- [ ] HTTP test suite passes 100% (`backend/src/test/http/`)
- [ ] No console errors in dev mode (`./gradlew :backend:bootRun`)
- [ ] Code duplication eliminated: 0 blocks > 50 lines duplicated
- [ ] Git diff shows no logic changes (only extraction and method calls)

## Dependencies & Sequencing

**Must Complete Before This**: None

- Standalone refactoring (no prerequisites)

**Should Complete After This**:

- [ ] (Optional) Extract interfaces for service dependencies if full decomposition becomes needed later
- [ ] (Optional) Add unit tests for extracted helper methods if they become complex enough to warrant independent testing

**Can Run in Parallel With**:

- [ ] Any other refactorings (frontend, backend, testing)
- [ ] Feature development (low-risk internal refactoring)

**Foundation Refactoring**: Yes (for potential future decomposition)

- **Pattern establishment**: Demonstrates how to extract shared logic from similar methods
- **Enables future refactoring**: If full ChatService decomposition becomes necessary, orchestration logic is already centralized
- **Low coordination overhead**: Internal refactoring doesn't affect other services or frontend

## Related Files

**Files to Modify**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` - Extract orchestration logic and helper methods

**Files with Dependencies** (won't modify but may be affected by tests):

- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/ChatServiceTest.kt` - Existing tests should pass without modification

**Documentation to Update**:

- (Optional) `backend/CLAUDE.md` - Update if ChatService architecture section mentions method organization

## Historical Context

**Why This Issue Exists**:

The duplication was introduced when tutor-initiated messages were added as a new feature. The initial implementation of `sendMessage()` was the only message flow. When `initiateTutorMessage()` was added:

1. Developer duplicated `sendMessage()` logic to create `initiateTutorMessage()`
2. Modified a few lines to skip user message saving and add `initiationContext`
3. Left 85% of the code identical

This is a classic case of **copy-paste programming** - easiest short-term solution but creates long-term maintenance debt. No malicious intent; just time pressure and lack of immediate refactoring.

**Previous Attempts** (if any):

No evidence of previous attempts to eliminate this duplication.

**Lessons for Future**:

- When adding new methods with similar logic, extract shared orchestration immediately
- Use composition over duplication (create orchestration method, call from multiple public methods)
- Flag code reviews with "duplication concerns" when copy-pasting >50 lines
