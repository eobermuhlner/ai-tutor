# Task 0005: Implementation - End-to-End Controller Test

## Status
**State**: Planned
**Created**: 2025-10-04
**Target Coverage**: 80% (currently at 77%)

## Context

After adding comprehensive unit tests for `auth.exception` package (coverage: 19% → 97%), overall code coverage improved from 74% to 77%. We're still 3% short of the 80% JaCoCo threshold.

Analysis shows:
- `chat.service`: 63% coverage (448/1238 instructions missed)
- `auth.service`: 64% coverage
- `user.service`: 73% coverage
- `chat.controller`: 54% coverage

Large service classes with complex dependencies (Spring AI, repositories, external services) are difficult to unit test effectively. End-to-end controller tests with mocked AI implementation provide better coverage ROI.

## Objective

Add end-to-end controller tests that:
1. Test full HTTP request/response cycle
2. Cover controller logic (currently 54%)
3. Indirectly cover service logic through integration
4. Mock AI interactions to avoid external dependencies
5. Reach 80%+ code coverage

## Implementation Plan

### Step 1: Create Dummy AI Service Implementation

**File**: `/src/test/kotlin/ch/obermuhlner/aitutor/conversation/service/DummyAiChatService.kt`

Create a test double implementing `AiChatService` that returns predictable responses:

```kotlin
package ch.obermuhlner.aitutor.conversation.service

import ch.obermuhlner.aitutor.conversation.dto.AiChatRequest
import ch.obermuhlner.aitutor.conversation.dto.AiChatResponse
import ch.obermuhlner.aitutor.core.model.Correction
import ch.obermuhlner.aitutor.core.model.ErrorSeverity
import ch.obermuhlner.aitutor.core.model.ErrorType
import ch.obermuhlner.aitutor.core.model.NewVocabulary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("test")
class DummyAiChatService : AiChatService {

    var responseToReturn: AiChatResponse? = null
    var lastRequest: AiChatRequest? = null

    override fun call(request: AiChatRequest, onReplyText: (String) -> Unit): AiChatResponse? {
        lastRequest = request

        // Simulate streaming response
        val replyText = responseToReturn?.replyText ?: "Test response from AI tutor"
        onReplyText(replyText)

        return responseToReturn ?: createDefaultResponse(replyText)
    }

    fun reset() {
        responseToReturn = null
        lastRequest = null
    }

    private fun createDefaultResponse(replyText: String) = AiChatResponse(
        replyText = replyText,
        corrections = listOf(
            Correction(
                errorText = "helo",
                correctedText = "hello",
                explanation = "Spelling error",
                errorType = ErrorType.SPELLING,
                severity = ErrorSeverity.LOW
            )
        ),
        newVocabulary = listOf(
            NewVocabulary(
                word = "hello",
                translation = "hola",
                context = "greeting"
            )
        ),
        nextTopicSuggestion = null,
        currentTopicSuggestion = "greetings"
    )
}
```

**Key Features**:
- Implements `AiChatService` interface
- Returns configurable responses via `responseToReturn`
- Records last request for verification
- Simulates streaming via `onReplyText` callback
- Provides sensible default response with corrections and vocabulary
- `@Profile("test")` ensures it's only used in tests

### Step 2: Expand VocabularyControllerTest

**File**: `/src/test/kotlin/ch/obermuhlner/aitutor/vocabulary/controller/VocabularyControllerTest.kt`

Current coverage: 141 lines, 5 tests (limited to empty lists and not-found cases)

**Add 5 new end-to-end tests**:

