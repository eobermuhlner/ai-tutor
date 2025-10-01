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
- **ChatController** - REST endpoints (`/api/v1/chat/*`)
  - POST `/sessions` - Create session
  - GET `/sessions?userId={id}` - List sessions
  - GET `/sessions/{id}` - Get session with messages
  - POST `/sessions/{id}/messages` - Send message
  - POST `/sessions/{id}/messages/stream` - Send message with SSE streaming
  - DELETE `/sessions/{id}` - Delete session
- **VocabularyController** - REST endpoints (`/api/v1/vocabulary/*`)
  - GET `/?userId={id}&lang={lang}` - Get user's vocabulary (optionally filtered by language)
  - GET `/{itemId}` - Get vocabulary item with all contexts
- **ChatService** - Session/message orchestration, integrates TutorService
- **ChatSessionRepository** / **ChatMessageRepository** - JPA persistence

### Core Components
- `TutorService` - Main tutoring logic with adaptive conversation phases (Free/Drill)
- `AiChatService` - AI chat integration with streaming responses
- `VocabularyService` - Vocabulary tracking with context and exposure counting

### Conversation Model
- **Phases**: Free (no corrections) or Drill (with corrections)
- **CEFR Levels**: A1-C2 language proficiency tracking
- **Error Detection**: Grammar, typo, and word choice corrections with explanations
- **Session Persistence**: Chat sessions and messages stored in H2 database

## Commands
- `./gradlew bootRun` - Run application (requires OPENAI_API_KEY)
- `./gradlew build` - Build project
- H2 Console: http://localhost:8080/h2-console
- HTTP Tests: `src/test/http/http-client-requests.http`

## Package Structure
```
ch.obermuhlner.aitutor
├── chat/                   # Chat REST API layer
│   ├── controller/         # ChatController (/api/v1/chat)
│   ├── service/            # ChatService
│   ├── repository/         # ChatSessionRepository, ChatMessageRepository
│   ├── domain/             # ChatSessionEntity, ChatMessageEntity
│   └── dto/                # API request/response DTOs
├── vocabulary/             # Vocabulary tracking
│   ├── controller/         # VocabularyController (/api/v1/vocabulary)
│   ├── service/            # VocabularyService
│   ├── repository/         # VocabularyItemRepository, VocabularyContextRepository
│   ├── domain/             # VocabularyItemEntity, VocabularyContextEntity
│   └── dto/                # Vocabulary response DTOs
├── tutor/service           # Core tutoring logic
├── conversation/service    # AI chat abstractions
└── core/                   # Models (Correction, Tutor, ConversationState, etc.)
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