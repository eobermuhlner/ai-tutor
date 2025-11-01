# AI Tutor

Language learning assistant with conversational AI tutoring and vocabulary tracking.

## Tech Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (JPA)
- **AI**: Spring AI 1.0.1 (multi-provider support: OpenAI, Azure OpenAI, Ollama)
- **Build**: Gradle (build.gradle), Java 17
- **Logging**: SLF4J (via Spring Boot starter)

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
  - POST `/sessions` - Create session (legacy)
  - POST `/sessions/from-course` - Create session from course template
  - GET `/sessions/active?userId={id}` - Get active learning sessions with progress
  - GET `/sessions?userId={id}` - List sessions (omit userId for current user)
  - GET `/sessions/{id}` - Get session with messages
  - GET `/sessions/{id}/progress` - Get session progress
  - PATCH `/sessions/{id}/phase` - Update conversation phase (Free/Correction/Drill/Auto)
  - PATCH `/sessions/{id}/topic` - Update current conversation topic
  - PATCH `/sessions/{id}/teaching-style` - Update session teaching style (Reactive/Guided/Directive)
  - GET `/sessions/{id}/topics/history` - Get topic history
  - POST `/sessions/{id}/messages` - Send message
  - POST `/sessions/{id}/messages/stream` - Send message with SSE streaming
  - POST `/sessions/{id}/messages/initiate` - Tutor initiates message without user input (welcome/re-engagement)
  - POST `/sessions/{id}/messages/initiate/stream` - Tutor initiates message with SSE streaming
  - DELETE `/sessions/{id}` - Delete session
