# AI Tutor

An intelligent language learning platform powered by AI that provides personalized conversational tutoring with real-time error correction and vocabulary tracking.

## Features

- **Conversational AI Tutoring**: Natural language conversations with adaptive AI tutors
- **Custom Tutor Creation**: Users can create personalized tutors; admins can create global tutors
- **Real-time Error Detection**: Automatic correction of grammar, typography, and word choice errors
- **CEFR Level Tracking**: Monitors learner progress from A1 to C2 levels
- **Vocabulary Management**: Tracks new vocabulary with context and exposure frequency
- **Dual-Language Explanations**: Provides corrections in both source and target languages
- **Conversation Phases**:
  - **Free**: Pure fluency focus - no error tracking, maximum confidence building
  - **Correction**: Balanced learning - errors tracked for UI hover display, conversation flows naturally
  - **Drill**: Active accuracy work - explicit error discussion and correction practice
  - **Auto**: Intelligent phase selection based on learner performance and error patterns
- **Pedagogical Test Harness**: Automated quality assurance using LLM-as-judge methodology
- **Session Persistence**: Save and resume learning sessions
- **RESTful API**: Full REST API for integration with web and mobile applications

## Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.x
- OpenAI API key

### Docker Setup

Alternatively, you can run the entire application using Docker without installing Java or Gradle:

1. **Prerequisites for Docker:**
   - Docker Desktop or Docker Engine
   - Docker Compose

2. **Create a .env file** in the project root with your AI provider configuration:

```bash
# .env file
OPENAI_API_KEY=your-openai-api-key-here
JWT_SECRET=change-this-to-a-secure-random-key
```

3. **Build and run with Docker Compose:**
```bash
# Build and start all services (detached mode)
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

4. **Alternative: Build individual services:**
```bash
# Build only the backend
docker build -f backend/Dockerfile . -t ai-tutor-backend

# Build only the frontend  
docker build -f frontend/Dockerfile frontend -t ai-tutor-frontend
```

5. **Access the application:**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
   - API Documentation: http://localhost:8080/swagger-ui.html

### Standard Setup (without Docker)

- Java 17 or higher
- Gradle 8.x
- OpenAI API key

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd ai-tutor
```

2. Configure AI Provider (choose one):

**Option A: OpenAI (default)**
```bash
export OPENAI_API_KEY=your-api-key-here
```

**Option B: Azure OpenAI**
```bash
export AZURE_OPENAI_API_KEY=your-azure-key
export AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
```

Edit `backend/src/main/resources/application.yml` to uncomment Azure configuration:
```yaml
spring.ai.azure.openai.api-key: ${AZURE_OPENAI_API_KEY}
spring.ai.azure.openai.endpoint: ${AZURE_OPENAI_ENDPOINT}
spring.ai.azure.openai.chat.options.deployment-name: gpt-4o
```

**Option C: Ollama (local, no API key needed)**
```bash
# Start Ollama with a model (e.g., llama3, granite3.2:8b)
ollama run llama3
```

Edit `backend/src/main/resources/application.yml` to uncomment Ollama configuration:
```yaml
spring.ai.ollama.base-url: http://localhost:11434
spring.ai.ollama.chat.options.model: llama3
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
| GET | `/api/v1/chat/sessions/active?userId={uuid}` | Get active sessions with progress metrics (messageCount, vocabularyCount, daysActive) |
| GET | `/api/v1/chat/sessions/{id}` | Get session with full message history |
| GET | `/api/v1/chat/sessions/{id}/progress` | Get session progress metrics |
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

#### Summarization Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/summaries/sessions/{id}/info` | Get summary statistics for session (owner or admin) |
| GET | `/api/v1/summaries/sessions/{id}/details` | Get detailed summaries with text (admin only) |
| POST | `/api/v1/summaries/sessions/{id}/trigger` | Manually trigger summarization (admin only) |
| GET | `/api/v1/summaries/stats` | Get global summarization statistics (admin only) |

