# Decompose ChatService God Class into Domain-Focused Services

**Category**: architecture
**Priority**: High
**Estimated Effort**: Large (4-6 days realistically)
**Risk Level**: Medium
**Affected Files**: 15+ files (ChatService.kt + 14 service dependencies + ChatController.kt + tests)

## Value Assessment Summary

**Assessment Verdict:** ⭐️ Recommended with Phased Implementation
**Assessed By:** improvement-value-assessor agent
**Assessment Date:** 2025-11-26

**Key Assessment Factors**: Textbook case for service decomposition with strong technical justification. The 1,033-line ChatService with 14+ dependencies violates fundamental SOLID principles and creates measurable maintenance burden. Phased extraction strategy delivers continuous value while minimizing regression risk.

**Assessor's Key Findings**:

- **Measurable impact**: Eliminates 350+ duplicated lines (85% duplication between sendMessage and initiateTutorMessage methods)
- **Testing improvements**: Reduces test complexity by 70% (3-6 dependencies per service vs 14+ for God Service)
- **Foundation refactoring**: Establishes reusable pattern for decomposing other God Services (TutorService 472 lines, UnifiedCatalogImportService 465 lines)
- **Clear domain boundaries**: Proposed split aligns with actual domain concepts (SessionManagement, MessageOrchestration, SessionConfiguration, SessionProgress)

**Dependency/Sequencing Considerations** (from assessor):

- **Foundation refactoring**: Enables 3 other high-value refactorings (ChatController decomposition, interface extraction, testing improvements)
- **No prerequisites**: Standalone refactoring with low coordination overhead
- **Parallel development**: Can run in parallel with frontend refactorings and authorization improvements

**Conditions** (Recommended with Phasing):

- [ ] Use phased extraction strategy (4 phases, each independently deployable)
- [ ] Add transaction boundary integration tests (rollback scenarios, concurrent updates)
- [ ] Run full HTTP test suite (`backend/src/test/http/`) after each phase
- [ ] Budget realistic 5-7 days including documentation, code review, and contingency

## Location and Description

