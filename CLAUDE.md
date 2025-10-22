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
  - DELETE `/sessions/{id}` - Delete session
- **CatalogController** - Catalog browsing REST endpoints (`/api/v1/catalog/*`)
  - GET `/languages?sourceLanguage={lang}` - List available languages
  - GET `/languages/{code}/courses?sourceLanguage={lang}&userLevel={level}` - List courses for language
  - GET `/languages/{code}/tutors?sourceLanguage={lang}` - List tutors for language
  - GET `/courses/{id}?sourceLanguage={lang}` - Get course details
  - GET `/tutors/{id}?sourceLanguage={lang}` - Get tutor details
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
- **ChatService** - Session/message orchestration, integrates TutorService
- **CatalogService** - Browse languages, courses, and tutors with localization
- **UserLanguageService** - Manage user's language proficiency profiles
- **LocalizationService** - Handle multilingual content with AI translation fallback
- **TranslationService** - AI-powered translation (OpenAI)
- **ChatSessionRepository** / **ChatMessageRepository** - JPA persistence
- **TutorProfileRepository** / **CourseTemplateRepository** - Catalog persistence
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
- **Progressive Summarization**: Hierarchical message summarization for long conversations
  - Level-1 summaries: Chunks of N messages (configurable, default 10)
  - Level-2+ summaries: Recursive summarization of lower-level summaries
  - Async execution: Summarization runs in background, doesn't block requests
  - Token optimization: Aggressive compaction with preserved context quality
  - Monitoring: REST endpoints for tracking summary statistics and compression ratios

## Commands
- `./gradlew runServer` - Run REST API server (requires AI provider configuration: OpenAI, Azure OpenAI, or Ollama)
- `./gradlew runCli` - Run CLI client (connects to running server)
- `./gradlew runTestHarness` - Run pedagogical test harness with LLM-as-judge evaluation (supports OpenAI, Azure OpenAI, or Ollama)
  - `--args="--list"` - List all available test scenarios
  - `--args="--scenario NAME"` - Run specific scenario(s)
  - `--args="--help"` - Show test harness help
- `./gradlew bootRun` - Run REST API server (alternative to runServer)
- `./gradlew build` - Build project
- H2 Console: http://localhost:8080/h2-console
- HTTP Tests: `src/test/http/http-client-requests.http`

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
│                           # MessageResponse, SessionWithMessagesResponse, UpdatePhaseRequest,
│                           # UpdateTopicRequest, UpdateTeachingStyleRequest, TopicHistoryResponse,
│                           # CreateSessionFromCourseRequest, SessionWithProgressResponse, SessionProgressResponse,
│                           # SessionSummaryInfoResponse, SummaryLevelInfo, SummaryDetailResponse
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
    │                       # Correction, NewVocabulary, WordCard)
    │   └── catalog/        # Catalog domain models (LanguageMetadata, TutorPersonality,
    │                       # CourseCategory, Difficulty, LanguageProficiencyType)
    └── util/               # Utilities (LlmJson, Placeholder)
```

## Course Lesson Writing Guidelines

### Overview
Course lessons are markdown files with YAML frontmatter located in `src/main/resources/course-content/{course-id}/`. Each lesson follows a standardized structure to ensure pedagogical consistency and quality across all language courses.

### Directory Structure
```
src/main/resources/course-content/
├── de-conversational-german/
│   ├── curriculum.yml              # Course structure and lesson sequence
│   ├── week-01-greetings.md        # Individual lesson files
│   ├── week-02-introductions.md
│   └── ...
├── es-conversational-spanish/
│   ├── curriculum.yml
│   └── ...
├── fr-conversational-french/
│   ├── curriculum.yml
│   └── ...
└── ja-conversational-japanese/
    ├── curriculum.yml
    └── ...
