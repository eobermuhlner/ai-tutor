# AI Tutor v0.0.1 - Initial Release

**Release Date:** November 20, 2025

We're excited to announce the first production release of AI Tutor, an intelligent language learning platform powered by AI that provides personalized conversational tutoring with real-time error correction and vocabulary tracking.

## Overview

AI Tutor is a comprehensive language learning platform designed to provide adaptive, personalized instruction through AI-powered conversations. This release represents months of development focused on pedagogical effectiveness, scalability, and user experience.

## Platform Architecture

- **Backend:** Kotlin + Spring Boot 3.5.6 with Spring AI 1.0.1
- **Frontend:** React 19.1.1 + TypeScript + Vite + Tailwind CSS
- **Database:** H2 (JPA) with automatic schema management
- **Deployment:** Docker support with GitHub Actions CI/CD
- **API:** RESTful API with JWT authentication and OpenAPI documentation

## Major Features

### Core Learning Experience

#### Adaptive Conversation Phases
- **Free Phase:** Pure fluency focus with no error tracking, building confidence
- **Correction Phase:** Balanced learning with passive error display (default mode)
- **Drill Phase:** Active accuracy work with explicit error correction
- **Auto Mode:** Intelligent phase selection based on severity-weighted error patterns

#### Advanced Error Detection & Analytics
- 9 error types with context-aware severity classification (Critical, High, Medium, Low)
- Chat-context intelligence recognizing casual communication norms
- Error pattern tracking and trend analysis (improving/stable/worsening)
- Fossilization detection with automatic drill phase triggers
- Weighted severity scoring for phase transition decisions

#### CEFR-Based Assessment
- Skill-specific CEFR level tracking (A1-C2)
- Heuristic assessment across 4 dimensions: grammar, vocabulary, fluency, comprehension
- Manual reassessment endpoints for testing and calibration

#### Intelligent Conversation Management
- Topic tracking with hysteresis to prevent thrashing (min 3 turns, max 12 turns)
- Topic history to avoid repetition
- AI-proposed topics validated by stability logic
- Multiple teaching styles: Reactive, Guided, Directive

### Content Management

#### Course System
- Course-based learning with structured curriculum
- Lesson sequencing with progress tracking
- AI-powered lesson switching based on learner needs
- Travel, conversational, grammar, and specialized courses
- Course editor with step-by-step creation workflow
- File upload support for bulk course import

#### Multi-Language Support
**Supported Languages:**
- Spanish, German, French, Italian, Portuguese, Russian
- Japanese (Hiragana, Katakana, Kanji, Manga)
- Korean (Hangul, Manhwa)
- Chinese (Simplified and Traditional)
- English variants (US, UK, Canadian, Australian)

#### Custom Tutor Creation
- User-specific custom tutors
- Global tutor templates (admin-created)
- Tutor personality configuration
- Location and image customization
- Preview mode for tutor testing

### Vocabulary & Learning Cards

#### Vocabulary Tracking
- Automatic vocabulary extraction from conversations
- Context-aware vocabulary storage with example sentences
- Exposure frequency counting
- Spaced Repetition System (SRS) for review scheduling
- Vocabulary filtering by language

#### Dual Learning Card System
- **Word Cards:** Visual vocabulary with concepts and images
- **Character Cards:** Individual character/symbol practice (hiragana, katakana, hangul, etc.)
- Flashcard display format in CLI
- AI-selected card type based on learning context

### Progressive Summarization

- Hierarchical message summarization for long conversations
- Level-1 summaries: Chunks of configurable messages (default: 10)
- Level-2+ summaries: Recursive summarization of lower-level summaries
- Async background execution to prevent blocking
- Token optimization with preserved context quality
- Monitoring endpoints for compression statistics

### Multi-Provider AI Integration

**Supported Providers:**
- OpenAI (GPT-4o, GPT-4o-mini)
- Azure OpenAI
- Anthropic (Claude Sonnet, Claude Opus)
- Ollama (local models: Llama 3, Granite 3.2, etc.)

**Features:**
- Strict JSON schema enforcement for structured outputs
- Provider-specific optimizations (native JSON_SCHEMA for OpenAI)
- Bring Your Own Key (BYOK) support with AES-256-GCM encryption
- Configurable model selection and temperature settings

### User Management & Authentication

- JWT-based authentication with access/refresh tokens
- User registration with email verification
- Password management and change functionality
- User language proficiency profiles
- Multiple language proficiency types (Target, Source, Native)
- Primary language designation
- Admin and Editor role-based access control

### Content Administration