#### Error Analytics Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/errors/patterns?lang={code}&limit={n}` | Get top error patterns sorted by weighted severity score (default limit: 5) |
| GET | `/api/v1/analytics/errors/trends/{errorType}?lang={code}` | Get trend analysis for specific error type (IMPROVING/STABLE/WORSENING/INSUFFICIENT_DATA) |
| GET | `/api/v1/analytics/errors/samples?limit={n}` | Get recent error samples for debugging/UI display (default limit: 20) |

#### CEFR Assessment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/assessment/sessions/{id}/skills` | Get skill-specific CEFR breakdown (grammar, vocabulary, fluency, comprehension) for a session |
| POST | `/api/v1/assessment/sessions/{id}/reassess` | Trigger manual reassessment of all skill levels (for testing/debugging) |

#### OpenAPI Documentation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/swagger-ui.html` | Interactive Swagger UI for API documentation |
| GET | `/v3/api-docs` | OpenAPI 3.0 JSON specification |
| GET | `/v3/api-docs.yaml` | OpenAPI 3.0 YAML specification |

## Testing with IntelliJ HTTP Client

The project includes HTTP request examples in `backend/src/test/http/http-client-requests.http`.

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
│   ├── controller/     # Chat REST API endpoints, SummaryController
│   ├── service/        # Business logic & orchestration (ChatService, SummaryQueryService)
│   ├── repository/     # Data access layer (ChatSessionRepository, ChatMessageRepository, MessageSummaryRepository)
│   ├── domain/         # JPA entities (ChatSessionEntity, ChatMessageEntity, MessageSummaryEntity)
│   └── dto/            # API DTOs (CreateSessionRequest, SessionResponse, UpdateTopicRequest, etc.)
├── vocabulary/
│   ├── controller/     # Vocabulary REST API
│   ├── service/        # Vocabulary tracking (VocabularyService, VocabularyQueryService, VocabularyContextService)
│   ├── repository/     # Vocabulary persistence (VocabularyItemRepository, VocabularyContextRepository)
│   ├── domain/         # Vocabulary entities (VocabularyItemEntity, VocabularyContextEntity)
│   └── dto/            # Vocabulary DTOs (NewVocabularyDTO, VocabularyItemResponse, etc.)
├── analytics/
│   ├── controller/     # ErrorAnalyticsController (/api/v1/analytics/errors)
│   ├── service/        # ErrorAnalyticsService, ErrorPatternService
│   ├── repository/     # ErrorPatternRepository
│   ├── domain/         # ErrorPatternEntity
│   └── dto/            # ErrorPatternResponse, ErrorTrendResponse, ErrorSampleResponse
├── assessment/
│   ├── controller/     # AssessmentController (/api/v1/assessment)
│   ├── service/        # CEFRAssessmentService (heuristic-based skill assessment)
│   └── dto/            # SkillBreakdownResponse
├── tutor/
│   ├── service/        # Core tutoring logic (TutorService, PhaseDecisionService, TopicDecisionService)
│   └── domain/         # Tutor domain models (Tutor, ConversationState, ConversationResponse, ConversationPhase)
├── conversation/
│   ├── service/        # AI chat integration & streaming (AiChatService implementations)
│   └── dto/            # AI chat DTOs (AiChatRequest, AiChatResponse)
├── language/
│   └── service/        # Language utilities (LanguageService)
├── catalog/            # Catalog-based tutor/course management
│   ├── controller/     # CatalogController (/api/v1/catalog)
│   ├── service/        # CatalogService, SeedDataService (with curriculum validation)
│   ├── repository/     # TutorProfileRepository, CourseTemplateRepository
│   ├── domain/         # TutorProfileEntity, CourseTemplateEntity
│   └── dto/            # LanguageResponse, CourseResponse, CourseDetailResponse,
│                       # TutorResponse, TutorDetailResponse
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

Application configuration is in `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ai-tutor
  profiles:
    group:
      dev: h2

spring.ai.openai.chat.options.model: gpt-4o

# AI Tutor specific configuration
ai-tutor:
  chat:
    strict-schema-enforcement: true  # Enable strict JSON schema enforcement (default: true)
                                      # OpenAI: Uses native JSON_SCHEMA with strict=true
                                      # Ollama: Uses format parameter with schema map
```