```

### File Naming Convention
- Pattern: `week-{NN}-{topic-slug}.md`
- Examples: `week-01-greetings.md`, `week-04-food-ordering.md`, `week-09-past-tense.md`
- Use lowercase, hyphen-separated topic names
- Week numbers are zero-padded (01, 02, ..., 10)

### Lesson Structure
Every lesson must include these sections in order:

#### 1. YAML Frontmatter (Required)
```yaml
---
lessonId: week-01-greetings
title: Grüße und erste Worte            # In target language
weekNumber: 1
estimatedDuration: 1 week
focusAreas:
  - Basic greetings                     # 2-4 focus areas
  - Polite expressions
  - Common questions
targetCEFR: A1                          # A1, A2, B1, B2, C1, or C2
---
```

#### 2. This Week's Goals
- 3-5 bullet points describing learning objectives
- Use active, learner-focused language
- Example: "Greet people in formal and informal contexts"

#### 3. Grammar Focus
- **CRITICAL**: Grammar explanations must be linguistically accurate
- Include clear rules with examples for each grammar point
- Use bold for important rules: `**Rule:** Subject + verb + object`
- Show conjugation tables when relevant
- Provide 3-5 examples per grammar point
- Include negative forms and question forms

Example structure:
```markdown
### Present Tense Regular Verbs

**Rule:** Regular verbs follow this pattern: stem + ending

Conjugation of hablar (to speak):
- Yo hablo (I speak)
- Tú hablas (You speak)
...

Examples:
- Yo hablo español (I speak Spanish)
- ¿Hablas inglés? (Do you speak English?)
```

#### 4. Vocabulary
- Organize by semantic categories (e.g., "Greetings", "Time Expressions", "Food Items")
- Use bold for target language: `**Hola** - Hello`
- 30-60 vocabulary items total
- Include common phrases and collocations
- **CRITICAL**: Translations must be accurate

Example:
```markdown
### Greetings
- **Hola** - Hello
- **Buenos días** - Good morning
- **Buenas tardes** - Good afternoon
```

#### 5. Conversation Scenarios
- **AI-FIRST DESIGN**: Do NOT write full dialogues
- Provide brief scenario descriptions (2-4 sentences)
- Describe topics the AI tutor will guide learners through
- Include sample phrases and key vocabulary to use
- Focus on communicative goals, not scripted exchanges

**CORRECT Approach:**
```markdown
### Ordering at a Café

Practice ordering food and drinks in a café setting.

