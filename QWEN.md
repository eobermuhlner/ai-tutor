# AI Tutor

Language learning assistant with conversational AI tutoring and vocabulary tracking.

## Tech Stack
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (JPA)
- **AI**: Spring AI 1.0.1 (multi-provider support: OpenAI, Azure OpenAI, Ollama)
- **Build**: Gradle, Java 17
- **Logging**: SLF4J (via Spring Boot starter)

## Core Components
- `TutorService` - Main tutoring logic with adaptive conversation phases
- `PhaseDecisionService` - Automatic phase selection based on learner error patterns
- `TopicDecisionService` - Conversation topic tracking with hysteresis
- `ProgressiveSummarizationService` - Hierarchical message summarization
- `MessageCompactionService` - Context compaction using progressive summaries
- `ErrorAnalyticsService` - Error pattern tracking and trend analysis
- `CEFRAssessmentService` - CEFR skill assessment (grammar, vocabulary, fluency, comprehension)
- `AiChatService` - AI chat integration with streaming
- `VocabularyService` - Vocabulary tracking with context

## Key Features
- **Conversation Phases**: Free, Correction, Drill, Auto (severity-weighted)
- **Error Severity System**: Critical (3.0), High (2.0), Medium (1.0), Low (0.3)
- **Topic Management**: With hysteresis to prevent topic thrashing
- **CEFR Levels**: A1-C2 language proficiency tracking
- **Progressive Summarization**: Hierarchical message summarization for long conversations
- **JWT Authentication**: With access/refresh tokens

## REST API Endpoints
- `/api/v1/auth/*` - Authentication
- `/api/v1/chat/*` - Chat sessions and messages
- `/api/v1/catalog/*` - Languages, courses, tutors
- `/api/v1/users/{userId}/languages/*` - User language management
- `/api/v1/vocabulary/*` - Vocabulary tracking
- `/api/v1/summaries/*` - Summarization monitoring
- `/api/v1/analytics/*` - Error analytics
- `/api/v1/assessment/*` - CEFR skill assessment

## Commands
- `./gradlew runServer` - Run REST API server
- `./gradlew runCli` - Run CLI client
- `./gradlew runTestHarness` - Run pedagogical test harness
- `./gradlew build` - Build project
- `./gradlew test` - Run tests

## Development Guidelines
- Update README.md, HTTP tests, and CLAUDE.md when adding endpoints
- Never commit with failing tests
- Don't use regex for natural language parsing
- Follow Git commit guidelines (no AI attribution)
- Preserve existing functionality