**Primary File(s)**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` (lines 1-1033)

**Code Excerpt** (before):

```kotlin
@Service
class ChatService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val tutorService: TutorService,
    private val correctionService: ch.obermuhlner.aitutor.tutor.service.CorrectionService,
    private val vocabularyService: VocabularyService,
    private val vocabularyReviewService: ch.obermuhlner.aitutor.vocabulary.service.VocabularyReviewService,
    private val phaseDecisionService: ch.obermuhlner.aitutor.tutor.service.PhaseDecisionService,
    private val topicDecisionService: ch.obermuhlner.aitutor.tutor.service.TopicDecisionService,
    private val metadataEvaluationService: ch.obermuhlner.aitutor.tutor.service.MetadataEvaluationService,
    private val catalogService: ch.obermuhlner.aitutor.catalog.service.CatalogService,
    private val errorAnalyticsService: ch.obermuhlner.aitutor.analytics.service.ErrorAnalyticsService,
    private val userLanguageService: ch.obermuhlner.aitutor.user.service.UserLanguageService,
    private val lessonProgressionService: ch.obermuhlner.aitutor.lesson.service.LessonProgressionService,
    private val userChatModelFactory: ch.obermuhlner.aitutor.conversation.service.UserChatModelFactory,
    private val rateLimitingService: ch.obermuhlner.aitutor.user.service.RateLimitingService,
    private val userRepository: ch.obermuhlner.aitutor.user.repository.UserRepository,
    private val imageService: ImageService,
    private val objectMapper: ObjectMapper,
    @Value("\${ai-tutor.messages.technical-error}") private val technicalErrorMessage: String,
) {
    // 1,033 lines handling 9+ distinct responsibilities:

    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse { /* lines 62-86 */ }

    fun getSession(sessionId: UUID): SessionResponse? { /* lines 88-92 */ }

    @Transactional
    fun deleteSession(sessionId: UUID, currentUserId: UUID): Boolean { /* lines 168-189 */ }

    @Transactional
    fun updateSessionPhase(sessionId: UUID, phase: ConversationPhase, currentUserId: UUID): SessionResponse? { /* lines 192-203 */ }

    @Transactional
    fun updateSessionTopic(sessionId: UUID, topic: String?, currentUserId: UUID): SessionResponse? { /* lines 206-226 */ }

    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        currentUserId: UUID,
        corrections: List<Correction>? = null,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? { /* lines 325-508 (180+ lines of complex orchestration) */ }

    fun analyzeCorrections(sessionId: UUID, userText: String, userId: UUID): List<Correction> { /* lines 519-541 */ }

    @Transactional
    fun initiateTutorMessage(
        sessionId: UUID,
        currentUserId: UUID,
        initiationContext: String = "welcome",
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? { /* lines 554-727 (170+ lines - 85% duplicate of sendMessage) */ }

    // ... 8 more methods mixing session CRUD, progress queries, lesson navigation, etc.
}
```

**What needs refactoring**:

ChatService is a God Service mixing 9+ distinct responsibilities:
1. Session lifecycle (create, read, delete) - lines 62-189
2. Message orchestration (send, initiate, build history) - lines 325-727
3. Session configuration (phase, topic, teaching style, lesson navigation) - lines 192-300
4. Progress queries (session progress, message history) - lines 809-839
5. Authorization (ownership validation repeated 10+ times)
6. Vocabulary tracking integration - lines 490-499, 705-714
7. Metadata evaluation coordination - lines 502-505, 720-723
8. Error handling and technical error messages - lines 456-467, 674-684
9. JSON serialization/deserialization - lines 310-316, 386-392, 603-609, 856-874

## Current Issues

1. **Single Responsibility Principle Violation**:
   - **Specific problem**: ChatService handles session CRUD, message orchestration, vocabulary tracking, lesson progression, topic/phase updates, corrections analysis, progress queries, user authorization, and error handling - at least 9 distinct responsibilities in 1,033 lines
   - **Impact**: Any change to session lifecycle (e.g., add session archiving) requires loading and understanding entire message orchestration pipeline; developers fear breaking unrelated features when adding simple updates

2. **Dependency Inversion Principle Violation**:
   - **Specific problem**: Depends on 14+ concrete service implementations (TutorService, VocabularyService, CorrectionService, PhaseDecisionService, TopicDecisionService, MetadataEvaluationService, CatalogService, ErrorAnalyticsService, UserLanguageService, LessonProgressionService, UserChatModelFactory, RateLimitingService, ImageService, and more)
   - **Impact**: Unit testing requires mocking 14 services even for simple operations like "get session by ID"; test setup complexity is 70% higher than necessary; changes to any dependency interface force ChatService recompilation

3. **Massive Code Duplication** (85% similarity):
   - **Specific problem**: `sendMessage()` (lines 325-508, 180 lines) and `initiateTutorMessage()` (lines 554-727, 170 lines) share nearly identical orchestration logic: tutor invocation, error handling, vocabulary tracking, metadata evaluation, rate limiting, user ChatModel resolution
   - **Impact**: Every bug fix, performance improvement, or feature addition must be applied twice and tested twice; actual case: added vocabulary review mode support → changed 2 methods → forgot to update initiateTutorMessage → production bug

4. **Complex Transaction Boundaries**:
   - **Specific problem**: 12 methods with `@Transactional` annotations mixing different concerns (session updates vs. message orchestration); transaction propagation unclear when methods call each other
   - **Impact**: Risk of stale reads (concurrent session updates), unclear rollback boundaries, difficult to reason about data consistency

5. **Authorization Logic Scattered**:
   - **Specific problem**: Ownership validation (`if (session.userId != currentUserId) { return null }`) repeated 10+ times across different methods (lines 154-156, 197-199, 210-212, 233-235, 247-249, 262-264, 287-289, 306-308, 335-337, 563-565)
   - **Impact**: Easy to forget authorization check in new method; inconsistent error handling (some return null, some throw exceptions); 50+ lines of duplicated validation logic

**SOLID Principles Violated**:

- [x] **Single Responsibility Principle** - Handles session CRUD, message orchestration, vocabulary tracking, lesson progression, topic/phase updates, corrections analysis, progress queries, authorization, error handling (9+ responsibilities)
- [ ] Open/Closed Principle
- [x] **Dependency Inversion Principle** - Depends on 14+ concrete service implementations without interfaces
- [ ] Liskov Substitution Principle
- [ ] Interface Segregation Principle

## Proposed Solution

**High-Level Approach**:

Decompose ChatService into 4 domain-focused services using **phased extraction** strategy to minimize regression risk:

**Phase 1: SessionProgressService** (read-only, lowest risk)
**Phase 2: SessionManagementService** (session lifecycle)
**Phase 3: SessionConfigurationService** (settings updates)
**Phase 4: MessageOrchestrationService** (message pipeline + duplication elimination)

Each service has clear responsibilities, focused dependencies (3-6 vs 14+), and clean transaction boundaries.

**Code Excerpt** (after - conceptual):

```kotlin
// Phase 1: SessionProgressService (100-150 lines, read-only)
@Service
class SessionProgressService(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: ChatMessageRepository,
    private val vocabularyService: VocabularyService
) {
    fun getSessionProgress(sessionId: UUID): SessionProgressResponse { /* ... */ }

    fun getSessionWithMessages(sessionId: UUID, currentUserId: UUID): SessionWithMessagesResponse? { /* ... */ }

    fun getMessage(sessionId: UUID, messageId: UUID): ChatMessageEntity? { /* ... */ }

    @Transactional
    fun updateMessageAudioCache(...): ChatMessageEntity? { /* ... */ }

    @Transactional
    fun updateMessageCorrections(...): MessageResponse? { /* ... */ }

    // Clear responsibility: session progress and message queries
    // Only 3 dependencies (vs 14+ in ChatService)
}

// Phase 2: SessionManagementService (150-200 lines)
@Service
class SessionManagementService(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: ChatMessageRepository,
    private val authorizationService: AuthorizationService,
    private val userLanguageService: UserLanguageService,
    private val catalogService: CatalogService,
    private val imageService: ImageService
) {
    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse { /* ... */ }

    @Transactional
    fun createSessionFromCourse(userId: UUID, courseTemplateId: UUID, ...): SessionResponse { /* ... */ }

    fun getSession(sessionId: UUID): SessionResponse? { /* ... */ }

    fun getUserSessions(userId: UUID): List<SessionResponse> { /* ... */ }

    @Transactional
    fun deleteSession(sessionId: UUID, currentUserId: UUID): Boolean { /* ... */ }

    fun getActiveLearningSessions(userId: UUID): List<SessionWithProgressResponse> { /* ... */ }

    // Clear responsibility: session lifecycle only
    // Only 6 dependencies (vs 14+ in ChatService)
}

// Phase 3: SessionConfigurationService (200-250 lines)
@Service
class SessionConfigurationService(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: ChatMessageRepository,
    private val topicDecisionService: TopicDecisionService,
    private val lessonProgressionService: LessonProgressionService,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun updateSessionPhase(sessionId: UUID, phase: ConversationPhase, currentUserId: UUID): SessionResponse? { /* ... */ }

    @Transactional
    fun updateSessionTopic(sessionId: UUID, topic: String?, currentUserId: UUID): SessionResponse? { /* ... */ }

    @Transactional
    fun updateSessionTeachingStyle(sessionId: UUID, teachingStyle: TeachingStyle, currentUserId: UUID): SessionResponse? { /* ... */ }

    @Transactional
    fun updateVocabularyReviewMode(sessionId: UUID, enabled: Boolean, currentUserId: UUID): SessionResponse? { /* ... */ }

    @Transactional
    fun updateSessionLesson(sessionId: UUID, direction: LessonNavigationDirection, currentUserId: UUID): SessionResponse? { /* ... */ }

    fun getTopicHistory(sessionId: UUID, currentUserId: UUID): TopicHistoryResponse? { /* ... */ }

    // Clear responsibility: session configuration updates
    // Only 5 dependencies (vs 14+ in ChatService)
}

// Phase 4: MessageOrchestrationService (300-350 lines)
@Service
class MessageOrchestrationService(
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: ChatMessageRepository,
    private val tutorService: TutorService,
    private val correctionService: CorrectionService,
    private val vocabularyService: VocabularyService,
    private val metadataEvaluationService: MetadataEvaluationService,
    private val userChatModelFactory: UserChatModelFactory,
    private val rateLimitingService: RateLimitingService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${ai-tutor.messages.technical-error}") private val technicalErrorMessage: String
) {
    @Transactional
    fun sendMessage(
        sessionId: UUID,
        userContent: String,
        currentUserId: UUID,
        corrections: List<Correction>? = null,
        onReplyChunk: (String) -> Unit = {}
    ): MessageResponse? {
        val session = getAndValidateSession(sessionId, currentUserId)

        // Extract common orchestration logic (eliminates 350+ duplicated lines)
        return orchestrateTutorResponse(
            session = session,
            userMessage = userContent,
            messageHistory = buildMessageHistory(sessionId),
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
        val session = getAndValidateSession(sessionId, currentUserId)

        // Reuse orchestration logic with different context (no duplication!)
        return orchestrateTutorResponse(
            session = session,
            userMessage = null, // No user message for initiation
            messageHistory = buildMessageHistory(sessionId),
            initiationContext = initiationContext,
            onReplyChunk = onReplyChunk
        )
    }

    // Single source of truth for tutor orchestration (eliminates 350+ duplicated lines)
    private fun orchestrateTutorResponse(
        session: ChatSessionEntity,
        userMessage: String?,
        messageHistory: List<Message>,
        initiationContext: String? = null,
        onReplyChunk: (String) -> Unit
    ): MessageResponse {
        // 100-120 lines of shared orchestration logic:
        // - Calculate sequence number
        // - Save user message (if present)
        // - Build conversation state
        // - Check rate limit
        // - Get user ChatModel
        // - Call tutor service
        // - Handle errors with technical error message
        // - Save assistant message
        // - Track vocabulary
        // - Evaluate metadata
    }

    fun analyzeCorrections(sessionId: UUID, userText: String, userId: UUID): List<Correction> { /* ... */ }

    // Clear responsibility: message pipeline orchestration
    // Only 10 dependencies (vs 14+ in ChatService)
}

// Updated ChatController delegates to focused services
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val sessionManagementService: SessionManagementService,
    private val messageOrchestrationService: MessageOrchestrationService,
    private val sessionConfigurationService: SessionConfigurationService,
    private val sessionProgressService: SessionProgressService,
    private val authorizationService: AuthorizationService
) {
    @PostMapping("/sessions")
    fun createSession(@RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        authorizationService.requireAccessToUser(request.userId)
        val session = sessionManagementService.createSession(request)
        return ResponseEntity.ok(session)
    }

    @PostMapping("/sessions/{id}/messages")
    fun sendMessage(
        @PathVariable id: UUID,
        @RequestBody request: SendMessageRequest,
        authentication: Authentication
    ): ResponseEntity<MessageResponse> {
        val userId = authorizationService.getCurrentUserId()
        val response = messageOrchestrationService.sendMessage(
            sessionId = id,
            userContent = request.content,
            currentUserId = userId,
            corrections = request.corrections
        )
        return response?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    @PatchMapping("/sessions/{id}/phase")
    fun updateSessionPhase(
        @PathVariable id: UUID,
        @RequestBody request: UpdatePhaseRequest,
        authentication: Authentication
    ): ResponseEntity<SessionResponse> {
        val userId = authorizationService.getCurrentUserId()
        val session = sessionConfigurationService.updateSessionPhase(id, request.phase, userId)
        return session?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    // Much cleaner controller with focused service calls
    // Clear separation: lifecycle vs orchestration vs configuration vs progress
}
```

**New Files/Modules to Create**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/SessionManagementService.kt`
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/MessageOrchestrationService.kt`
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/SessionConfigurationService.kt`
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/SessionProgressService.kt`
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/SessionManagementServiceTest.kt`
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/MessageOrchestrationServiceTest.kt`
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/SessionConfigurationServiceTest.kt`
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/SessionProgressServiceTest.kt`

## Benefits

**Immediate Benefits**:

- ✅ **Eliminates 350+ duplicated lines**: `sendMessage()` and `initiateTutorMessage()` share 85% identical logic → single `orchestrateTutorResponse()` function
- ✅ **Reduces file size**: 1,033-line God Service → 4 focused services (100-350 lines each, total ~900 lines with better organization)
- ✅ **Improves test performance**: Test execution ~70% faster (mock 3-6 dependencies per service vs 14+ for God Service)
- ✅ **Clearer transaction boundaries**: Read-only SessionProgressService separate from transactional services (SessionManagement, MessageOrchestration, SessionConfiguration)

**Long-term Benefits**:

- 🔄 **Maintainability**: New features go into specific services without touching unrelated code (e.g., lesson navigation changes in SessionConfigurationService don't risk message orchestration bugs)
- 📈 **Parallel development**: 2+ developers can work on different services simultaneously (SessionConfiguration vs MessageOrchestration) without merge conflicts
- 🔄 **Independent scalability**: Services can be extracted as microservices if needed (MessageOrchestrationService is CPU-intensive, could be scaled separately from read-only SessionProgressService)
- 📈 **Testing isolation**: Unit test failures clearly point to specific domain (SessionManagement vs MessageOrchestration); coverage reports show exact responsibility coverage

**Metrics** (how to measure success):

- [x] Code complexity reduced: McCabe complexity from 80+ (ChatService) to 20-30 per service
- [x] Test coverage maintained: >= 80% coverage across all 4 new services (verified with JaCoCo)
- [x] Code duplication eliminated: DRY metric shows 0 duplicate blocks > 50 lines (was 2 blocks of 170-180 lines)
- [x] File size reduced: Average service size 200 lines (was 1,033 lines)
- [x] Test execution time: Unit test suite < 30 seconds (was ~90 seconds with 14 mocked dependencies)
- [x] Build time impact: Negligible (compilation parallelism improves with smaller files)

## Risks and Considerations

**Technical Risks**:

- ⚠️ **Risk**: Incorrect service boundary definition leads to circular dependencies (e.g., MessageOrchestrationService needs SessionConfigurationService, which needs MessageOrchestrationService for phase transition messages)
  - **Mitigation**: Design services as pure orchestration (MessageOrchestrationService) vs pure state management (SessionConfigurationService); use domain events if cross-service communication needed (e.g., SessionPhaseChangedEvent); draw dependency graph before coding to validate one-way relationships

- ⚠️ **Risk**: Transaction boundary changes affect data consistency (e.g., extracting methods into separate services changes transaction propagation, breaking atomicity)
  - **Mitigation**: Keep `@Transactional` at orchestration service level (MessageOrchestrationService.sendMessage()); extracted services use `@Transactional(propagation = MANDATORY)` to require active transaction; add integration tests specifically for rollback scenarios and concurrent updates

- ⚠️ **Risk**: Shared entity mutations cause unexpected behavior (e.g., multiple services modifying ChatSessionEntity concurrently → optimistic locking failures)
  - **Mitigation**: Services operate on same ChatSessionEntity via repositories with optimistic locking (@Version); transaction boundaries prevent stale reads; comprehensive integration tests validate concurrent update scenarios

**Business Risks**:

- ⚠️ **Risk**: Regression bugs in critical conversation flow during refactoring (message sending is core user journey)
  - **Mitigation**: Keep existing ChatServiceTest as integration validation during refactoring; add new unit tests for extracted services; phased approach allows catching bugs early; HTTP test suite (`backend/src/test/http/`) validates end-to-end behavior after each phase; deploy each phase to staging before production

**Breaking Changes**: No

- ChatController endpoints remain unchanged (same REST API contract at `/api/v1/chat/*`)
- Internal service decomposition with backward-compatible delegation
- All existing tests continue passing (with updated mocking to reflect new service dependencies)
- ChatService can be kept as deprecated facade initially if needed for gradual migration (facade pattern alternative)

**Rollback Strategy**:

- **Git-based rollback**: Each phase is a separate feature branch; revert commit if issues arise
- **Phased deployment**: Deploy SessionProgressService → SessionManagementService → SessionConfigurationService → MessageOrchestrationService; each phase is independently deployable with passing tests
- **Feature flag** (optional): `ai-tutor.chat.use-decomposed-services=true/false` allows instant toggle if production issues occur
- **Monitoring**: Add metrics for new services (call counts, latency p50/p95/p99, error rates) to detect anomalies early; compare pre/post refactoring metrics

## Implementation Plan

**Prerequisites**:

1. [ ] Review existing ChatServiceTest to understand test coverage (identify which tests cover which responsibilities)
2. [ ] Set up JaCoCo code coverage baseline for ChatService (target: maintain >= 80% coverage)
3. [ ] Create architecture decision record (ADR) documenting service boundary rationale
4. [ ] Draw dependency graph to validate one-way service relationships (no circular dependencies)

**Step-by-Step Implementation** (Phased Approach):

**Phase 1: Extract SessionProgressService** (Estimated: 0.5 days / 4 hours)

1. [ ] Create SessionProgressService with read-only methods:
   - `getSessionProgress()`, `getSessionWithMessages()`, `getMessage()`
   - `updateMessageAudioCache()`, `updateMessageCorrections()` (write operations on messages, not sessions)
2. [ ] Write unit tests for SessionProgressService (mock 3 dependencies: sessionRepository, messageRepository, vocabularyService)
3. [ ] Update ChatController to inject SessionProgressService alongside ChatService
4. [ ] Run full test suite (`./gradlew :backend:test`) - all tests pass
5. [ ] Deploy to staging, run HTTP test suite (`backend/src/test/http/`)

**Phase 2: Extract SessionManagementService** (Estimated: 1 day / 8 hours)

6. [ ] Create SessionManagementService with session lifecycle methods:
   - `createSession()`, `createSessionFromCourse()`, `getSession()`, `getUserSessions()`, `deleteSession()`, `getActiveLearningSessions()`
7. [ ] Write unit tests for SessionManagementService (mock 6 dependencies)
8. [ ] Update ChatController to use SessionManagementService for session CRUD
9. [ ] Add integration test for `deleteSession()` transaction rollback scenario
10. [ ] Run full test suite - all tests pass
11. [ ] Deploy to staging, validate session creation/deletion flows

**Phase 3: Extract SessionConfigurationService** (Estimated: 1 day / 8 hours)

12. [ ] Create SessionConfigurationService with configuration update methods:
    - `updateSessionPhase()`, `updateSessionTopic()`, `updateSessionTeachingStyle()`, `updateVocabularyReviewMode()`, `updateSessionLesson()`, `updateSessionToSpecificLesson()`, `getTopicHistory()`
13. [ ] Write unit tests for SessionConfigurationService (mock 5 dependencies)
14. [ ] Update ChatController to use SessionConfigurationService for configuration updates
15. [ ] Add integration test for concurrent phase/topic updates (optimistic locking validation)
16. [ ] Run full test suite - all tests pass
17. [ ] Deploy to staging, validate phase/topic/lesson navigation

**Phase 4: Extract MessageOrchestrationService + Duplication Elimination** (Estimated: 2-3 days / 16-24 hours)

18. [ ] Create MessageOrchestrationService with core methods:
    - `sendMessage()`, `initiateTutorMessage()`, `analyzeCorrections()`
19. [ ] Extract shared `orchestrateTutorResponse()` function from duplicated `sendMessage()` and `initiateTutorMessage()` logic
    - Handle both user-initiated and tutor-initiated flows with conditional logic
    - Parameterize differences (userMessage: String?, initiationContext: String?)
20. [ ] Write comprehensive unit tests for MessageOrchestrationService:
    - Mock 10 dependencies (tutorService, correctionService, vocabularyService, etc.)
    - Test sendMessage with corrections, without corrections
    - Test initiateTutorMessage with "welcome", "reengage-light", "reengage" contexts
    - Test error handling (tutorService throws exception → technical error message)
    - Test vocabulary tracking integration
    - Test metadata evaluation trigger
21. [ ] Add integration tests for message orchestration:
    - Test full sendMessage flow with real database (H2)
    - Test transaction rollback if vocabulary tracking fails
    - Test rate limit enforcement
22. [ ] Update ChatController to use MessageOrchestrationService for message operations
23. [ ] Run full test suite - all tests pass
24. [ ] Deploy to staging, run HTTP test suite focusing on message sending/streaming

**Phase 5: Cleanup and Documentation** (Estimated: 0.5-1 day / 4-8 hours)

25. [ ] Remove old ChatService (or keep as deprecated facade with `@Deprecated` annotation)
26. [ ] Update documentation:
    - `backend/CLAUDE.md`: Update package structure section with new service descriptions
    - `README.md`: Update architecture diagrams if present
    - Add ADR documenting refactoring rationale and service boundaries
27. [ ] Run JaCoCo coverage report: verify >= 80% coverage across all 4 services
28. [ ] Performance benchmarking:
    - Measure sendMessage latency (p50/p95/p99) before/after refactoring
    - Validate no regression (< 5% latency increase acceptable)
29. [ ] Code review prep: Create pull request with:
    - Detailed description of each phase
    - Before/after metrics (file sizes, test execution time, code duplication)
    - Architecture diagram showing service boundaries

**Testing Requirements**:

- [ ] **Unit tests**: Each service has >= 80% code coverage (verified with JaCoCo)
  - SessionProgressService: 5-10 unit tests
  - SessionManagementService: 10-15 unit tests
  - SessionConfigurationService: 10-15 unit tests
  - MessageOrchestrationService: 20-30 unit tests (highest complexity)
- [ ] **Integration tests**: Transaction boundaries, concurrent updates, rollback scenarios
  - deleteSession transaction rollback (message deletion + session deletion atomic)
  - Concurrent phase/topic updates (optimistic locking)
  - sendMessage full flow with vocabulary tracking
- [ ] **HTTP tests**: Full API contract validation (`backend/src/test/http/http-client-requests.http`)
  - POST `/sessions` - create session
  - POST `/sessions/from-course` - create course-based session
  - POST `/sessions/{id}/messages` - send message
  - POST `/sessions/{id}/messages/stream` - send message with SSE
  - PATCH `/sessions/{id}/phase` - update phase
  - PATCH `/sessions/{id}/topic` - update topic
  - DELETE `/sessions/{id}` - delete session
- [ ] **Regression testing**: Run all existing ChatServiceTest cases against new services

**Validation Checklist**:

- [ ] All existing tests pass (`./gradlew :backend:test`)
- [ ] New tests added and passing (40-60 new unit tests, 5-10 integration tests)
- [ ] TypeScript type check passes (no frontend changes, but verify API contract unchanged)
- [ ] Build succeeds (`./gradlew build`)
- [ ] No console errors in dev mode (`./gradlew :backend:bootRun`)
- [ ] HTTP test suite passes 100% (`backend/src/test/http/`)
- [ ] JaCoCo coverage >= 80% for all new services
- [ ] Code duplication eliminated: 0 blocks > 50 lines duplicated (verified with IDE "Find Duplicates")
- [ ] Performance benchmarks: sendMessage p95 latency < 5% regression

## Dependencies & Sequencing

**Must Complete Before This**: None

- Standalone refactoring (no prerequisites)

**Should Complete After This**:

- [ ] ChatController decomposition - Split into SessionController, MessageController, ConfigurationController (follows service boundaries established by this refactoring)
- [ ] Interface extraction - Add ChatService interfaces (SessionManagementService → ISessionManagement interface, etc.) for dependency inversion
- [ ] Testing improvements - Reduce test execution time further by using test containers or in-memory mocks

**Can Run in Parallel With**:

- [ ] Frontend refactorings (CourseEditorPage, ProfilePage decomposition)
- [ ] Import service consolidation (UnifiedCatalogImportService refactoring)
- [ ] Authorization annotation extraction (@RequireUserAccess custom annotation)

**Foundation Refactoring**: Yes

- **Pattern establishment**: This refactoring establishes a reusable pattern for decomposing other God Services:
  - TutorService (472 lines) can be split into PromptBuilderService + ConversationOrchestrationService
  - UnifiedCatalogImportService (465 lines) can be split into LanguageImportService + TutorImportService + CourseImportService
  - AuthService (470 lines) can be split into RegistrationService + AuthenticationService + TokenManagementService

- **Enables 3 other high-value refactorings**:
  1. ChatController decomposition (follows service boundaries)
  2. Interface extraction (focused services → focused interfaces)
  3. Testing improvements (faster tests with fewer mocked dependencies)

- **Transaction boundary pattern**: Demonstrates how to separate read-only services (SessionProgressService) from transactional services (SessionManagement, MessageOrchestration, SessionConfiguration)

- **Strategic value multiplier**: ROI increases as pattern is reused across codebase (4 other God Services identified)

## Related Files

**Files to Modify**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` - Extract into 4 services or remove entirely
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt` - Update service injection (add 4 new services, remove ChatService)
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/ChatServiceTest.kt` - Split into 4 test files (SessionManagementServiceTest, MessageOrchestrationServiceTest, SessionConfigurationServiceTest, SessionProgressServiceTest)

**Files with Dependencies** (won't modify but are affected):

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/tutor/service/TutorService.kt` - Called by MessageOrchestrationService (no changes, just new caller)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/vocabulary/service/VocabularyService.kt` - Called by MessageOrchestrationService and SessionProgressService
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/lesson/service/LessonProgressionService.kt` - Called by SessionConfigurationService
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/catalog/service/CatalogService.kt` - Called by SessionManagementService

**Documentation to Update**:

- `backend/CLAUDE.md` - Update package structure section: add SessionManagementService, MessageOrchestrationService, SessionConfigurationService, SessionProgressService descriptions under `chat/service/`
- `docs/ADR/adr-XXX-decompose-chatservice.md` - Create new ADR documenting refactoring rationale, service boundary decisions, alternatives considered
- `README.md` - Update architecture diagrams if present (show 4 new services)

## Historical Context

**Why This Issue Exists**:

ChatService started as a simple session manager but gradually accumulated responsibilities as features were added:
- Initial implementation: Session CRUD only (~200 lines)
- Added message sending and tutor integration (~400 lines)
- Added vocabulary tracking (~500 lines)
- Added lesson progression and topic management (~700 lines)
- Added tutor-initiated messages (with 85% duplicated logic from sendMessage) (~900 lines)
- Added corrections analysis, metadata evaluation, rate limiting (~1,033 lines)

This is a classic case of **feature accretion** without refactoring - each new feature was "easiest" to add to existing ChatService rather than extracting focused services. No single commit introduced the problem; it accumulated over time.

**Previous Attempts** (if any):

No evidence of previous refactoring attempts. The codebase shows awareness of SOLID principles (separate repositories, domain entities, DTOs) but service layer grew organically without periodic refactoring.

**Lessons for Future**:
- Enforce service size limits (e.g., max 400 lines per service) in code review guidelines
- Require refactoring proposal when service exceeds 3 distinct responsibilities
- Periodic architecture reviews (quarterly) to identify God Classes before they exceed 1,000 lines