Topics to cover:
- "Ich möchte einen Kaffee, bitte" (I'd like a coffee, please)
- "Was kostet das?" (How much does that cost?)
- "Die Rechnung, bitte" (The bill, please)
```

**WRONG Approach (too scripted):**
```markdown
### Ordering at a Café

Waiter: Guten Tag! Was möchten Sie?
Student: Ich möchte einen Kaffee.
Waiter: Gerne. Mit Milch?
Student: Ja, mit Milch, bitte.
...
```

#### 6. Practice Patterns
- 4-8 bullet points with specific practice activities
- Focus on production, not just comprehension
- Examples: "Form questions using all pronouns", "Describe your daily routine using present tense"

#### 7. Common Mistakes to Watch
- List 5-10 common errors learners make
- Show incorrect → correct with explanation
- Format: `Wrong form → Correct form (explanation)`
- Example: `"Yo soy hambre" → "Yo tengo hambre" (use tener, not ser!)`

#### 8. Cultural Notes
- 3-6 bullet points about cultural context
- Include social norms, regional variations, usage tips
- Connect language to real-world situations
- Example: "In Spain, lunch is the main meal and typically eaten 2-3pm"

### Size Requirements
- **Target**: 100-200 lines of markdown per lesson
- **Minimum**: 100 lines (pedagogically complete)
- **Recommended**: 150-180 lines (comprehensive without being overwhelming)
- **Maximum**: 230 lines (avoid information overload)

Lines are counted including YAML frontmatter, headers, and blank lines.

### Quality Standards

#### Grammar and Vocabulary Accuracy
- **CRITICAL**: All grammar explanations must be linguistically correct
- Verify verb conjugations, noun genders, case systems, etc.
- Use authoritative references for grammar rules
- Double-check vocabulary translations
- Be consistent with terminology throughout the course

#### Pedagogical Completeness
- Each lesson should be self-contained for one week of study
- Progressive difficulty: build on previous lessons
- Balance comprehension (input) and production (output)
- Include recognition and production vocabulary
- Cover all major grammar points for the CEFR level

#### AI-First Design
- Remember: The AI tutor will conduct actual conversations
- Scenario descriptions guide the AI, not the learner directly
- Focus on communicative goals and key phrases
- Let the AI adapt to learner level and interests
- Avoid rigid dialogue scripts

### Connecting Lessons to Courses

Lessons are connected to courses via **explicit declaration** in `curriculum.yml`:

```yaml
courseId: de-conversational-german
progressionMode: TIME_BASED           # or SEQUENTIAL
lessons:
  - id: week-01-greetings            # Must match lessonId in frontmatter
    file: week-01-greetings.md       # Filename in same directory
    unlockAfterDays: 0                # Day 0 = immediately available
    requiredTurns: 5                  # Minimum conversation turns to complete
  - id: week-02-introductions
    file: week-02-introductions.md
    unlockAfterDays: 7                # Unlocks after 7 days
    requiredTurns: 8
  # ... additional lessons
```

**Important:**
- `lessonId` in YAML frontmatter must match `id` in curriculum.yml
- `file` path is relative to the course directory
- `unlockAfterDays` controls time-based progression
- `requiredTurns` sets minimum engagement before lesson completion

### Reference Examples

**High-quality reference lessons:**
- Spanish: `src/main/resources/course-content/es-conversational-spanish/week-01-greetings.md` (117 lines)
- German: `src/main/resources/course-content/de-conversational-german/week-01-greetings.md` (165 lines)
- French: `src/main/resources/course-content/fr-conversational-french/week-02-introductions.md` (157 lines)

Study these files for examples of:
- Clear grammar explanations with multiple examples
- Well-organized vocabulary sections
- AI-friendly conversation scenario descriptions
- Balanced lesson length and content density

### Checklist Before Adding a New Lesson

- [ ] YAML frontmatter is complete and valid
- [ ] lessonId matches the pattern `week-NN-topic`
- [ ] All 8 required sections are present
- [ ] Grammar explanations are linguistically accurate
- [ ] Vocabulary translations are correct
- [ ] Conversation scenarios are summaries, not full dialogues
- [ ] Common mistakes section shows wrong → correct forms
- [ ] Lesson is 100-200 lines
- [ ] Cultural notes provide real-world context
- [ ] Lesson is added to curriculum.yml with correct id and file path

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

### Git Commit Guidelines

⚠️ **CRITICAL: FOLLOW THESE RULES EXACTLY - NO EXCEPTIONS** ⚠️

**Commit message format (MANDATORY):**
- **First line**: Concise summary (imperative mood, no period)
- **Body** (optional): Brief explanation of what and why (one sentence per line)
- **❌ NEVER include attribution**: ABSOLUTELY NO "Generated with Claude Code", "Co-Authored-By: Claude", or ANY similar AI attribution

**✅ CORRECT Example:**
```
Add effectivePhase to separate user preference from active phase

User-controlled conversationPhase (Auto/Free/Correction/Drill) now separate from effectivePhase (actual active phase).
LLM suggestions only update effectivePhase when in Auto mode, never override manual user choices.
```

**❌ WRONG Example (DO NOT DO THIS):**
```
Add effectivePhase to separate user preference from active phase

User-controlled conversationPhase now separate from effectivePhase.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

**Before every commit, verify:**
1. ✅ No "Generated with" or "Co-Authored-By" lines
2. ✅ Imperative mood ("Add" not "Added", "Fix" not "Fixed")
3. ✅ No period at end of first line
4. ✅ Blank line between subject and body (if body exists)

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