Database configuration in `backend/src/main/resources/application-h2.yml`:

```yaml
spring.datasource.url: jdbc:h2:mem:testdb
spring.h2.console.enabled: true
spring.jpa.hibernate.ddl-auto: create-drop
```

### Docker Configuration

When running with Docker, configuration is managed through environment variables in the `.env` file and `docker-compose.yml`:

**Required configuration in `.env` file:**
```bash
# API Keys (at least one required)
OPENAI_API_KEY=your-openai-api-key
# AZURE_OPENAI_API_KEY=your-azure-api-key
# AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
# OLLAMA_BASE_URL=http://host.docker.internal:11434  # Uncomment if using Ollama

# Security (required)
JWT_SECRET=change-this-to-a-secure-random-key

# Optional defaults
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
DEMO_USERNAME=demo
DEMO_PASSWORD=demo
```

**Docker-specific configuration in `docker-compose.yml`:**
- Backend runs on port 8080
- Frontend runs on port 5173 (served via nginx on port 80)
- Health checks for both services
- Course content mounted as volume for easy updates
- Service dependency to ensure backend starts before frontend

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

### Building

```bash
# Build all modules (traditional method)
./gradlew build

# Build without tests
./gradlew build -x test

# Run backend tests
./gradlew :backend:test

# Run tests with coverage
./gradlew :backend:test :backend:jacocoTestReport

# Create bootable JAR
./gradlew :backend:bootJar
```

### Building with Docker

```bash
# Build Docker images for both services
docker-compose build

# Build only backend service
docker-compose build backend

# Build only frontend service
docker-compose build frontend

# Rebuild and start services
docker-compose up --build
```

### Running

```bash
# Run Spring Boot REST API server (traditional method)
./gradlew :backend:bootRun


# Run JAR
java -jar backend/build/libs/ai-tutor-0.0.1-SNAPSHOT.jar

# Run test harness
./gradlew :testharness:runTestHarness
```

### Running with Docker

```bash
# Start all services (detached mode)
docker-compose up -d

# Start all services (attached mode, shows logs)
docker-compose up

# Start specific service only
docker-compose up backend
docker-compose up frontend

# View logs from all services
docker-compose logs

# View logs from specific service
docker-compose logs backend
docker-compose logs frontend

# Stop all services
docker-compose down

# Stop all services and remove volumes
docker-compose down -v

# Rebuild and restart specific service
docker-compose up --build backend
```

**Note:** This project has multiple entry points:
- **REST API Server** (`:backend:bootRun`): Launches the Spring Boot backend at `http://localhost:8080`
- **Test Harness** (`:backend:runTestHarness`): Standalone pedagogical evaluation tool using LLM-as-judge

## Testing

The project includes comprehensive test coverage with unit, controller, and integration tests.

### Test Structure

```
backend/src/test/kotlin/
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
# Run all backend tests
./gradlew :backend:test

# Run specific test class
./gradlew :backend:test --tests ChatControllerTest

# Run with coverage report
./gradlew :backend:test :backend:jacocoTestReport
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

## Pedagogical Test Harness

The project includes a comprehensive test harness for evaluating tutor behavior using **LLM-as-judge** methodology. This provides automated quality assurance for pedagogical behaviors that traditional unit tests cannot capture.

### Overview

The test harness:
- **Simulates realistic learner conversations** with intentional errors at various CEFR levels
- **Uses LLM as a judge** to systematically evaluate pedagogical quality across 6 dimensions (supports OpenAI, Azure OpenAI, and Ollama)
- **Generates comprehensive reports** with quantitative scores and qualitative feedback
- **Validates expected behaviors** including phase transitions, error detection accuracy, and topic management
- **Supports CI/CD integration** with configurable pass/fail thresholds and exit codes

### Running the Test Harness

**Prerequisites:**
- Running AI Tutor server (`./gradlew :backend:bootRun`)
- AI provider configured (see Configuration section below for OpenAI, Azure OpenAI, or Ollama setup)
- Demo user registered (username: `demo`, password: `demo`)

**Basic Usage:**

```bash
# List available scenarios
./gradlew :testharness:runTestHarness --args="--list"

