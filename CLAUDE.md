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
├── chat/                   # REST API layer
│   ├── controller/         # ChatController (/api/v1/chat)
│   ├── service/            # ChatService
│   ├── repository/         # ChatSessionRepository, ChatMessageRepository
│   ├── domain/             # ChatSessionEntity, ChatMessageEntity
│   └── dto/                # API request/response DTOs
├── tutor/service           # Core tutoring logic
├── conversation/service    # AI chat abstractions
├── vocabulary/             # Vocabulary tracking (domain/repository/service)
└── core/                   # Models (Correction, Tutor, ConversationState, etc.)
```