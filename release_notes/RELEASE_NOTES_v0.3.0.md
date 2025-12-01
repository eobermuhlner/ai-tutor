# AI Tutor v0.3.0

**Release Date:** December 1, 2025

Major release with PostgreSQL support, Prometheus/Grafana monitoring, configurable rate limits, enhanced tutors, improved lesson progress tracking, and production deployment readiness.

## What's Changed

### Infrastructure & Deployment

- **PostgreSQL Support:** Full production database support with migration from H2
  - Docker Compose integration for local development
  - Production-ready configuration for deployment
- **Monitoring & Metrics:** Comprehensive Prometheus and Grafana integration
  - AI provider and model-specific metrics tracking
  - Real-time performance monitoring dashboards
  - Production deployment with Nginx reverse proxy configuration
- **Production Profile:** Dedicated production environment configuration with optimized log levels

### Rate Limiting

- **Configurable Rate Limits:** Flexible rate limiting system per AI provider
- **Improved Rate Limit Display:** More accurate percentage calculations and compact UI display

### Content & Tutors

- **Enhanced Tutor Roster:** Expanded tutor selection with diverse backgrounds
  - Unique tutor ages and names for better personalization
  - Romanization specialists for Chinese languages
  - Improved person image search integration
- **New Etymology Course:** Etymology course added for all supported languages
- **Course Improvements:**
  - Removed time-based progression from courses (self-paced learning)
  - Renamed lessons to remove "week-" prefix for clarity
  - TEST course with smaller lessons for development
  - Enhanced SPECIAL course with provider-specific prompts
  - Cleaner language descriptions

### Chat Experience

- **Improved Lesson Progress Tracking:**
  - Replaced JSON-based lesson progress with structured `lessonProgressTurnCount` and `lessonProgressGoalsCompleted`
  - Better lesson completion evaluation
  - Enhanced lesson selection in chat side panel
  - Nicer session titles in catalog cards and chat interface
- **System Prompt Optimization:** Restructured prompts for better AI response caching
- **AI Model Compatibility:** Workaround for OpenAI GPT-5 temperature parameter handling

### Code Quality

- **ChatService Refactoring:** Extracted code duplication into `generateTutorResponse()` method
- **Exception Handling:** Fixed empty response bodies with custom exceptions
- **Data Model Cleanup:**
  - Renamed `lessonGoals` to `lessonContent` throughout codebase
  - Removed obsolete common module
- **Testing Infrastructure:**
  - Simplified and improved test harness
  - Fixed backend test suite
  - Organized imports and eliminated warnings

## Technical Details

### Backend Changes

- **Database Migration:**
  - PostgreSQL driver and JPA configurations
  - Docker Compose PostgreSQL service definition
  - Environment-specific database profiles

- **Monitoring Integration:**
  - Micrometer Prometheus registry
  - Custom metrics for AI provider usage
  - Grafana dashboard configurations
  - Nginx reverse proxy for Grafana with sub-path routing

- **Rate Limiting:**
  - Configurable per-provider rate limits
  - Improved rate limit calculation accuracy
  - Better error messaging

- **Code Refactoring:**
  - Extracted tutor orchestration logic
  - Simplified chat options generation
  - Better separation of concerns in ChatService

### Frontend Changes

- **UI Improvements:**
  - Better lesson selection interface
  - Compact rate limit indicators
  - Enhanced session title display

### Deployment

- **Docker Compose:**
  - PostgreSQL service with persistent volumes
  - Prometheus metrics collection
  - Grafana dashboard with Nginx reverse proxy
  - Environment variable configuration

- **Nginx Configuration:**
  - Grafana sub-path routing with redirect loop fixes
  - Proper header handling for HTTPS proxying
  - Diagnostic and troubleshooting tools

### Removed Features

- **Common Module:** Removed obsolete shared module

## Upgrade Notes

### Database Migration Required

Upgrading from v0.2.0 requires migrating from H2 to PostgreSQL for production deployments:

1. Export existing H2 data (if preserving user data)
2. Set up PostgreSQL database
3. Configure `POSTGRES_*` environment variables
4. Run application with `prod` profile

### For Developers

- Use `docker-compose.yml` for local PostgreSQL development
- Prometheus metrics available at `/actuator/prometheus`
- Grafana dashboards accessible at `/grafana` (requires Nginx configuration)

## Known Issues

None reported.

## System Requirements

- Java 17 or higher
- Node.js 18 or higher (for frontend)
- PostgreSQL 15+ (for production) or H2 (for development)
- Docker and Docker Compose (optional, for local development)
- AI Provider account (OpenAI, Azure OpenAI, Anthropic, or Ollama)

## API Changes

No breaking API changes. All existing endpoints remain compatible.

## Performance Improvements

- **System Prompt Caching:** Optimized prompt structure for better AI provider caching
- **Database Performance:** PostgreSQL production database for better scalability
- **Monitoring:** Real-time performance metrics for identifying bottlenecks

## Security Improvements

- Production-specific logging configuration
- Secure Grafana authentication and reverse proxy setup

---

**Full Changelog:** https://github.com/eobermuhlner/ai-tutor/compare/v0.2.0...v0.3.0

For questions, feedback, or support, please visit our GitHub repository or contact us through the issue tracker.