# Run all scenarios
./gradlew :testharness:runTestHarness

# Run specific scenario(s)
./gradlew :testharness:runTestHarness --args="--scenario beginner-errors"
./gradlew :testharness:runTestHarness --args="--scenario phase"  # All scenarios with "phase" in name

# Use custom configuration
./gradlew :testharness:runTestHarness --args="--config custom-config.yml"

# Get help
./gradlew :testharness:runTestHarness --args="--help"
```

**Example --list output:**
```
📚 Available Test Scenarios

Scenarios directory: scenarios

┌─────────────────────────────────────┬────────┬──────────┬─────────────────────────┐
│ Scenario ID                         │ Level  │ Language │ Focus                   │
├─────────────────────────────────────┼────────┼──────────┼─────────────────────────┤
│ beginner-agreement-errors           │ A1     │ es       │ ERROR_DETECTION         │
│ advanced-fluency-focus              │ C1     │ fr       │ ERROR_DETECTION         │
│ intermediate-mixed-errors           │ B1     │ de       │ ERROR_DETECTION         │
│ topic-management-test               │ B2     │ es       │ TOPIC_MANAGEMENT        │
│ critical-comprehension-errors       │ A2     │ it       │ ERROR_DETECTION         │
└─────────────────────────────────────┴────────┴──────────┴─────────────────────────┘

Total: 5 scenario(s)
```

**Exit Codes:**
- `0`: All scenarios passed (score ≥ threshold)
- `1`: One or more scenarios failed (score < threshold)

### Evaluation Dimensions

The LLM judge evaluates conversations across six dimensions (0-100 scale):

1. **Error Detection**: Accuracy of error identification and classification
2. **Phase Appropriateness**: Whether conversation phase matches error patterns
3. **Correction Quality**: Clarity and effectiveness of error explanations
4. **Encouragement Balance**: Balance between correction and motivation
5. **Topic Management**: Topic flow, variety, and coherence
6. **Vocabulary Teaching**: Appropriate introduction and reinforcement

### Built-in Test Scenarios

The harness includes 5 scenarios covering critical pedagogical behaviors:

| Scenario | CEFR Level | Language | Focus | Expected Behavior |
|----------|------------|----------|-------|-------------------|
| **beginner-agreement-errors** | A1 | Spanish | Repeated subject-verb errors | Detects fossilization → transitions to Drill phase |
| **advanced-fluency-focus** | C1 | French | Minor typography only | Maintains Free phase, no interruptions |
| **intermediate-mixed-errors** | B1 | German | Articles, cases, word order | Stays in Correction phase, passive feedback |
| **topic-management-test** | B2 | Spanish | Multiple topic changes | Smooth transitions, variety, no thrashing |
| **critical-comprehension-errors** | A2 | Italian | Critical vocabulary/tense errors | Immediate Drill intervention |

See `scenarios/README.md` for detailed scenario documentation.

### Creating Custom Scenarios

Create YAML files in the `scenarios/` directory:

```yaml
id: my-scenario
name: My Test Scenario
description: Description of what this tests
learnerPersona:
  name: TestLearner
  cefrLevel: B1
  sourceLanguage: en
  targetLanguage: es
  commonErrors:
    - Error type 1
  learningGoals:
    - Learning goal 1
tutorConfig:
  tutorName: TestTutor
  initialPhase: Auto
  teachingStyle: Guided
conversationScript:
  - content: "Learner message"
    intentionalErrors:
      - span: "error text"
        errorType: "Agreement"
        expectedSeverity: "Medium"
        correctForm: "correct text"
        reasoning: "Why this is an error"
expectedOutcomes:
  minimumCorrectionsDetected: 1
  shouldTriggerDrillPhase: false
evaluationFocus:
  - ERROR_DETECTION
  - PHASE_APPROPRIATENESS
