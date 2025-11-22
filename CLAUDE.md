# AI Tutor - Multi-Module Project

An intelligent language learning platform with AI-powered conversational tutoring, real-time error correction, and vocabulary tracking.

## Project Structure

Multi-module Gradle project with separate backend and frontend:

```
ai-tutor/
├── backend/           # Spring Boot REST API (Kotlin)
├── frontend/          # React web application (TypeScript + Vite)
├── docs/              # Documentation files
└── build.gradle       # Root Gradle configuration
```

## Documentation Index

- **Backend**: `backend/CLAUDE.md` - Backend API, services, and data models
- **Frontend**: `frontend/CLAUDE.md` - React components and frontend architecture
- **Course Writing**: `CLAUDE_COURSE.md` - Lesson authoring standards
- **Catalog Import**: `docs/CATALOG_IMPORT_FORMAT.md` - Unified YAML import format
- **Migration**: `docs/COURSE_MIGRATION_GUIDE.md` - Course migration instructions
- **Deployment**: `docs/DEPLOYMENT_*.md` - Deployment and configuration guides

## Quick Start

### Backend (Spring Boot + Kotlin)

```bash
# Build and run backend server
./gradlew :backend:bootRun

# Run backend tests
./gradlew :backend:test

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

## Technology Stack

### Backend
- Kotlin 1.9.25 + Spring Boot 3.5.6
- Spring AI 1.0.1 (OpenAI, Azure OpenAI, Anthropic, Ollama)
- H2 Database (JPA) + Gradle

**Core Features:**
- RESTful API (`/api/v1/*`) with JWT authentication
- Multi-provider AI integration with optional user API keys (BYOK)
- Adaptive tutoring phases (Free/Correction/Drill/Auto)
- Vocabulary tracking and CEFR assessment
- Progressive message summarization

### Frontend
- React 19.1.1 + TypeScript 5.9.3
- Vite 5.4.20 + Tailwind CSS 3.4.18 + Zustand

**Core Features:**
- Interactive chat with real-time error corrections
- Course catalog and session management
- Type-safe API client integration

## Development Workflow

**Backend:** Edit `backend/src/` → Test `./gradlew :backend:test` → Run `./gradlew :backend:bootRun`
**Frontend:** Edit `frontend/src/` → Auto-reload at `localhost:5173` → Build `npm run build`

**Quality checks that MUST pass to complete any task:**
- ✅ `npm run lint` - No errors
- ✅ `npm run build` - Successful build
- ✅ `./gradlew :backend:test` - All tests pass
- ✅ `./gradlew build` - Complete build successful

## Git Commit Guidelines

**Format (MANDATORY):**
- First line: Imperative mood, no period (e.g., "Add feature" not "Added feature.")
- Body (optional): Brief explanation (one sentence per line)
- **❌ NEVER include**: "Generated with Claude Code", "Co-Authored-By: Claude", or ANY AI attribution

**Pre-commit checklist:**
1. ✅ No AI attribution lines
2. ✅ Imperative mood
3. ✅ No period at end of subject
4. ✅ Blank line between subject and body

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

Courses stored in `backend/src/main/resources/course-content/` (legacy) or database (preferred).

**See `CLAUDE_COURSE.md` for:**
- Lesson structure and pedagogical standards
- Course types (Conversational, Grammar, Travel)
- Creating curriculum.yml files

**Quick command:** `/course:write-lesson`

## Catalog Import & Migration

Import languages, tutors, and courses via unified YAML format (replaces legacy file-based seeding).

**📖 Complete documentation:**
- Format specification: `docs/CATALOG_IMPORT_FORMAT.md`
- Migration guide: `docs/COURSE_MIGRATION_GUIDE.md`

### Quick Migration

**Automatic (Recommended):**
```bash
./gradlew :backend:migrateCoursesFromFiles
```

**Manual via API:**
```bash
./gradlew :backend:extractCourseFiles
./upload-course-example.sh course-content-extracted/de-conversational-german YOUR_TOKEN
```

**Via UI:**
Course Management → "Import Course" → Upload `curriculum.yml` + lesson `.md` files

### Key API Endpoints

```bash
# Import complete catalog (languages + tutors + courses)
POST /api/v1/catalog/import
# Multipart: catalogFile (YAML), lessonFiles (optional .md files)
# Requires: ADMIN role

# Import individual course
POST /api/v1/courses/import/file
# Multipart: curriculumFile, lessonFiles, languageCode, courseName
# Requires: EDITOR or ADMIN role

# Validate before importing
POST /api/v1/catalog/import/validate
POST /api/v1/courses/import/validate
```

### Source Type Tracking

Courses and tutors track origin via `sourceType`:
- **SEEDED**: Legacy startup seeding (being phased out)
- **UPLOADED**: Imported via file upload API
- **CREATED**: Created via UI/API

## Testing

**Backend Tests:**
```bash
./gradlew :backend:test                                      # Run all tests
./gradlew :backend:test --tests ChatControllerTest           # Specific test
./gradlew :backend:test :backend:jacocoTestReport            # With coverage
```


## Environment Configuration

### Backend

**LLM Provider (choose one):**
```bash
# OpenAI (default)
export OPENAI_API_KEY=your-api-key

# Azure OpenAI (uncomment config in application.yml)
export AZURE_OPENAI_API_KEY=your-key
export AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com

# Anthropic (use dev-anthropic profile in application.yml)
export ANTHROPIC_API_KEY=your-api-key

# Ollama (local, uncomment config in application.yml)
ollama run llama3
```

**Security (required for BYOK):**
```bash
export ENCRYPTION_KEY=$(openssl rand -base64 32)  # User API key encryption
```

### Frontend

Copy `frontend/.env.example` to `frontend/.env`:
```
VITE_API_BASE_URL=http://localhost:8080
```

## Bring Your Own Key (BYOK)

⚠️ **Status: Partially Implemented**
- ✅ Secure API key storage (AES-256-GCM encrypted)
- ✅ User profile UI and REST API endpoints
- ✅ Support for OpenAI, Azure OpenAI, Anthropic

**Limitation:** Spring AI 1.0.1 lacks builder APIs for runtime ChatModel instantiation. Future implementation requires Spring AI improvements or upgrade.

**User Configuration:**
1. Profile & Settings → "AI Provider Settings (BYOK)"
2. Enter API key for desired provider
3. Save (format validation only)

**Key Endpoints:**
- `GET /api/v1/users/me/api-keys` - Get configuration status
- `PUT /api/v1/users/me/api-keys/{openai|azure-openai|anthropic}` - Set key
- `DELETE /api/v1/users/me/api-keys/{provider}` - Remove key

## IntelliJ IDEA Run Configurations

Pre-configured in `.idea/runConfigurations/`:
- **AiTutor Server (OpenAI/Azure/Ollama)** - Run backend with different providers
- **Tests in 'ai-tutor.backend.test'** - Run backend tests

All use `ai-tutor.backend.main` module.

## Development Guidelines

**CRITICAL: Preserve Existing Functionality**
1. ✅ Verify REST endpoints work after changes (use HTTP client)
2. ✅ Ensure tests pass (`./gradlew :backend:test`)
3. ✅ Ask before deleting code
4. ✅ Maintain API backward compatibility
5. ✅ Document breaking changes if unavoidable

## Quick Reference

**Documentation:**
- `backend/CLAUDE.md` - Backend API and architecture
- `frontend/CLAUDE.md` - Frontend components and patterns
- `CLAUDE_COURSE.md` - Lesson authoring standards
- `docs/CATALOG_IMPORT_FORMAT.md` - Import format specification
- `docs/COURSE_MIGRATION_GUIDE.md` - Migration instructions
- `README.md` - User-facing documentation

**Key Directories:**
- `backend/src/main/kotlin/` - Backend source code
- `frontend/src/` - Frontend source code
- `backend/src/main/resources/course-content/` - Legacy course files
- `backend/src/test/http/` - HTTP client test requests
- `scenarios/` - Test harness scenarios

**Tools:**
- OpenAPI docs: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`