#### Language Management
- Active/inactive language toggling
- Language metadata configuration
- Multi-variant language support (e.g., English US/UK/CA/AU)
- Flag icons for visual identification

#### Course Management
- Complete CRUD operations for courses
- Curriculum validation ensuring all lessons have content files
- Draft/Published state management
- Course categorization (Conversational, Grammar, Travel, Specialized)
- CEFR level targeting (startingLevel, targetLevel)
- Tag-based course organization

#### Tutor Management
- Global and user-specific tutor visibility
- Tutor archetype system for DRY configuration
- Personality and teaching style customization
- Image and emoji assignment

#### Unified Catalog Import
- Single-file YAML catalog format
- Support for languages, tutors, and courses in one file
- Optional tutor archetypes for reusability
- Embedded or file-referenced lesson content
- Validation before import
- REST API endpoints for programmatic import

### Test Harness & Quality Assurance

#### Pedagogical Test Harness
- LLM-as-judge evaluation methodology
- 6 evaluation dimensions: error detection, phase appropriateness, correction quality, encouragement balance, topic management, vocabulary teaching
- 5 built-in test scenarios covering critical pedagogical behaviors
- Comprehensive markdown reports with recommendations
- CI/CD integration with configurable pass/fail thresholds
- Rate limiting and retry logic for API quota management
- Multi-provider support (OpenAI, Azure OpenAI, Ollama)

### Rate Limiting & Subscription Management

#### Rate Limiting
- Request-based rate limiting per user
- Configurable limits by subscription tier
- X-RateLimit headers for client tracking
- Exposed in production for frontend integration
- Rate limit display in session side panel

#### Subscription Plans (Stripe Integration)
- Free tier with basic features
- Premium tiers with increased limits
- Stripe payment integration
- Subscription upgrade/downgrade
- Cancellation through Stripe portal
- Plan information display in user profile
- Auto-switch to Free + BYOK when API key provided

### Developer Experience

#### API Documentation
- OpenAPI 3.0 specification
- Interactive Swagger UI (`/swagger-ui.html`)
- HTTP client test files (`http-client-requests.http`)
- Comprehensive endpoint documentation

#### Testing
- Comprehensive test coverage (unit, integration, controller tests)
- MockK-based mocking strategy
- Fixture factories for consistent test data
- Jacoco code coverage reports

#### Deployment
- Docker and Docker Compose support
- GitHub Actions CI/CD pipeline
- Multi-environment configuration (dev, test, prod)
- Automated Docker builds and GHCR publishing
- Nginx-based frontend proxy
- Health checks and graceful shutdown

## API Highlights

### Authentication Endpoints
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get JWT tokens
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Get current user profile
- `POST /api/v1/auth/password` - Change password

### Chat Session Endpoints
- `POST /api/v1/chat/sessions` - Create learning session
- `POST /api/v1/chat/sessions/from-course` - Create session from course template
- `GET /api/v1/chat/sessions/active` - Get active sessions with progress
- `GET /api/v1/chat/sessions/{id}` - Get session with messages
- `POST /api/v1/chat/sessions/{id}/messages` - Send message (JSON)
- `POST /api/v1/chat/sessions/{id}/messages/stream` - Send message (SSE streaming)
- `POST /api/v1/chat/sessions/{id}/messages/initiate` - Tutor-initiated messages
- `PATCH /api/v1/chat/sessions/{id}/phase` - Update conversation phase
- `PATCH /api/v1/chat/sessions/{id}/topic` - Update conversation topic
- `PATCH /api/v1/chat/sessions/{id}/teaching-style` - Update teaching style

### Catalog Endpoints
- `GET /api/v1/catalog/languages` - List available languages
- `GET /api/v1/catalog/languages/{code}/courses` - List courses for language
- `GET /api/v1/catalog/languages/{code}/tutors` - List tutors for language
- `GET /api/v1/catalog/courses/{id}` - Get course details
- `GET /api/v1/catalog/tutors/{id}` - Get tutor details
- `POST /api/v1/catalog/tutors` - Create custom tutor
- `POST /api/v1/catalog/import` - Import unified catalog

### Vocabulary & Analytics Endpoints
- `GET /api/v1/vocabulary` - Get user's vocabulary
- `GET /api/v1/analytics/errors/patterns` - Get top error patterns
- `GET /api/v1/analytics/errors/trends/{errorType}` - Get error trend analysis
- `GET /api/v1/assessment/sessions/{id}/skills` - Get skill-specific CEFR breakdown

## Configuration

### Environment Variables

