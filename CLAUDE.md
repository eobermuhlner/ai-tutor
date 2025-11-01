# AI Tutor - Multi-Module Project

An intelligent language learning platform with AI-powered conversational tutoring, real-time error correction, and vocabulary tracking.

## Project Structure

This is a multi-module Gradle project with separate backend and frontend:

```
ai-tutor/
├── backend/           # Spring Boot REST API (Kotlin)
├── frontend/          # React web application (TypeScript + Vite)
├── CLAUDE_COURSE.md   # Course lesson writing guidelines
└── build.gradle       # Root Gradle configuration
```

### Module Documentation

- **Backend**: See `backend/CLAUDE.md` for backend-specific documentation
- **Frontend**: See `frontend/CLAUDE.md` for frontend-specific documentation
- **Course Writing**: See `CLAUDE_COURSE.md` for lesson authoring guidelines

## Quick Start

### Backend (Spring Boot + Kotlin)

```bash
# Build and run backend server
./gradlew :backend:bootRun

# Run backend tests
./gradlew :backend:test

# Run test harness
./gradlew :backend:runTestHarness
```

**Default ports:**
- API Server: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

### Frontend (React + Vite)

```bash
# Install dependencies (first time only)
cd frontend
npm install

# Run development server
npm run dev

# Build for production
npm run build
```

**Default ports:**
- Dev Server: `http://localhost:5173`

## Architecture Overview

### Backend (`backend/`)

**Tech Stack:**
- Kotlin 1.9.25 + Spring Boot 3.5.6
- Spring AI 1.0.1 (OpenAI, Azure OpenAI, Ollama)
- H2 Database (JPA)
- Gradle build system

**Key Features:**
- RESTful API (`/api/v1/*`)
- JWT-based authentication
- Multi-provider AI integration (OpenAI, Azure OpenAI, Ollama)
- Adaptive tutoring with conversation phases (Free/Correction/Drill/Auto)
- Course-based learning with curriculum validation
- Vocabulary tracking and CEFR assessment
- Progressive message summarization

### Frontend (`frontend/`)

**Tech Stack:**
- React 19.1.1 + TypeScript 5.9.3
- Vite 5.4.20 build tool
- Tailwind CSS 3.4.18
- Zustand state management

**Key Features:**
- Course catalog browsing
- Interactive chat interface with real-time error corrections
- Session management with progress tracking
- Responsive design with Tailwind CSS
- Type-safe API client integration

## Development Workflow

### Backend Development

1. Make changes in `backend/src/`
2. Run tests: `./gradlew :backend:test`
3. Run server: `./gradlew :backend:bootRun`
4. Test with HTTP client: `backend/src/test/http/http-client-requests.http`

### Frontend Development

1. Make changes in `frontend/src/`
2. Dev server auto-reloads at `http://localhost:5173`
3. Test API integration with running backend
4. Build for production: `npm run build`

### Full-Stack Testing

1. Start backend: `./gradlew :backend:bootRun` (port 8080)
2. Start frontend: `cd frontend && npm run dev` (port 5173)
3. Test complete workflows in browser

## Git Commit Guidelines

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

## API Integration

The frontend consumes the backend REST API at `http://localhost:8080/api/v1/*`.

**Key endpoints:**
- `/auth/*` - Authentication (register, login, refresh tokens)
- `/chat/*` - Learning sessions and messaging
- `/catalog/*` - Languages, courses, and tutors
- `/vocabulary/*` - Vocabulary tracking
- `/analytics/*` - Error analytics
- `/assessment/*` - CEFR skill assessments

**Authentication:**
- Register user or use demo account (username: `demo`, password: `demo`)
- Login returns JWT access token and refresh token
- Include `Authorization: Bearer {token}` header in all authenticated requests

## Course Authoring

Language courses are stored in `backend/src/main/resources/course-content/`.

See **CLAUDE_COURSE.md** for comprehensive guidelines on:
- Course types (Conversational, Grammar, Travel)
- Lesson structure (8 required sections)
- Pedagogical approaches and research foundations
- Size requirements and quality standards
- Creating curriculum.yml files

Quick command: `/course:write-lesson` for guided lesson creation.

## Testing

### Backend Tests

```bash
# Run all backend tests
./gradlew :backend:test

# Run specific test class
./gradlew :backend:test --tests ChatControllerTest

# Run with coverage report
./gradlew :backend:test :backend:jacocoTestReport
```

### Pedagogical Test Harness

Automated LLM-as-judge evaluation for tutor behavior:

```bash
# List available scenarios
./gradlew :backend:runTestHarness --args="--list"

# Run all scenarios
./gradlew :backend:runTestHarness

# Run specific scenario
./gradlew :backend:runTestHarness --args="--scenario beginner-errors"
```

## Environment Configuration

### Backend Environment Variables

**OpenAI (default):**
```bash
export OPENAI_API_KEY=your-api-key
```

**Azure OpenAI:**
```bash
export AZURE_OPENAI_API_KEY=your-key
export AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
```

Edit `backend/src/main/resources/application.yml` to uncomment Azure configuration.

**Ollama (local):**
```bash
# Start Ollama with a model
ollama run llama3
```

Edit `backend/src/main/resources/application.yml` to uncomment Ollama configuration.

### Frontend Environment Variables

Copy `frontend/.env.example` to `frontend/.env` and configure:
```
VITE_API_BASE_URL=http://localhost:8080
```

## IntelliJ IDEA

The project includes pre-configured run configurations:
- **AiTutor Server (OpenAI)** - Run backend with OpenAI
- **AiTutor Server (Azure OpenAI)** - Run backend with Azure OpenAI
- **AiTutor Server (Ollama)** - Run backend with Ollama
- **TestHarnessMain (OpenAI)** - Run test harness
- **Tests in 'ai-tutor.backend.test'** - Run backend tests

All configurations use the `ai-tutor.backend.main` module.

## Preserving Existing Functionality

**CRITICAL**: Never remove or break existing features without explicit user approval.

When refactoring or adding features:
1. **Verify all existing REST endpoints still work** - Test with HTTP client
2. **Check existing tests pass** - Failing tests indicate broken functionality
3. **Review before deletion** - If removing code, ask user first
4. **Maintain backward compatibility** - Don't change existing API contracts
5. **Document breaking changes** - If unavoidable, clearly communicate impact

## Additional Resources

- **Backend API**: `backend/CLAUDE.md` - Full backend documentation
- **Frontend Guide**: `frontend/CLAUDE.md` - Frontend development guidelines
- **Course Writing**: `CLAUDE_COURSE.md` - Lesson authoring standards
- **README**: `README.md` - User-facing documentation
- **OpenAPI Docs**: `http://localhost:8080/swagger-ui.html` (when backend running)

## Project Links

- Backend source: `backend/src/main/kotlin/`
- Frontend source: `frontend/src/`
- Course content: `backend/src/main/resources/course-content/`
- HTTP tests: `backend/src/test/http/`
- Test scenarios: `scenarios/`