```kotlin
@Test
@WithMockUser(username = "user1")
fun `getUserVocabulary should return multiple items with details`() {
    // Setup: Create vocabulary items with contexts
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val user = UserEntity(id = userId, username = "user1", email = "user1@example.com")
    val item1 = VocabularyItemEntity(
        id = UUID.randomUUID(),
        user = user,
        word = "hello",
        translation = "hola",
        targetLanguage = "Spanish",
        exposureCount = 3
    )
    val item2 = VocabularyItemEntity(
        id = UUID.randomUUID(),
        user = user,
        word = "goodbye",
        translation = "adiós",
        targetLanguage = "Spanish",
        exposureCount = 1
    )

    `when`(vocabularyItemRepository.findByUserId(userId))
        .thenReturn(listOf(item1, item2))
    `when`(vocabularyContextRepository.countByVocabularyItemId(item1.id!!))
        .thenReturn(2)
    `when`(vocabularyContextRepository.countByVocabularyItemId(item2.id!!))
        .thenReturn(1)

    // Execute
    mockMvc.perform(
        get("/api/v1/vocabulary")
            .param("userId", userId.toString())
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Int>(2)))
        .andExpect(jsonPath("$[0].word").value("hello"))
        .andExpect(jsonPath("$[0].translation").value("hola"))
        .andExpect(jsonPath("$[0].exposureCount").value(3))
        .andExpect(jsonPath("$[0].contextCount").value(2))
        .andExpect(jsonPath("$[1].word").value("goodbye"))
}

@Test
@WithMockUser(username = "user1")
fun `getUserVocabulary should filter by language`() {
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val user = UserEntity(id = userId, username = "user1", email = "user1@example.com")
    val spanishItem = VocabularyItemEntity(
        id = UUID.randomUUID(),
        user = user,
        word = "hello",
        translation = "hola",
        targetLanguage = "Spanish",
        exposureCount = 1
    )

    `when`(vocabularyItemRepository.findByUserIdAndTargetLanguage(userId, "Spanish"))
        .thenReturn(listOf(spanishItem))
    `when`(vocabularyContextRepository.countByVocabularyItemId(spanishItem.id!!))
        .thenReturn(1)

    mockMvc.perform(
        get("/api/v1/vocabulary")
            .param("userId", userId.toString())
            .param("lang", "Spanish")
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Int>(1)))
        .andExpect(jsonPath("$[0].targetLanguage").value("Spanish"))
}

@Test
@WithMockUser(username = "user1")
fun `getVocabularyItem should return item with all contexts`() {
    val itemId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val user = UserEntity(id = userId, username = "user1", email = "user1@example.com")
    val item = VocabularyItemEntity(
        id = itemId,
        user = user,
        word = "hello",
        translation = "hola",
        targetLanguage = "Spanish",
        exposureCount = 2
    )
    val context1 = VocabularyContextEntity(
        id = UUID.randomUUID(),
        vocabularyItem = item,
        sentence = "Hello world",
        sessionId = UUID.randomUUID(),
        timestamp = Instant.now()
    )
    val context2 = VocabularyContextEntity(
        id = UUID.randomUUID(),
        vocabularyItem = item,
        sentence = "Hello friend",
        sessionId = UUID.randomUUID(),
        timestamp = Instant.now()
    )

    `when`(vocabularyItemRepository.findById(itemId))
        .thenReturn(Optional.of(item))
    `when`(vocabularyContextRepository.findByVocabularyItemId(itemId))
        .thenReturn(listOf(context1, context2))

    mockMvc.perform(get("/api/v1/vocabulary/{itemId}", itemId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.word").value("hello"))
        .andExpect(jsonPath("$.translation").value("hola"))
        .andExpect(jsonPath("$.contexts", hasSize<Int>(2)))
        .andExpect(jsonPath("$.contexts[0].sentence").value("Hello world"))
        .andExpect(jsonPath("$.contexts[1].sentence").value("Hello friend"))
}

@Test
@WithMockUser(username = "user1")
fun `getUserVocabulary should return empty list when no items exist`() {
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    `when`(vocabularyItemRepository.findByUserId(userId))
        .thenReturn(emptyList())

    mockMvc.perform(
        get("/api/v1/vocabulary")
            .param("userId", userId.toString())
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Int>(0)))
}

@Test
@WithMockUser(username = "user1")
fun `getVocabularyItem should return 404 when item doesn't exist`() {
    val itemId = UUID.randomUUID()

    `when`(vocabularyItemRepository.findById(itemId))
        .thenReturn(Optional.empty())

    mockMvc.perform(get("/api/v1/vocabulary/{itemId}", itemId))
        .andExpect(status().isNotFound)
}
```

**Coverage Impact**: Tests full controller logic + service interactions

### Step 3: Expand ChatControllerTest

**File**: `/src/test/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatControllerTest.kt`

Current: 496 lines, 17 tests

**Add 8 new end-to-end tests** focusing on untested endpoints:

```kotlin
@Test
@WithMockUser(username = "testuser")
fun `createSessionFromCourse should create session with course template`() {
    val courseId = UUID.randomUUID()
    val request = CreateSessionFromCourseRequest(
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        courseId = courseId,
        sourceLanguage = "English"
    )
    val session = ChatSessionEntity(
        id = UUID.randomUUID(),
        userId = request.userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION,
        courseId = courseId,
        courseName = "Beginner Spanish",
        courseDescription = "Learn Spanish basics"
    )

    `when`(authorizationService.requireAccessToUser(request.userId))
        .thenReturn(Unit)
    `when`(chatService.createSessionFromCourse(request))
        .thenReturn(session)

    mockMvc.perform(
        post("/api/v1/chat/sessions/from-course")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
    )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.id").value(session.id.toString()))
        .andExpect(jsonPath("$.courseId").value(courseId.toString()))
        .andExpect(jsonPath("$.courseName").value("Beginner Spanish"))
}

