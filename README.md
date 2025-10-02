# AI Tutor

An intelligent language learning platform powered by AI that provides personalized conversational tutoring with real-time error correction and vocabulary tracking.

## Features

- **Conversational AI Tutoring**: Natural language conversations with adaptive AI tutors
- **Real-time Error Detection**: Automatic correction of grammar, typography, and word choice errors
- **CEFR Level Tracking**: Monitors learner progress from A1 to C2 levels
- **Vocabulary Management**: Tracks new vocabulary with context and exposure frequency
- **Dual-Language Explanations**: Provides corrections in both source and target languages
- **Conversation Phases**:
  - **Free**: Pure fluency focus - no error tracking, maximum confidence building
  - **Correction**: Balanced learning - errors tracked for UI hover display, conversation flows naturally
  - **Drill**: Active accuracy work - explicit error discussion and correction practice
  - **Auto**: Intelligent phase selection based on learner performance and error patterns
- **Session Persistence**: Save and resume learning sessions
- **RESTful API**: Full REST API for integration with web and mobile applications

## Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.x
- OpenAI API key

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd ai-tutor
```

2. Set your OpenAI API key:
```bash
export OPENAI_API_KEY=your-api-key-here
```

3. Build the project:
```bash
./gradlew build
```

4. Run the application:
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`.

## API Usage

### Authentication

The API uses JWT-based authentication. First, register and login to get access tokens:

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demouser",
    "email": "demo@example.com",
    "password": "DemoPassword123",
    "firstName": "Demo",
    "lastName": "User"
  }'

# Login to get access tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demouser",
    "password": "DemoPassword123"
  }'
```

The login response includes `accessToken` and `refreshToken`. Use the access token in the `Authorization: Bearer {token}` header for all subsequent requests.

### Create a Learning Session

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "userId": "{your-user-id}",
    "tutorName": "Maria",
    "sourceLanguageCode": "en",
    "targetLanguageCode": "es",
    "conversationPhase": "Auto",
    "estimatedCEFRLevel": "A1"
  }'
```

### Send a Message

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions/{sessionId}/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{"content": "Hola, como estas?"}'
```

### Get Session History

```bash
curl http://localhost:8080/api/v1/chat/sessions/{sessionId} \
  -H "Authorization: Bearer {accessToken}"
```

### API Endpoints

#### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token using refresh token |
| POST | `/api/v1/auth/logout` | Logout (invalidates refresh tokens) |
| GET | `/api/v1/auth/me` | Get current user profile |
| POST | `/api/v1/auth/password` | Change password |

#### Chat Session Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/chat/sessions` | Create new learning session |
| GET | `/api/v1/chat/sessions?userId={uuid}` | List user's sessions (omit userId for current user) |
| GET | `/api/v1/chat/sessions/{id}` | Get session with full message history |
| PATCH | `/api/v1/chat/sessions/{id}/phase` | Update conversation phase (Free/Correction/Drill/Auto) |
| PATCH | `/api/v1/chat/sessions/{id}/topic` | Update current conversation topic |
| GET | `/api/v1/chat/sessions/{id}/topics/history` | Get conversation topic history |
| POST | `/api/v1/chat/sessions/{id}/messages` | Send message (JSON response) |
| POST | `/api/v1/chat/sessions/{id}/messages/stream` | Send message (SSE streaming) |
| DELETE | `/api/v1/chat/sessions/{id}` | Delete session |

#### Vocabulary Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/vocabulary?userId={uuid}&lang={code}` | Get user's vocabulary (optional language filter) |
| GET | `/api/v1/vocabulary/{itemId}` | Get vocabulary item with all contexts |

## Testing with IntelliJ HTTP Client

The project includes HTTP request examples in `src/test/http/http-client-requests.http`.

1. Open the file in IntelliJ IDEA
2. Select environment: `local 8080`
3. Run "Create Chat Session" to generate a session
4. The session ID is automatically captured for subsequent requests
5. Execute other requests to interact with the API

## Architecture

### Technology Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (in-memory, JPA)
- **AI Integration**: Spring AI 1.0.1 with OpenAI
- **Build Tool**: Gradle

### Core Components

