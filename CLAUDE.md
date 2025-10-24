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
- **SeedDataService** - Database seed service with curriculum validation to ensure all configured courses have corresponding curriculum files at startup
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

### Curriculum Validation
Starting with the enhanced system, the application validates that all configured courses in `catalogProperties.courses` have corresponding curriculum files at startup. If a course is configured but its curriculum file is missing, the application will fail to start with an error message. This ensures that all courses registered in the system have valid curriculum content.

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

### Course Types and Pedagogical Approaches

Lessons are tailored to three distinct course types based on learning objectives and proven pedagogy:

#### Conversational Courses
**Pedagogical foundation**: Communicative Language Teaching (CLT - Canale & Swain 1980), Output Hypothesis (Swain 1985)

**Content emphasis**: 60% vocabulary & scenarios, 40% grammar
- Balances form (grammar accuracy) and function (meaningful communication)
- Develops communicative competence through interaction
- Supports learner fluency with grammatical accuracy
- **Target CEFR**: A1-B2
- **Examples**: "Conversational German", "Conversational Spanish", "American English"

**Lesson characteristics**:
- Grammar sections: 40-60 lines with 2-3 core grammar points
- Vocabulary: 30-60 items organized by semantic categories
- Scenario count: 2-3 AI-friendly descriptions (not full dialogues)
- Practice focus: Production-oriented (meaningful communication)
- Size: 150-180 lines

**Rationale**: Research shows learners develop proficiency fastest when combining explicit grammar instruction with meaningful communicative practice (Long 1991).

---

#### Grammar Courses
**Pedagogical foundation**: Focus on Form (Long 1991, Doughty & Williams 1998), Skill Acquisition Theory (DeKeyser 2007)

**Content emphasis**: 70% grammar, 30% supporting vocabulary
- Prioritizes explicit, systematic grammar instruction
- Develops metalinguistic awareness and grammatical accuracy
- Supports test preparation and formal learning contexts
- **Target CEFR**: A1-C2 (depends on curriculum sequence)
- **Examples**: "German Grammar Fundamentals", "Spanish Grammar Essentials", "Advanced French Grammar"

**Lesson characteristics**:
- Grammar sections: 80-120 lines with 4-6 grammar points
  - Include paradigm tables (conjugation tables, case declensions, gender charts)
  - Multiple example sentences per rule (minimum 5-7 examples)
  - Negative forms, questions, and irregular forms
  - Contrastive analysis ("use X here, use Y there")
- Vocabulary: 15-30 items (only high-frequency grammar-supporting words)
  - Include grammatical terminology in target language
  - Function words and particles
- Scenario count: 1-2 brief AI-friendly descriptions (grammar in context)
- Practice focus: Form-focused drills and controlled production
  - "Conjugate all pronouns in present tense"
  - "Transform sentences from present to past"
  - "Correct errors in these sentences"
- Size: 150-200 lines