**Required:**
```bash
# AI Provider (at least one)
OPENAI_API_KEY=your-api-key
# OR
AZURE_OPENAI_API_KEY=your-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
# OR
OLLAMA_BASE_URL=http://localhost:11434

# Security
JWT_SECRET=change-this-to-a-secure-random-key
ENCRYPTION_KEY=$(openssl rand -base64 32)  # For BYOK feature
```

**Optional:**
```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
DEMO_USERNAME=demo
DEMO_PASSWORD=demo
```

### Application Configuration

Configurable via `application.yml`:
- AI model selection and temperature
- Strict schema enforcement toggle
- Database connection settings
- Rate limiting thresholds
- Summarization parameters
- Seeding behavior

## Documentation

Comprehensive documentation included:
- `README.md` - User-facing documentation with quick start
- `CLAUDE.md` - Project structure and development guidelines
- `backend/CLAUDE.md` - Backend architecture and API documentation
- `frontend/CLAUDE.md` - Frontend development guidelines
- `CATALOG_IMPORT_FORMAT.md` - Unified import format specification
- `COURSE_MIGRATION_GUIDE.md` - Course migration instructions
- `DEPLOYMENT_PROCESS.md` - Deployment setup and procedures
- `STRIPE_SETUP.md` - Stripe integration guide
- `CI.md` - GitHub Actions CI/CD pipeline documentation
- `TEST_HARNESS_SUMMARY.md` - Test harness overview

## Known Limitations

### BYOK (Bring Your Own Key)
The BYOK feature is **partially implemented**:
- ✅ User API keys can be configured and stored securely (AES-256-GCM encryption)
- ✅ User profile UI for key management
- ✅ REST API endpoints for key configuration
- ❌ Runtime ChatModel instantiation with user keys (currently uses system default)
- ❌ Actual API key validation (only format checks)

**Technical Limitation:** Spring AI 1.0.1's ChatModel constructors require complex dependency injection that's not well-suited for runtime instantiation. Future implementation will require either Spring AI API improvements or deeper integration with autoconfiguration internals.

### OAuth2 Authentication
Google Account login was implemented but subsequently reverted due to HTTPS configuration complexities in development and test environments. May be re-introduced in future releases.

## Migration Notes

For users upgrading from file-based course seeding:

1. **Automatic Migration:** Run `./gradlew :backend:migrateCoursesFromFiles`
2. **Manual Migration:** Use REST API to import individual courses
3. **UI Migration:** Use Course Management page "Import Course" button

After migration, lessons load from database automatically. File-based fallback remains available for backwards compatibility.

## System Requirements

- **Java:** 17 or higher
- **Node.js:** 18 or higher (for frontend development)
- **Docker:** Optional for containerized deployment
- **AI Provider:** OpenAI, Azure OpenAI, Anthropic, or Ollama account

## Quick Start

### Using Docker (Recommended)

```bash
# Create .env file with API keys
echo "OPENAI_API_KEY=your-key" > .env
echo "JWT_SECRET=your-secret" >> .env

# Start all services
docker-compose up --build -d

# Access application
# Frontend: http://localhost:5173
# Backend: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
```

### Standard Installation

```bash
# Backend
export OPENAI_API_KEY=your-api-key
./gradlew :backend:bootRun

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

## Acknowledgments

AI Tutor is built with modern technologies:
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Spring AI](https://spring.io/projects/spring-ai) - AI integration layer
- [React](https://react.dev/) - Frontend framework
- [Tailwind CSS](https://tailwindcss.com/) - Styling framework
- [OpenAI](https://openai.com/) - AI provider
- [CEFR](https://www.coe.int/en/web/common-european-framework-reference-languages) - Language proficiency framework

Pedagogical design based on research in:
- Second Language Acquisition (SLA)
- Noticing Hypothesis
- Affective Filter Hypothesis
- Spaced Repetition System (SRS)
- Error Correction in communicative contexts

## Support & Contributing

- **Issues:** Report bugs or request features via GitHub Issues
- **Contributing:** See contributing guidelines in README.md
- **License:** MIT License

## What's Next?

Planned for future releases:
- Complete BYOK runtime implementation
- Real-time collaborative learning sessions
- Mobile application (iOS/Android)
- Audio/video chat support
- Enhanced SRS algorithm
- Gamification features
- Community course sharing
- Advanced analytics dashboards
- Integration with external learning management systems

---

**Thank you for choosing AI Tutor!** We're excited to help you on your language learning journey.

For questions, feedback, or support, please visit our GitHub repository or contact us through the issue tracker.
