# AI Tutor

Language learning assistant with conversational AI tutoring and vocabulary tracking.

## Tech Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (JPA)
- **AI**: Spring AI 1.0.1 (OpenAI integration)
- **Build**: Gradle, Java 17

## Architecture

### REST API Layer
- **AuthController** - Authentication REST endpoints (`/api/v1/auth/*`)
  - POST `/register` - Register new user
  - POST `/login` - Login and get JWT tokens
  - POST `/refresh` - Refresh access token
  - POST `/logout` - Logout (invalidate refresh tokens)
  - GET `/me` - Get current user profile
  - POST `/password` - Change password
- **ChatController** - Chat REST endpoints (`/api/v1/chat/*`)
  - POST `/sessions` - Create session
  - GET `/sessions?userId={id}` - List sessions (omit userId for current user)
  - GET `/sessions/{id}` - Get session with messages
  - PATCH `/sessions/{id}/phase` - Update conversation phase (Free/Correction/Drill/Auto)
  - PATCH `/sessions/{id}/topic` - Update current conversation topic
  - GET `/sessions/{id}/topics/history` - Get topic history
  - POST `/sessions/{id}/messages` - Send message
  - POST `/sessions/{id}/messages/stream` - Send message with SSE streaming
  - DELETE `/sessions/{id}` - Delete session
- **VocabularyController** - Vocabulary REST endpoints (`/api/v1/vocabulary/*`)
  - GET `/?userId={id}&lang={lang}` - Get user's vocabulary (optionally filtered by language)
  - GET `/{itemId}` - Get vocabulary item with all contexts
- **ChatService** - Session/message orchestration, integrates TutorService
- **ChatSessionRepository** / **ChatMessageRepository** - JPA persistence
- **AuthService** / **AuthorizationService** / **JwtTokenService** - Authentication/authorization
- **UserService** / **UserRepository** - User management

### Core Components
- `TutorService` - Main tutoring logic with adaptive conversation phases (Free/Correction/Drill/Auto)
- `PhaseDecisionService` - Automatic phase selection based on learner error patterns
- `TopicDecisionService` - Conversation topic tracking with hysteresis (prevents topic thrashing)
- `AiChatService` - AI chat integration with streaming responses
- `VocabularyService` - Vocabulary tracking with context and exposure counting
- `AuthService` / `AuthorizationService` / `JwtTokenService` - JWT-based authentication
- `UserService` - User management and Spring Security integration

### Conversation Model
- **Phases** (3-phase pedagogical approach):
  - Free: Pure fluency focus, no error tracking
  - Correction: Errors tracked for UI hover, not mentioned in conversation (default)
  - Drill: Explicit error work with tutor discussion
  - Auto: Severity-weighted phase selection