```
ch.obermuhlner.aitutor
├── auth/
│   ├── controller/     # Authentication REST API (register, login, refresh, logout, password)
│   ├── service/        # Auth services (AuthService, JwtTokenService, AuthorizationService)
│   └── dto/            # Auth DTOs (RegisterRequest, LoginRequest, LoginResponse, etc.)
├── user/
│   ├── service/        # User management (UserService, CustomUserDetailsService)
│   ├── repository/     # User persistence (UserRepository, RefreshTokenRepository)
│   └── domain/         # User entities (UserEntity, RefreshTokenEntity, UserRole, AuthProvider)
├── chat/
│   ├── controller/     # Chat REST API endpoints
│   ├── service/        # Business logic & orchestration (ChatService)
│   ├── repository/     # Data access layer (ChatSessionRepository, ChatMessageRepository)
│   ├── domain/         # JPA entities (ChatSessionEntity, ChatMessageEntity, MessageRole)
│   └── dto/            # API DTOs (CreateSessionRequest, SessionResponse, UpdateTopicRequest, etc.)
├── vocabulary/
│   ├── controller/     # Vocabulary REST API
│   ├── service/        # Vocabulary tracking (VocabularyService, VocabularyQueryService, VocabularyContextService)
│   ├── repository/     # Vocabulary persistence (VocabularyItemRepository, VocabularyContextRepository)
│   ├── domain/         # Vocabulary entities (VocabularyItemEntity, VocabularyContextEntity)
│   └── dto/            # Vocabulary DTOs (NewVocabularyDTO, VocabularyItemResponse, etc.)
├── tutor/
│   ├── service/        # Core tutoring logic (TutorService, PhaseDecisionService, TopicDecisionService)
│   └── domain/         # Tutor domain models (Tutor, ConversationState, ConversationResponse, ConversationPhase)
├── conversation/
│   ├── service/        # AI chat integration & streaming (AiChatService implementations)
│   └── dto/            # AI chat DTOs (AiChatRequest, AiChatResponse)
├── language/
│   └── service/        # Language utilities (LanguageService)
├── cli/                # Standalone CLI client (AiTutorCli, HttpApiClient, CliConfig)
└── core/
    ├── model/          # Shared domain models (CEFRLevel, ErrorType, ErrorSeverity,
    │                   # Correction, NewVocabulary, WordCard)
    └── util/           # Shared utilities (LlmJson, Placeholder)
```

### Key Design Patterns

- **Repository Pattern**: Clean separation of data access
- **Service Layer**: Business logic isolation
- **DTO Pattern**: API contract separation from domain models
- **Strategy Pattern**: Pluggable AI chat service implementations

## Development

### Configuration

Application configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ai-tutor
  profiles:
    group:
      dev: h2

spring.ai.openai.chat.options.model: gpt-4o
```

Database configuration in `src/main/resources/application-h2.yml`:

```yaml
spring.datasource.url: jdbc:h2:mem:testdb
spring.h2.console.enabled: true
spring.jpa.hibernate.ddl-auto: create-drop
```

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

### Building

```bash
# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Create bootable JAR
./gradlew bootJar
```

### Running

```bash
# Run Spring Boot REST API server
./gradlew runServer
# (or use the standard task: ./gradlew bootRun)

# Run standalone CLI client
./gradlew runCli
# (or use the standard task: ./gradlew run)

# Run JAR
java -jar build/libs/ai-tutor-0.0.1-SNAPSHOT.jar
```

**Note:** This project has two entry points:
- **REST API Server** (`runServer`/`bootRun`): Launches the Spring Boot backend at `http://localhost:8080`
- **CLI Client** (`runCli`/`run`): Launches the interactive command-line interface that connects to the API server

## Testing

The project includes comprehensive test coverage with unit, controller, and integration tests.

### Test Structure

```
src/test/kotlin/
├── chat/
│   ├── controller/    # REST API tests (@WebMvcTest)
│   └── service/       # Business logic tests (MockK)
├── vocabulary/
│   └── controller/    # Vocabulary API tests
├── config/            # Test configuration
└── fixtures/          # Test data factories
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ChatControllerTest

# Run with coverage report
./gradlew test jacocoTestReport
```

### Test Categories

- **Controller Tests** (`@WebMvcTest`): Test REST endpoints with MockMvc
- **Service Tests** (MockK): Test business logic with mocked dependencies
- **Integration Tests**: Test complete flows with real database

### Mocking Strategy

- ✅ OpenAI API is mocked in tests (configured in `TestConfig`)
- ✅ Services are mocked in controller tests
- ✅ Use `TestDataFactory` for consistent test data

## Data Model

### Chat Session
- User ID, tutor configuration (name, persona, domain)
- Language pair (source → target)
- Conversation state (phase, CEFR level)
- Timestamps

### Chat Message
- Session reference
- Role (USER/ASSISTANT)
- Content
- Corrections (JSON)
- Vocabulary (JSON)
- Timestamp

### Vocabulary Item
- User ID, language, lemma
- Exposure count
- Last seen timestamp
- Context examples

## Learning Concepts

### CEFR Levels

The system tracks language proficiency using the Common European Framework of Reference:
- **A1/A2**: Beginner
- **B1/B2**: Intermediate
- **C1/C2**: Advanced