@Test
@WithMockUser(username = "testuser")
fun `getSessionProgress should return progress with vocabulary and corrections`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION
    )

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)
    `when`(chatMessageRepository.countBySessionId(sessionId))
        .thenReturn(10L)
    `when`(vocabularyItemRepository.findBySessionId(sessionId))
        .thenReturn(emptyList())

    mockMvc.perform(get("/api/v1/chat/sessions/{sessionId}/progress", sessionId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.messageCount").value(10))
        .andExpect(jsonPath("$.vocabularyCount").value(0))
}

@Test
@WithMockUser(username = "testuser")
fun `updateSessionPhase should change conversation phase`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION
    )
    val updatedSession = session.copy(conversationPhase = ConversationPhase.DRILL)
    val request = UpdatePhaseRequest(ConversationPhase.DRILL)

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)
    `when`(chatService.updateSessionPhase(sessionId, ConversationPhase.DRILL))
        .thenReturn(updatedSession)

    mockMvc.perform(
        patch("/api/v1/chat/sessions/{sessionId}/phase", sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.conversationPhase").value("DRILL"))
}

@Test
@WithMockUser(username = "testuser")
fun `updateSessionTopic should change conversation topic`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION,
        currentTopic = "travel"
    )
    val updatedSession = session.copy(currentTopic = "food")
    val request = UpdateTopicRequest("food")

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)
    `when`(chatService.updateSessionTopic(sessionId, "food"))
        .thenReturn(updatedSession)

    mockMvc.perform(
        patch("/api/v1/chat/sessions/{sessionId}/topic", sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.currentTopic").value("food"))
}

@Test
@WithMockUser(username = "testuser")
fun `getTopicHistory should return past topics`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION,
        topicHistory = mutableListOf("travel", "food", "hobbies")
    )

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)

    mockMvc.perform(get("/api/v1/chat/sessions/{sessionId}/topics/history", sessionId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$.topics", hasSize<Int>(3)))
        .andExpect(jsonPath("$.topics[0]").value("travel"))
        .andExpect(jsonPath("$.topics[2]").value("hobbies"))
}

@Test
@WithMockUser(username = "testuser")
fun `deactivateSession should mark session inactive`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION,
        isActive = true
    )
    val inactiveSession = session.copy(isActive = false)

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)
    `when`(chatService.deactivateSession(sessionId))
        .thenReturn(inactiveSession)

    mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/deactivate", sessionId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.isActive").value(false))
}