- **Topic Management**:
  - Current topic tracked per session (e.g., "travel", "food")
  - Topic history prevents repetition (won't revisit last 3 topics)
  - Hysteresis rules: min 3 turns before change, max 12 turns before encouraging change
  - LLM proposes topics, TopicDecisionService validates with stability logic
- **Error Severity System** (chat-context aware):
  - Critical (3.0): Comprehension blocked
  - High (2.0): Global errors, significant barrier
  - Medium (1.0): Grammar issues, meaning clear
  - Low (0.3): Minor/chat-acceptable issues
  - Recognizes casual chat norms (missing accents, caps, punctuation)
- **Phase Decision Logic**: Severity-weighted scoring triggers phase transitions
- **CEFR Levels**: A1-C2 language proficiency tracking
- **Error Detection**: 9 error types with severity classification
- **UI Integration**: Corrections displayed as hover tooltips with severity indicators
- **Session Persistence**: Chat sessions and messages stored in H2 database
- **Authentication**: JWT-based with access/refresh tokens, Spring Security integration

## Commands
- `./gradlew runServer` - Run REST API server (requires OPENAI_API_KEY)
- `./gradlew runCli` - Run CLI client (connects to running server)
- `./gradlew bootRun` - Run REST API server (alternative to runServer)
- `./gradlew build` - Build project
- H2 Console: http://localhost:8080/h2-console
- HTTP Tests: `src/test/http/http-client-requests.http`

**Note:** Two independent entry points - REST API server and CLI client

## Package Structure
```
ch.obermuhlner.aitutor
├── auth/                   # Authentication and authorization
│   ├── controller/         # AuthController (/api/v1/auth)
│   ├── service/            # AuthService, JwtTokenService, AuthorizationService
│   └── dto/                # RegisterRequest, LoginRequest, LoginResponse, UserResponse,
│                           # RefreshTokenRequest, ChangePasswordRequest
├── user/                   # User management
│   ├── service/            # UserService, CustomUserDetailsService
│   ├── repository/         # UserRepository, RefreshTokenRepository
│   └── domain/             # UserEntity, RefreshTokenEntity, UserRole, AuthProvider
├── chat/                   # Chat REST API layer
│   ├── controller/         # ChatController (/api/v1/chat)
│   ├── service/            # ChatService
│   ├── repository/         # ChatSessionRepository, ChatMessageRepository
│   ├── domain/             # ChatSessionEntity, ChatMessageEntity, MessageRole
│   └── dto/                # CreateSessionRequest, SessionResponse, SendMessageRequest,
│                           # MessageResponse, SessionWithMessagesResponse, UpdatePhaseRequest,
│                           # UpdateTopicRequest, TopicHistoryResponse
├── vocabulary/             # Vocabulary tracking
│   ├── controller/         # VocabularyController (/api/v1/vocabulary)
│   ├── service/            # VocabularyService, VocabularyContextService, VocabularyQueryService
│   ├── repository/         # VocabularyItemRepository, VocabularyContextRepository
│   ├── domain/             # VocabularyItemEntity, VocabularyContextEntity
│   └── dto/                # NewVocabularyDTO, VocabularyItemResponse,
│                           # VocabularyContextResponse, VocabularyItemWithContextsResponse
├── tutor/                  # Tutoring logic and domain
│   ├── service/            # TutorService, PhaseDecisionService, TopicDecisionService
│   └── domain/             # Tutor, ConversationState, ConversationResponse, ConversationPhase
├── conversation/           # AI chat integration
│   ├── service/            # AiChatService (interface), SingleJsonEntityAiChatService,
│   │                       # StreamReplyThenJsonEntityAiChatService
│   └── dto/                # AiChatRequest, AiChatResponse
├── language/service        # LanguageService (language code validation)
├── cli/                    # Standalone CLI client
│   ├── AiTutorCli          # Main CLI application
│   ├── CliConfig           # Configuration management
│   └── HttpApiClient       # HTTP client for REST API
└── core/                   # Shared models and utilities
    ├── model/              # Shared domain models (CEFRLevel, ErrorType, ErrorSeverity,
    │                       # Correction, NewVocabulary, WordCard)
    └── util/               # Utilities (LlmJson, Placeholder)
```

## Development Guidelines

### When Adding New REST Endpoints
When adding new REST endpoints or modifying existing ones, **always update**:
1. **README.md** - Update API endpoint table and examples
2. **src/test/http/http-client-requests.http** - Add HTTP client test examples
3. **CLAUDE.md** - Update REST API Layer section and package structure
4. **http-client.env.json** - Add any new variables if needed

### When Making Code Changes
**Before committing any changes:**
1. **Run all tests**: `./gradlew test`
2. **Fix all failing tests** - Never commit with failing tests
3. **Add tests for new functionality** - Maintain test coverage
4. **Verify build succeeds**: `./gradlew build`

If tests fail, investigate and fix the root cause. Common issues:
- Mocked dependencies need updating
- Test data doesn't match new validation rules
- Missing or incorrect test configuration

### Preserving Existing Functionality
**CRITICAL: Never remove or break existing features without explicit user approval.**

When refactoring or adding features:
1. **Verify all existing REST endpoints still work** - Test with HTTP client
2. **Check existing tests pass** - Failing tests indicate broken functionality
3. **Review before deletion** - If removing code, ask user first
4. **Maintain backward compatibility** - Don't change existing API contracts
5. **Document breaking changes** - If unavoidable, clearly communicate impact

**Red flags that indicate functionality loss:**
- Deleted controller methods or endpoints
- Removed service methods still called elsewhere
- Changed DTO field names or types
- Modified database entity structure without migration
- Tests that were passing now fail or are deleted