```

### Configuration

Edit `testharness-config.yml`:

```yaml
# API Configuration
apiBaseUrl: http://localhost:8080
apiUsername: demo
apiPassword: demo

# Judge Configuration (LLM-as-judge)
# Supports multiple AI providers: openai, azure-openai, ollama
judgeProvider: openai              # AI provider (openai | azure-openai | ollama)
judgeModel: gpt-4o                 # Model name
judgeTemperature: 0.2              # Temperature (0.0-2.0)

# Optional: Override API configuration (defaults to environment variables)
# judgeApiKey: sk-...              # API key (or use OPENAI_API_KEY / AZURE_OPENAI_API_KEY / TESTHARNESS_JUDGE_API_KEY env var)
# judgeApiEndpoint: https://...    # API endpoint (or use provider defaults / TESTHARNESS_JUDGE_API_ENDPOINT)
# judgeDeploymentName: gpt-4o      # Azure OpenAI deployment name (or use AZURE_OPENAI_DEPLOYMENT / TESTHARNESS_JUDGE_DEPLOYMENT_NAME)

# Test Scenarios
scenariosPath: scenarios
reportsOutputDir: test-reports

# Test Execution
passThreshold: 70.0

# Rate Limiting (helps avoid API quota/rate limit errors)
delayBetweenRequestsMs: 1000       # Delay between consecutive requests (default: 1000ms = 1 req/sec)
maxRetries: 3                      # Maximum number of retries for 429/503 errors
retryBackoffMultiplier: 2.0        # Exponential backoff multiplier (delay = 1000 * multiplier^attempt)
```

**Provider Examples:**

**OpenAI (default):**
```yaml
judgeProvider: openai
judgeModel: gpt-4o
# Uses OPENAI_API_KEY environment variable
```

**Azure OpenAI:**
```yaml
judgeProvider: azure-openai
judgeModel: gpt-4o  # Not used for Azure (uses deployment name)
judgeDeploymentName: my-gpt4-deployment
judgeApiEndpoint: https://myresource.openai.azure.com
# Uses AZURE_OPENAI_API_KEY environment variable
```

**Ollama (local, no API key needed):**
```yaml
judgeProvider: ollama
judgeModel: llama3  # Or granite3.2:8b, etc.
judgeApiEndpoint: http://localhost:11434/api/chat
# No API key needed for local Ollama
```

### Reports

Reports are generated in `test-reports/` with timestamped filenames:

**Report Contents:**
- **Summary Table**: Overall and per-dimension scores for all scenarios
- **Technical Metrics**: Error detection accuracy, phase transitions, vocabulary counts
- **Judge Evaluation**: Detailed feedback for each of the 6 dimensions
- **Strengths & Improvements**: Specific recommendations from the LLM judge
- **Complete Transcripts**: Full conversations with error annotations
- **Overall Recommendations**: Priority improvements across all scenarios

**Sample Report Excerpt:**
```markdown
## Summary

| Scenario | Overall Score | Error Detection | Phase | Correction | ...
|----------|--------------|----------------|-------|------------|
| beginner-agreement-errors | 87.5 | 95.0 | 90.0 | 85.0 | ...
| advanced-fluency-focus | 92.0 | 88.0 | 98.0 | 90.0 | ...

## Overall Recommendations

**Priority Improvements:**
- Consider more explicit explanations for fossilized errors
- Maintain encouragement balance during intensive correction phases
```

### Architecture

The test harness is a standalone application built on:

```
testharness/
├── TestHarnessMain.kt        # Test harness entry point
├── config/                    # Configuration management
├── client/                    # REST API client
├── domain/                    # Scenario and result models
├── judge/                     # LLM-based evaluation
├── executor/                  # Test orchestration
├── scenario/                  # YAML scenario loading
└── report/                    # Markdown report generation
```

**Key Technologies:**
- Multi-provider AI integration (OpenAI, Azure OpenAI, Ollama) via direct HTTP clients
- Jackson YAML parsing for scenarios
- Java HTTP client for API communication
- Markdown generation for reports

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