@Test
@WithMockUser(username = "testuser")
fun `getActiveLearningSessions should return only active sessions with progress`() {
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val activeSession = ChatSessionEntity(
        id = UUID.randomUUID(),
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION,
        isActive = true
    )

    `when`(authorizationService.requireAccessToUser(userId))
        .thenReturn(Unit)
    `when`(chatSessionRepository.findByUserIdAndIsActiveTrue(userId))
        .thenReturn(listOf(activeSession))
    `when`(chatMessageRepository.countBySessionId(activeSession.id!!))
        .thenReturn(5L)
    `when`(vocabularyItemRepository.findBySessionId(activeSession.id!!))
        .thenReturn(emptyList())

    mockMvc.perform(
        get("/api/v1/chat/sessions/active")
            .param("userId", userId.toString())
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$", hasSize<Int>(1)))
        .andExpect(jsonPath("$[0].isActive").value(true))
        .andExpect(jsonPath("$[0].progress.messageCount").value(5))
}

@Test
@WithMockUser(username = "testuser")
fun `sendMessage should process message and return response`() {
    val sessionId = UUID.randomUUID()
    val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val session = ChatSessionEntity(
        id = sessionId,
        userId = userId,
        tutorName = "Maria",
        targetLanguage = "Spanish",
        sourceLanguage = "English",
        userLevel = CEFRLevel.B1,
        conversationPhase = ConversationPhase.CORRECTION
    )
    val request = SendMessageRequest("Hola, como estas?")
    val savedMessage = ChatMessageEntity(
        id = UUID.randomUUID(),
        sessionId = sessionId,
        role = MessageRole.ASSISTANT,
        content = "Muy bien, gracias!",
        timestamp = Instant.now()
    )

    `when`(chatSessionRepository.findById(sessionId))
        .thenReturn(Optional.of(session))
    `when`(authorizationService.requireAccessToSession(sessionId))
        .thenReturn(session)
    `when`(chatService.sendMessage(eq(sessionId), eq(request.content), any()))
        .thenAnswer { invocation ->
            val callback = invocation.getArgument<(String) -> Unit>(2)
            callback("Muy bien, gracias!")
            savedMessage
        }

    mockMvc.perform(
        post("/api/v1/chat/sessions/{sessionId}/messages", sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.content").value("Muy bien, gracias!"))
        .andExpect(jsonPath("$.role").value("ASSISTANT"))
}
```

**Coverage Impact**: Tests 8 previously untested controller methods + service paths

### Step 4: Create Integration Test (Optional)

**File**: `/src/test/kotlin/ch/obermuhlner/aitutor/chat/service/ChatServiceIntegrationTest.kt`

If Steps 1-3 don't reach 80%, add integration test with in-memory database:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.ai.openai.api-key=test-key"])
class ChatServiceIntegrationTest {

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatSessionRepository: ChatSessionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var dummyAiChatService: DummyAiChatService

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setup() {
        testUser = userRepository.save(
            UserEntity(
                id = UUID.randomUUID(),
                username = "testuser",
                email = "test@example.com",
                password = "password"
            )
        )
        dummyAiChatService.reset()
    }

    @Test
    fun `full conversation flow with vocabulary tracking`() {
        // Create session
        val request = CreateSessionFromCourseRequest(
            userId = testUser.id!!,
            courseId = UUID.randomUUID(),
            sourceLanguage = "English"
        )
        val session = chatService.createSessionFromCourse(request)

        assertNotNull(session.id)
        assertEquals(testUser.id, session.userId)

        // Configure AI response
        dummyAiChatService.responseToReturn = AiChatResponse(
            replyText = "Great! Let's practice greetings.",
            corrections = emptyList(),
            newVocabulary = listOf(
                NewVocabulary("hello", "hola", "greeting")
            ),
            nextTopicSuggestion = null,
            currentTopicSuggestion = "greetings"
        )

        // Send message
        val response = chatService.sendMessage(session.id!!, "I want to learn greetings") {}

        assertNotNull(response)
        assertEquals("Great! Let's practice greetings.", response.content)
        assertEquals(MessageRole.ASSISTANT, response.role)

        // Verify AI was called
        assertNotNull(dummyAiChatService.lastRequest)
        assertTrue(dummyAiChatService.lastRequest!!.conversationHistory.isNotEmpty())

        // Verify session updated
        val updatedSession = chatSessionRepository.findById(session.id!!).get()
        assertEquals("greetings", updatedSession.currentTopic)
    }

    @Test
    fun `phase transition from correction to drill mode`() {
        val session = createTestSession()

        // Configure AI with high-severity errors
        dummyAiChatService.responseToReturn = AiChatResponse(
            replyText = "You need to practice verb conjugation more.",
            corrections = listOf(
                Correction("I goes", "I go", "Verb conjugation", ErrorType.GRAMMAR, ErrorSeverity.HIGH),
                Correction("he go", "he goes", "Subject-verb agreement", ErrorType.GRAMMAR, ErrorSeverity.HIGH)
            ),
            newVocabulary = emptyList(),
            nextTopicSuggestion = null,
            currentTopicSuggestion = null
        )

        // Send multiple messages with errors
        repeat(3) {
            chatService.sendMessage(session.id!!, "I goes to school and he go too") {}
        }

        // Phase should auto-transition to DRILL based on error patterns
        val updatedSession = chatSessionRepository.findById(session.id!!).get()
        // Note: Actual phase logic depends on PhaseDecisionService implementation
        assertNotNull(updatedSession.conversationPhase)
    }

    private fun createTestSession(): ChatSessionEntity {
        return chatSessionRepository.save(
            ChatSessionEntity(
                userId = testUser.id!!,
                tutorName = "Maria",
                targetLanguage = "Spanish",
                sourceLanguage = "English",
                userLevel = CEFRLevel.B1,
                conversationPhase = ConversationPhase.CORRECTION
            )
        )
    }
}
```

**Coverage Impact**: Tests actual service integration paths

## Expected Coverage Gain

**Current Coverage**: 77% (1553/2017 instructions)

**Target Coverage**: 80%+ (1614+ instructions)

**Coverage gain needed**: 61+ instructions

### Estimated Impact by Step:

1. **VocabularyController** (+5 tests): ~30 instructions
2. **ChatController** (+8 tests): ~50 instructions
3. **Service logic** (indirect): ~40 instructions
4. **Integration test** (if needed): ~20 instructions

**Total estimated**: ~140 instructions covered

**Expected final coverage**: ~82%

## Implementation Order

1. Create `DummyAiChatService.kt`
2. Expand `VocabularyControllerTest.kt` (+5 tests)
3. Run tests, check coverage: `./gradlew test jacocoTestReport`
4. Expand `ChatControllerTest.kt` (+8 tests)
5. Run tests, check coverage
6. If < 80%, add `ChatServiceIntegrationTest.kt`
7. Run final build: `./gradlew build`

## Verification Steps

After each phase:
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

Check:
- Overall instruction coverage percentage
- `ch.obermuhlner.aitutor.chat.service` package coverage
- `ch.obermuhlner.aitutor.chat.controller` package coverage
- `ch.obermuhlner.aitutor.vocabulary.controller` package coverage

Final verification:
```bash
./gradlew build
```

Should pass JaCoCo verification without errors.

## Notes

- **Test Isolation**: Each test uses `@BeforeEach` setup and mock resets
- **Authentication**: `@WithMockUser` provides Spring Security context
- **Serialization**: `objectMapper.writeValueAsString()` for request bodies
- **JSON Assertions**: `jsonPath()` for response verification
- **Mocking**: Mockito for repositories, DummyAiChatService for AI
- **SSE Testing**: Not covered (requires special MockMvc configuration)

## Risks

- **SSE endpoint untested**: `/messages/stream` requires special setup
- **Async behavior**: Streaming callbacks may need `@Async` test support
- **Database state**: Integration tests may have flaky cleanup
- **Coverage calculation**: JaCoCo may not count all indirect paths

## Alternatives Considered

1. **Mock-heavy unit tests**: Rejected - too many mocks, brittle tests
2. **Testcontainers + real DB**: Rejected - slow, complex setup
3. **Mock AI API calls**: Rejected - external dependency, flaky
4. **Current approach**: ✅ Best balance of coverage, speed, and maintainability