**Rationale**: Explicit form-focused instruction helps learners notice grammatical patterns (Schmidt's Noticing Hypothesis 1990) and proceduralize knowledge (DeKeyser 2007). Most effective for learners with explicit learning preferences or test preparation goals.

---

#### Travel Courses
**Pedagogical foundation**: Task-Based Language Teaching (Ellis 2003), Notional-Functional Syllabus (Wilkins 1976)

**Content emphasis**: 75% vocabulary & scenarios, 25% minimal grammar
- Prioritizes pragmatic competence and task completion
- Focuses on survival communication in authentic travel scenarios
- Emphasizes fluency and functional effectiveness over grammatical perfection
- **Target CEFR**: A1-A2 (survivalist level)
- **Examples**: "German for Travelers", "Spanish for Travelers", "Mandarin for Travelers"

**Lesson characteristics**:
- Grammar sections: 20-40 lines with 1-2 essential grammar points ONLY
  - Only include grammar necessary for immediate survival communication
  - Simple, practical explanations (no extensive paradigms)
  - Examples: question formation, basic word order, polite verb forms
- Vocabulary: 50-80 items (high-frequency phrases and chunks)
  - Organized by communicative function/task
  - Formulaic chunks: "Where is the...", "I would like...", "How much does it cost?", "Can you help me?"
  - Pronunciation guidance for critical phrases
- Scenario count: 3-4 task-based descriptions (real-world situations)
  - Focus on practical tasks: ordering food, asking directions, emergencies
  - Emphasize "successful communication" over accuracy
- Cultural Notes: EXPANDED (6-10 items covering pragmatic competence)
  - Local customs, politeness norms, non-verbal communication
  - What to do/avoid to show respect
- Practice focus: Task completion and functional communication
  - "Successfully order a meal at a restaurant"
  - "Navigate from hotel to train station"
  - "Handle emergency situations (lost passport, medical issue, police)"
- Size: 120-160 lines

**Rationale**: Travel learners have immediate, high-stakes communicative needs. Task-based approaches prioritize pragmatic success (communicating intent and understanding context) over grammatical accuracy (Ellis 2003). Formulaic chunks accelerate practical competence for urgent real-world situations.

---

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
- **CRITICAL**: All grammar explanations must be linguistically accurate
- Use clear rules with examples for each grammar point
- Use bold for rules: `**Rule:** Subject + verb + object`

**Conversational Courses:**
- Include 2-3 core grammar points per lesson
- Provide 3-5 examples per rule
- Include negative and question forms when relevant
- Show conjugation tables for high-frequency verbs
- Focus on communicative functionality

**Grammar Courses:**
- Include 4-6 grammar points per lesson with systematic depth
- Provide 5-7+ examples per rule (minimum)
- **Mandatory**: Include complete paradigm tables (conjugations, declensions, gender charts)
- **Mandatory**: Include negative forms, question forms, and irregular variations
- Add contrastive analysis: "Use X in situation Y, but Z in situation W"
- Include examples of common learner errors (bridge to Common Mistakes section)

**Travel Courses:**
- Include ONLY 1-2 essential grammar points (survival-critical)
- Focus on immediate communicative utility
- Provide 2-3 practical examples per rule
- Minimize paradigms and metalinguistic explanations
- Example: "How to ask questions" (not "subjunctive mood")

Example structure (Conversational):
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

Example structure (Grammar Course - expanded):
```markdown
### Present Tense Regular Verbs

**Rule:** German weak verbs add endings to the stem. The conjugation depends on person and number.

Complete Conjugation Table:
| Person | Singular | Plural |
|--------|----------|--------|
| 1st | ich mache | wir machen |
| 2nd (formal) | Sie machen | Sie machen |
...

**Negative form:** Ich mache das nicht.
**Question form:** Machst du das?

**Irregular variations:**
- haben (to have): ich habe, du hast, er hat
- sein (to be): ich bin, du bist, er ist

Examples of errors:
- "Ich make das" → "Ich mache das" (wrong stem form)
```

Example structure (Travel Course - minimal):
```markdown
### Asking Questions

**Rule:** To ask questions, use Wo (where), Wann (when), Wie viel (how much)

Examples:
- "Wo ist der Bahnhof?" (Where is the train station?)
- "Wie viel kostet das?" (How much does it cost?)
- "Wann kommt der Bus?" (When does the bus come?)
```

#### 4. Vocabulary
- Use bold for target language: `**Hola** - Hello`
- Organize by semantic categories (e.g., "Greetings", "Time Expressions", "Food Items")
- **CRITICAL**: All translations must be accurate
- Include common phrases and collocations

**Conversational Courses:**
- 30-60 vocabulary items total
- Balance nouns, verbs, adjectives, and phrases
- Include practical, high-frequency words for communication

**Grammar Courses:**
- 15-30 vocabulary items total (MINIMAL)
- Focus only on words needed to demonstrate grammar points
- Include grammatical function words (particles, prepositions, conjunctions)
- Include grammatical terminology in target language

**Travel Courses:**
- 50-80 vocabulary items total (MAXIMUM)
- Organize by communicative function/task (Ordering, Directions, Emergencies, etc.)
- Prioritize formulaic chunks: "Where is...", "I would like...", "How much...", "Can you..."
- Include pronunciation notes for critical phrases
- Focus on high-frequency survival vocabulary

Example (Conversational):
```markdown
### Greetings
- **Hola** - Hello
- **Buenos días** - Good morning
- **Buenas tardes** - Good afternoon
```

Example (Grammar Course - minimal):
```markdown
### Supporting Vocabulary
- **der/die/das** - the (definite articles)
- **ein/eine** - a/an (indefinite articles)
- **und** - and
- **aber** - but
- **oder** - or
```

Example (Travel Course - task-organized):
```markdown
### Ordering Food
- **Ich möchte...** - I would like...
- **Das kostet...** - That costs...
- **Die Rechnung, bitte** - The bill, please
- **Vegetarisch** - Vegetarian
- **Allergisch gegen...** - Allergic to...

### Asking Directions
- **Wo ist...?** - Where is...?
- **Nächste Haltestelle** - Next stop
- **Geradeaus** - Straight ahead
- **Links/Rechts** - Left/Right
```

#### 5. Conversation Scenarios
- **CRITICAL**: Do NOT write full scripted dialogues
- **AI-FIRST DESIGN**: The AI tutor will conduct actual conversations; provide guidance, not scripts
- Provide brief scenario descriptions (2-5 sentences depending on course type)
- Describe situations the AI will help learners navigate
- Include sample phrases and key vocabulary to practice

**Conversational Courses:**
- 2-3 scenarios per lesson
- Focus on communicative goals and meaningful interaction
- Provide 3-5 key phrases per scenario
- Examples: "Greeting a business contact", "Ordering at a restaurant", "Making small talk"

**Grammar Courses:**
- 1-2 scenarios per lesson (minimal)
- Focus on opportunities to use the grammar points being studied
- Provide 2-4 key phrases demonstrating the target grammar
- Examples: "Describe your family using possessive forms", "Narrate a past event using past tense"

**Travel Courses:**
- 3-4 scenarios per lesson (MAXIMUM)
- Focus on real-world survival tasks
- Provide 4-6 essential phrases per scenario
- Prioritize emergency/high-stakes situations
- Examples: "Ordering a meal", "Asking directions", "Medical emergency", "Lost baggage"

**CORRECT Approach (Conversational):**
```markdown
### Ordering at a Café

Practice ordering food and drinks in a café setting. The tutor will play the role of a café staff member.

Topics to cover:
- "Ich möchte einen Kaffee, bitte" (I'd like a coffee, please)
- "Was kostet das?" (How much does that cost?)
- "Die Rechnung, bitte" (The bill, please)

Tutor will prompt: "What beverage would you like?"
```

**CORRECT Approach (Grammar Course):**
```markdown
### Using Possessive Forms

Practice describing your family and personal belongings using German possessive adjectives (mein, dein, sein, ihr, etc.).

Key grammar to practice:
- "Das ist mein Bruder" (That's my brother)
- "Wer ist deine Schwester?" (Who is your sister?)
- "Das ist sein Haus" (That's his house)
```

**CORRECT Approach (Travel Course):**
```markdown
### Ordering at a Restaurant

Order a meal, ask about ingredients/allergies, and request the bill. Practice real-world ordering scenarios.

Key phrases:
- "Ich möchte..." (I would like...)
- "Vegetarisch" / "Allergisch gegen..." (Vegetarian / Allergic to...)
- "Die Rechnung, bitte" (The bill, please)
- "Kreditkarte" / "Bargeld" (Credit card / Cash)
```

**WRONG Approach (too scripted - avoid this for ALL course types):**
```markdown
### Ordering at a Café

Waiter: Guten Tag! Was möchten Sie?
Student: Ich möchte einen Kaffee.
Waiter: Gerne. Mit Milch?
Student: Ja, mit Milch, bitte.
...
```

#### 6. Practice Patterns
- Include specific, actionable practice activities
- Focus on production (output), not passive comprehension

**Conversational Courses:**
- 4-6 activities total
- Mix comprehension checks and production practice
- Examples: "Ask your tutor about their weekend", "Describe your daily routine", "Ask for clarification politely"

**Grammar Courses:**
- 4-8 activities total (MAXIMUM)
- Focus on controlled form production and drills
- Examples: "Conjugate all pronouns in present tense", "Transform sentences from present to past", "Identify which case to use in these sentences", "Correct common errors"

**Travel Courses:**
- 3-5 activities total
- Focus on task completion and functional communication
- Examples: "Successfully order a meal avoiding dietary restrictions", "Navigate from hotel to train station", "Handle a medical emergency", "Negotiate a price at a market"

#### 7. Common Mistakes to Watch
- Show incorrect → correct with explanation
- Format: `Wrong form → Correct form (explanation)`

**Conversational Courses:**
- 5-10 common errors per lesson
- Focus on mistakes that impede communication or sound unnatural
- Include pronunciation errors that change meaning
- Example: `"Yo soy hambre" → "Yo tengo hambre" (use tener, not ser for hunger!)`

**Grammar Courses:**
- 5-12 common errors per lesson (MAXIMUM)
- Focus on grammatical accuracy issues
- Include paradigm errors (wrong conjugation, declension, gender agreement)
- Include errors that demonstrate the grammar point being taught
- Format systematically to bridge grammar concepts
- Example: `"Der Frau" → "Die Frau" (neuter article error, die is feminine!)`

**Travel Courses:**
- 3-8 common errors per lesson
- Focus on comprehension breakdowns, not minor accuracy issues
- Include errors that would prevent the traveler from achieving their goal
- Prioritize misunderstandings and safety-critical mistakes
- Example: `"Ich bin allergisch Milch" → "Ich bin allergisch gegen Milch" (wrong preposition, could cause allergic reaction!)`

#### 8. Cultural Notes
- Include social norms, regional variations, usage tips
- Connect language to real-world situations

**Conversational Courses:**
- 3-6 bullet points about cultural context
- Focus on communication norms and social conventions
- Include regional variations in usage
- Example: "In Spain, lunch is the main meal and typically eaten 2-3pm"

**Grammar Courses:**
- 3-5 bullet points about cultural/linguistic context
- Focus on how grammar reflects cultural values and communication patterns
- Example: "German capitalization of nouns reflects precision and formality in language"

**Travel Courses:**
- 6-10 bullet points about pragmatic/survival competence (EXPANDED)
- Focus on cultural norms for survival and respect
- Include non-verbal communication (gestures, personal space, eye contact)
- Include what to do/avoid to show respect in your destination
- Include tipping customs, dress codes, holiday closures
- Example: "In Germany, service charge is usually included; small tips (1-2%) are optional but appreciated for good service"

### Size Requirements by Course Type
Size requirements vary based on course type to maintain appropriate content density:

**Conversational Courses:**
- **Target**: 150-180 lines
- **Minimum**: 140 lines
- **Maximum**: 200 lines
- Rationale: Balanced vocabulary and grammar requires moderate density

**Grammar Courses:**
- **Target**: 150-200 lines
- **Minimum**: 145 lines
- **Maximum**: 220 lines
- Rationale: Extensive grammar explanations and paradigms require more space; vocabulary is minimal

**Travel Courses:**
- **Target**: 120-160 lines
- **Minimum**: 110 lines
- **Maximum**: 180 lines
- Rationale: Prioritize vocabulary density and task scenarios; minimal grammar

**Line counting**: Include YAML frontmatter, headers, code blocks, tables, bullet points, and blank lines.

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

### Pedagogical Research Foundations

This lesson framework is grounded in modern applied linguistics research and proven instructional design principles:

**Conversational Courses** are based on:
- **Communicative Language Teaching (CLT)** - Canale & Swain (1980): Developing communicative competence through meaningful interaction
- **Output Hypothesis** - Swain (1985, 2005): Learners develop proficiency through producing language, not just consuming it
- **Interaction Hypothesis** - Long (1996): Negotiation of meaning through interaction facilitates language acquisition

**Grammar Courses** are based on:
- **Focus on Form (FonF)** - Long (1991), Doughty & Williams (1998): Targeted, deliberate attention to form within communicative contexts
- **Noticing Hypothesis** - Schmidt (1990): Learners acquire grammatical forms they consciously notice
- **Skill Acquisition Theory** - DeKeyser (2007): Explicit grammar instruction supports proceduralization of knowledge, especially for adult learners
- **Paradigm-based instruction** - Ellis (2006): Learning systematic patterns (paradigm tables) accelerates acquisition

**Travel Courses** are based on:
- **Task-Based Language Teaching (TBLT)** - Ellis (2003), Long (2015): Learners acquire language through completing meaningful tasks in the target language
- **Notional-Functional Syllabus** - Wilkins (1976): Organizing language around communicative functions and real-world situations (not grammatical structures)
- **Lexical Approach** - Lewis (1993): Formulaic chunks and collocations are primary units of language acquisition
- **Pragmatic Competence** - Hymes (1972): Knowing what to say, when to say it, and to whom—not just grammatical correctness

**General principles across all types:**
- **Comprehensible Input (i+1)** - Krashen (1985): Input should be slightly beyond current competence level
- **Spaced Repetition** - Cepeda et al. (2006): Spacing practice over time improves retention
- **Retrieval Practice** - Roediger & Butler (2011): Producing language from memory strengthens learning
- **Authentic Context** - Nunan (1989): Language should be practiced in realistic, meaningful contexts

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

**Basic Requirements (All Course Types):**
- [ ] YAML frontmatter is complete and valid
- [ ] lessonId matches the pattern `week-NN-topic`
- [ ] Course type (conversational/grammar/travel) is identified
- [ ] All 8 required sections are present and in order
- [ ] Grammar explanations are linguistically accurate with authoritative sources checked
- [ ] All vocabulary translations are accurate
- [ ] Conversation scenarios are brief descriptions, NOT full scripted dialogues
- [ ] No AI-first design violations (no dialogue scripts)
- [ ] Common mistakes show "wrong → correct (explanation)" format
- [ ] Lesson is added to curriculum.yml with correct id and file path

**Conversational Courses:**
- [ ] Lesson is 150-180 lines
- [ ] Grammar: 2-3 core points, 40-60 lines total
- [ ] Vocabulary: 30-60 items organized by semantic categories
- [ ] Scenarios: 2-3 descriptions with 3-5 key phrases each
- [ ] Practice patterns: 4-6 production-focused activities
- [ ] Common mistakes: 5-10 focused on communication clarity
- [ ] Cultural notes: 3-6 points on communication norms

**Grammar Courses:**
- [ ] Lesson is 150-200 lines
- [ ] Grammar: 4-6 points with 80-120 lines total
- [ ] Include complete paradigm tables (conjugations/declensions/cases)
- [ ] Minimum 5-7 examples per grammar rule
- [ ] Include negative, question, and irregular forms
- [ ] Vocabulary: 15-30 items (grammar-supporting words only)
- [ ] Scenarios: 1-2 descriptions focused on grammar application
- [ ] Practice patterns: 4-8 form-focused drills
- [ ] Common mistakes: 5-12 focused on grammatical accuracy
- [ ] Cultural notes: 3-5 points on grammar/cultural connections

**Travel Courses:**
- [ ] Lesson is 120-160 lines
- [ ] Grammar: 1-2 essential points only, 20-40 lines total
- [ ] Focus on survival communication, not meta-linguistic explanations
- [ ] Vocabulary: 50-80 items organized by communicative function/task
- [ ] Include formulaic chunks and pronunciation guidance for critical phrases
- [ ] Scenarios: 3-4 real-world task descriptions
- [ ] Practice patterns: 3-5 task completion activities
- [ ] Common mistakes: 3-8 focused on comprehension breakdowns/safety
- [ ] Cultural notes: 6-10 points on pragmatic competence and respect

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

**Use Standard Quotes and Apostrophes:**
- Use only standard ASCII quotes (`'` and `"`) in prompts and lesson text
- Avoid "smart" or "curly" quotes (`‘’` and `""`) and fancy apostrophes (`'`)
- These "strange" characters can cause parsing issues and encoding problems
- Stick to plain ASCII characters for maximum compatibility across systems

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