### Conversation Phases

The system uses a three-phase approach based on language acquisition research:

#### **Free Phase** - Pure Fluency Focus
- **Goal**: Maximum output, zero pressure, confidence building
- **Behavior**: No error tracking, conversation flows completely naturally
- **UI**: No corrections displayed
- **Best for**: Beginners, warm-up, confidence building, low-stakes practice
- **Pedagogy**: Reduces affective filter, encourages risk-taking, builds communication confidence

#### **Correction Phase** - Balanced Learning (Default)
- **Goal**: Awareness without interruption
- **Behavior**: All errors detected and tracked, but never mentioned in conversation
- **UI**: Errors shown as subtle hover tooltips in chat
- **Best for**: General practice, intermediate learners, autonomous learning
- **Pedagogy**: Supports "noticing hypothesis" - learners discover errors at their own pace, maintains conversation flow while providing accountability

#### **Drill Phase** - Active Accuracy Work
- **Goal**: Explicit error correction and accuracy improvement
- **Behavior**: Tutor actively discusses errors, prompts self-correction, provides explanations
- **UI**: Both hover tooltips AND explicit discussion in conversation
- **Best for**: Advanced learners, exam prep, addressing fossilized errors
- **Pedagogy**: Combines immediate feedback within "cognitive window" (<1 min) with self-correction opportunities for maximum retention

#### **Auto Mode** - Intelligent Phase Selection
The system automatically selects the optimal phase using **severity-weighted scoring**:

**Severity Weights:**
- Critical errors: 3.0 (blocks comprehension)
- High errors: 2.0 (global errors)
- Medium errors: 1.0 (grammar issues)
- Low errors: 0.3 (minor/chat-acceptable)

**Phase Transitions:**
- **Starts with**: Correction (balanced default)
- **Switches to Drill** if:
  - 2+ Critical errors in last 3 messages, OR
  - 3+ High-severity repeated errors (fossilization risk), OR
  - Weighted severity score ≥ 6.0 in last 5 messages
- **Switches to Free** if:
  - Only Low-severity errors in last 3 messages, AND
  - Weighted severity score < 1.0 in last 5 messages
- **Returns to Correction**: Default middle ground for moderate error patterns

You can manually override Auto mode at any time via the API to match learning goals.

### Conversation Topics

The system tracks conversation topics to provide structure and variety:

- **Current Topic**: The topic being discussed (e.g., "travel", "food", "weather")
- **Topic History**: Past topics to avoid repetition
- **Topic Hysteresis**: Prevents topic thrashing with configurable thresholds
  - Minimum 3 turns before topic can change (stability)
  - Maximum 12 turns before encouraging topic change (variety)
  - Won't revisit topics from last 3 topic changes (prevents repetition)

The AI tutor proposes topics naturally, and the `TopicDecisionService` validates changes to ensure stable, varied conversations. You can also manually set topics via the API.

### Error Types and Severity

The system classifies errors by both **type** and **severity** to provide appropriate feedback:

#### Error Types
- **TenseAspect**: Wrong tense or aspect
- **Agreement**: Subject-verb, gender, number agreement
- **WordOrder**: Syntax, misplaced words/clauses
- **Lexis**: Wrong vocabulary, false friends, register issues
- **Morphology**: Incorrect endings, cases, conjugations
- **Articles**: Missing/wrong/unnecessary articles or determiners
- **Pronouns**: Wrong form or reference
- **Prepositions**: Wrong or missing prepositions
- **Typography**: Spelling, diacritics, capitalization, punctuation

#### Error Severity (Context-Aware)
- **Critical**: Meaning completely lost, comprehension impossible
- **High**: Significant comprehension barrier (global errors) - native speaker confused
- **Medium**: Grammar error but meaning clear from context
- **Low**: Minor issue or acceptable in casual chat/texting (local errors)

#### Chat Context Intelligence
The system recognizes that casual chat communication differs from formal writing:
- Missing accents (café → cafe): **Low** severity in chat
- No capitalization: **Low** or ignored (chat norm)
- Missing end punctuation: **Low** or ignored (chat norm)
- Quick typos: **Low** (if meaning clear)

**Philosophy**: "Would a native speaker make this 'error' in casual texting?" If yes → Low severity or ignored

This ensures learners aren't penalized for typing naturally while still tracking genuine learning errors.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with [Spring AI](https://spring.io/projects/spring-ai)
- Powered by [OpenAI](https://openai.com/)
- Language framework based on [CEFR](https://www.coe.int/en/web/common-european-framework-reference-languages)

## Contact

For questions or support, please open an issue on GitHub.