- **CatalogController** - Catalog browsing REST endpoints (`/api/v1/catalog/*`)
  - GET `/languages?sourceLanguage={lang}` - List available languages
  - GET `/languages/{code}/courses?sourceLanguage={lang}&userLevel={level}` - List courses for language
  - GET `/languages/{code}/tutors?sourceLanguage={lang}` - List tutors for language (global + user's custom tutors)
  - GET `/courses/{id}?sourceLanguage={lang}` - Get course details
  - GET `/tutors/{id}?sourceLanguage={lang}` - Get tutor details (if visible to user)
  - POST `/tutors` - Create custom tutor (authenticated; admins can create global tutors with `isGlobal: true`)
- **UserLanguageController** - User language management (`/api/v1/users/{userId}/languages/*`)
  - GET `/` - Get user's language proficiencies
  - POST `/` - Add language proficiency
  - PATCH `/{languageCode}` - Update language proficiency level
  - PATCH `/{languageCode}/set-primary` - Set primary language
  - DELETE `/{languageCode}` - Remove language proficiency
- **VocabularyController** - Vocabulary REST endpoints (`/api/v1/vocabulary/*`)
  - GET `/?userId={id}&lang={lang}` - Get user's vocabulary (optionally filtered by language)
  - GET `/{itemId}` - Get vocabulary item with all contexts
- **SummaryController** - Summarization monitoring REST endpoints (`/api/v1/summaries/*`)
  - GET `/sessions/{id}/info` - Get summary statistics for session (owner or admin)
  - GET `/sessions/{id}/details` - Get detailed summaries with text (admin only)
  - POST `/sessions/{id}/trigger` - Manually trigger summarization (admin only)
  - GET `/stats` - Get global summarization statistics (admin only)
- **ErrorAnalyticsController** - Error analytics REST endpoints (`/api/v1/analytics/*`)
  - GET `/errors/patterns?lang={code}&limit={n}` - Get top error patterns sorted by weighted score
  - GET `/errors/trends/{errorType}?lang={code}` - Get trend analysis (IMPROVING/STABLE/WORSENING/INSUFFICIENT_DATA)
  - GET `/errors/samples?limit={n}` - Get recent error samples for debugging
- **AssessmentController** - CEFR skill assessment REST endpoints (`/api/v1/assessment/*`)
  - GET `/sessions/{id}/skills` - Get skill-specific CEFR breakdown (grammar, vocabulary, fluency, comprehension)
  - POST `/sessions/{id}/reassess` - Trigger manual reassessment of all skill levels
- **ChatService** - Session/message orchestration, integrates TutorService, supports tutor-initiated messages
- **CatalogService** - Browse languages, courses, and tutors with localization
- **UserLanguageService** - Manage user's language proficiency profiles
- **LocalizationService** - Handle multilingual content with AI translation fallback
- **TranslationService** - AI-powered translation (OpenAI)
- **SeedDataService** - Database seed service with curriculum validation to ensure all configured courses have corresponding curriculum files at startup
- **ChatSessionRepository** / **ChatMessageRepository** - JPA persistence
- **TutorProfileRepository** / **CourseTemplateRepository** - Catalog persistence with user-specific custom tutor support
- **UserLanguageProficiencyRepository** - User language profiles
- **AuthService** / **AuthorizationService** / **JwtTokenService** - Authentication/authorization
- **UserService** / **UserRepository** - User management

### Core Components
- `TutorService` - Main tutoring logic with adaptive conversation phases (Free/Correction/Drill/Auto)
- `PhaseDecisionService` - Automatic phase selection based on learner error patterns
- `TopicDecisionService` - Conversation topic tracking with hysteresis (prevents topic thrashing)
- `ProgressiveSummarizationService` - Hierarchical message summarization with async execution
- `MessageCompactionService` - Context compaction using progressive summaries
- `SummaryQueryService` - Query and monitor summarization statistics
- `ErrorAnalyticsService` - Error pattern tracking, trend analysis, and sample management
- `CEFRAssessmentService` - Heuristic-based skill-specific CEFR level assessment (grammar, vocabulary, fluency, comprehension)
- `AiChatService` - AI chat integration with streaming responses and **strict JSON schema enforcement**
  - **OpenAI**: Native `JSON_SCHEMA` response format with `strict=true` (gpt-4o, gpt-4o-mini)
  - **Ollama**: Format parameter with JSON schema map and temperature=0
  - **Configurable**: `ai-tutor.chat.strict-schema-enforcement` (default: true)
  - **Fallback**: Soft enforcement (prompt-based) for unknown providers or when disabled
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
- **CEFR Levels**: None-C2 language proficiency tracking
- **Error Detection**: 9 error types with severity classification
- **UI Integration**: Corrections displayed as hover tooltips with severity indicators
- **Session Persistence**: Chat sessions and messages stored in H2 database
- **Authentication**: JWT-based with access/refresh tokens, Spring Security integration
- **Custom Tutors**: User-specific and global tutor management
  - Global tutors: Seed data tutors visible to all users
  - User-specific tutors: Private custom tutors (only visible to creator)
  - Admin privileges: Admins can create global tutors (set `isGlobal: true`)
  - Visibility filtering: Users see global tutors + their own custom tutors
  - Database fields: `created_by_user_id` (nullable), `is_global` (boolean)
- **Progressive Summarization**: Hierarchical message summarization for long conversations
  - Level-1 summaries: Chunks of N messages (configurable, default 10)
  - Level-2+ summaries: Recursive summarization of lower-level summaries
  - Async execution: Summarization runs in background, doesn't block requests
  - Token optimization: Aggressive compaction with preserved context quality
  - Monitoring: REST endpoints for tracking summary statistics and compression ratios
- **Tutor-Initiated Messages**: Tutor can send first message without user input
  - Welcome messages: Tutor greets and introduces course when session starts
  - Re-engagement (two-tier system):
    - Light re-engagement (1-7 days): Brief, casual continuation message
    - Full re-engagement (7+ days): Warm welcome-back with context and motivation
  - Implementation: POST `/sessions/{id}/messages/initiate` with context ("welcome", "reengage-light", or "reengage")
  - System prompt adapts based on initiation context to generate appropriate greeting
  - No dummy user messages stored in database - clean semantic model
- **Learning Cards**: Two card types for different pedagogical purposes
  - **Word Cards** (`wordCards`): Vocabulary words/phrases with visual concepts
    - Fields: titleSourceLanguage, titleTargetLanguage, descriptionSourceLanguage, descriptionTargetLanguage, conceptName (for image lookup)
    - Use for: nouns, verbs, adjectives, phrases representing concrete concepts
    - Example: "りんご" (apple) with image, "красивый" (beautiful), "café" (coffee shop)
  - **Character Cards** (`characterCards`): Individual characters/symbols in special writing systems
    - Fields: character (1-3 chars in target language), pronunciation (romanization), description (learning guidance)
    - Use for: hiragana, katakana, hangul, cyrillic alphabet, kanji, etc.
    - Example: character="あ", pronunciation="a", description="Like 'a' in 'father'. First vowel in hiragana syllabary."
    - CLI Display: Large-font flashcard format (FRONT/BACK)
  - LLM chooses appropriate card type based on learning context

## Commands
- `./gradlew :backend:bootRun` - Run REST API server (requires AI provider configuration: OpenAI, Azure OpenAI, or Ollama)
- `./gradlew :backend:runTestHarness` - Run pedagogical test harness with LLM-as-judge evaluation (supports OpenAI, Azure OpenAI, or Ollama)
  - `--args="--list"` - List all available test scenarios
  - `--args="--scenario NAME"` - Run specific scenario(s)
  - `--args="--help"` - Show test harness help
- `./gradlew build` - Build all modules
- `./gradlew :backend:build` - Build backend module
- `./gradlew :backend:test` - Run backend tests
- H2 Console: http://localhost:8080/h2-console
- HTTP Tests: `backend/src/test/http/http-client-requests.http`

**Note:** Three independent entry points - REST API server, CLI client, and test harness

## Package Structure
```
ch.obermuhlner.aitutor
├── auth/                   # Authentication and authorization
│   ├── controller/         # AuthController (/api/v1/auth)
│   ├── service/            # AuthService, JwtTokenService, AuthorizationService
│   └── dto/                # RegisterRequest, LoginRequest, LoginResponse, UserResponse,
│                           # RefreshTokenRequest, ChangePasswordRequest
├── user/                   # User management
│   ├── controller/         # UserLanguageController (/api/v1/users/{userId}/languages)
│   ├── service/            # UserService, CustomUserDetailsService, UserLanguageService
│   ├── repository/         # UserRepository, RefreshTokenRepository, UserLanguageProficiencyRepository
│   ├── domain/             # UserEntity, RefreshTokenEntity, UserRole, AuthProvider,
│   │                       # UserLanguageProficiencyEntity
│   └── dto/                # UserLanguageProficiencyResponse, AddLanguageRequest, UpdateLanguageRequest
├── chat/                   # Chat REST API layer
│   ├── controller/         # ChatController (/api/v1/chat), SummaryController (/api/v1/summaries)
│   ├── service/            # ChatService, SummaryQueryService
│   ├── repository/         # ChatSessionRepository, ChatMessageRepository, MessageSummaryRepository
│   ├── domain/             # ChatSessionEntity (extended with course fields), ChatMessageEntity, MessageRole,
│   │                       # MessageSummaryEntity, SummarySourceType
│   └── dto/                # CreateSessionRequest, SessionResponse, SendMessageRequest,
│                           # InitiateTutorMessageRequest, MessageResponse, SessionWithMessagesResponse,
│                           # UpdatePhaseRequest, UpdateTopicRequest, UpdateTeachingStyleRequest,
│                           # TopicHistoryResponse, CreateSessionFromCourseRequest,
│                           # SessionWithProgressResponse, SessionProgressResponse,
│                           # SessionSummaryInfoResponse, SummaryLevelInfo, SummaryDetailResponse,
│                           # WordCardResponse, CharacterCardResponse, VocabularyWithImageResponse
├── catalog/                # Catalog-based tutor/course management
│   ├── controller/         # CatalogController (/api/v1/catalog)
│   ├── service/            # CatalogService, SeedDataService
│   ├── repository/         # TutorProfileRepository, CourseTemplateRepository
│   ├── domain/             # TutorProfileEntity, CourseTemplateEntity
│   └── dto/                # LanguageResponse, CourseResponse, CourseDetailResponse,
│                           # TutorResponse, TutorDetailResponse
├── vocabulary/             # Vocabulary tracking
│   ├── controller/         # VocabularyController (/api/v1/vocabulary)
│   ├── service/            # VocabularyService, VocabularyContextService, VocabularyQueryService
│   ├── repository/         # VocabularyItemRepository, VocabularyContextRepository
│   ├── domain/             # VocabularyItemEntity, VocabularyContextEntity
│   └── dto/                # NewVocabularyDTO, VocabularyItemResponse,
│                           # VocabularyContextResponse, VocabularyItemWithContextsResponse
├── analytics/              # Error analytics and tracking
│   ├── controller/         # ErrorAnalyticsController (/api/v1/analytics)
│   ├── service/            # ErrorAnalyticsService
│   ├── repository/         # ErrorPatternRepository, RecentErrorSampleRepository
│   ├── domain/             # ErrorPatternEntity, RecentErrorSampleEntity
│   └── dto/                # ErrorPatternResponse, ErrorTrendResponse, ErrorSampleResponse
├── assessment/             # CEFR skill assessment
│   ├── controller/         # AssessmentController (/api/v1/assessment)
│   ├── service/            # CEFRAssessmentService
│   └── dto/                # SkillBreakdownResponse
├── tutor/                  # Tutoring logic and domain
│   ├── service/            # TutorService, PhaseDecisionService, TopicDecisionService,
│   │                       # ProgressiveSummarizationService, MessageCompactionService,
│   │                       # ConversationSummarizationService
│   └── domain/             # Tutor, ConversationState, ConversationResponse, ConversationPhase
├── conversation/           # AI chat integration
│   ├── service/            # AiChatService (interface), SingleJsonEntityAiChatService,
│   │                       # StreamReplyThenJsonEntityAiChatService
│   └── dto/                # AiChatRequest, AiChatResponse
├── language/               # Language and localization services
│   ├── service/            # LanguageService, LocalizationService, TranslationService
│   │                       # OpenAITranslationService
│   └── config/             # LanguageConfig (language metadata configuration)
├── cli/                    # Standalone CLI client
│   ├── AiTutorCli          # Main CLI application with catalog commands
│   ├── CliConfig           # Configuration management (updated for course-based sessions)
│   └── HttpApiClient       # HTTP client for REST API (with catalog endpoints)
├── testharness/            # Pedagogical test harness (LLM-as-judge)
│   ├── TestHarnessMain     # Main entry point for test harness
│   ├── config/             # TestHarnessConfig - YAML configuration loading
│   ├── client/             # ApiClient - REST API client for test execution
│   ├── domain/             # TestScenario, EvaluationResult, LearnerPersona, etc.
│   ├── judge/              # JudgeService - LLM-based pedagogical evaluation
│   ├── ai/                 # AI provider implementations (OpenAI, Azure OpenAI, Ollama)
│   │   ├── AiProvider      # Provider abstraction interface
│   │   ├── AiProviderFactory # Provider factory
│   │   ├── OpenAiProvider  # OpenAI HTTP client
│   │   ├── AzureOpenAiProvider # Azure OpenAI HTTP client
│   │   └── OllamaProvider  # Ollama HTTP client
│   ├── executor/           # TestExecutor - Scenario orchestration and execution
│   ├── scenario/           # ScenarioLoader - YAML scenario file loading
│   └── report/             # ReportGenerator - Markdown report generation
└── core/                   # Shared models and utilities
    ├── model/              # Shared domain models (CEFRLevel, ErrorType, ErrorSeverity,
    │                       # Correction, NewVocabulary, WordCard, CharacterCard)
    │   └── catalog/        # Catalog domain models (LanguageMetadata, TutorPersonality,
    │                       # CourseCategory, Difficulty, LanguageProficiencyType)
    └── util/               # Utilities (LlmJson, Placeholder)
```

## Course Lesson Writing Guidelines

For comprehensive course lesson writing guidelines, see **CLAUDE_COURSE.md**.

This separate document contains:
- Course types and pedagogical approaches (Conversational, Grammar, Travel)
- Complete lesson structure (8 required sections)
- Size requirements by course type
- Quality standards and pedagogical research foundations
- Curriculum validation and curriculum.yml structure
- Reference examples and comprehensive checklists
- Quick access via `/course:write-lesson` slash command

## Development Guidelines

### When Adding New REST Endpoints
When adding new REST endpoints or modifying existing ones, **always update**:
1. **README.md** - Update API endpoint table and examples
2. **backend/src/test/http/http-client-requests.http** - Add HTTP client test examples
3. **CLAUDE.md** - Update REST API Layer section and package structure
4. **http-client.env.json** - Add any new variables if needed

### When Making Code Changes
**Before committing any changes:**
1. **Run all tests**: `./gradlew :backend:test`
2. **Fix all failing tests** - Never commit with failing tests
3. **Add tests for new functionality** - Maintain test coverage
4. **Verify build succeeds**: `./gradlew build`

If tests fail, investigate and fix the root cause. Common issues:
- Mocked dependencies need updating
- Test data doesn't match new validation rules
- Missing or incorrect test configuration

### Content Parsing Guidelines

**DO NOT use regex to parse natural language content (German, Spanish, French, Japanese text, etc.)**

Regex patterns are too brittle for parsing actual language text:
- Different languages have different sentence structures and patterns
- Cannot handle linguistic variations and edge cases
- Not maintainable across multiple natural languages
- Breaks with minor variations in phrasing or word choice

**Parsing markdown structure with regex is FINE:**
- Extracting headers, code blocks, bullet points, bold text is acceptable
- Make regex patterns flexible to handle whitespace variations
- Use `\s*` liberally to tolerate extra blank lines and spacing

**For natural language extraction, use:**
- LLM-based extraction when semantic understanding is needed
- String operations (split, substring, indexOf) for simple text extraction
- Structured data formats (YAML frontmatter, JSON) when possible

**Use Standard Quotes and Apostrophes:**
- Use only standard ASCII quotes (`'` and `"`) in prompts and lesson text
- Avoid "smart" or "curly" quotes (`‘’` and `""`) and fancy apostrophes (`'`)
- These "strange" characters can cause parsing issues and encoding problems
- Stick to plain ASCII characters for maximum compatibility across systems

### Git Commit Guidelines

See the root `CLAUDE.md` for project-wide git commit guidelines.

### Preserving Existing Functionality

See the root `CLAUDE.md` for project-wide guidelines on preserving existing functionality.