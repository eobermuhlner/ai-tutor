# AI Tutor v0.1.0

**Release Date:** November 21, 2025

Feature release with Google Login authentication, improved user management, database initialization, and testing infrastructure.

## What's Changed

### User Management
- **Role Management Enhancement:** Improved user role assignment and management system
- Better support for ADMIN and EDITOR role workflows

### Database & Initialization
- **Database Migration:** Seed lessons now migrate to database at application startup
- Simplified lesson handling with improved database initialization
- Automatic course content migration from file system to database

### Testing & CI/CD
- **Test Fixes:** Resolved backend test failures
- Enhanced test reliability and coverage
- Improved container naming for production vs test environments using IMAGE_TAG

### Authentication
- **Google Login:** New OAuth2 Google Account authentication implementation
- Enhanced third-party authentication flow with improved HTTPS support
- Fixed Google Login configuration for CI/CD environments
- Added comprehensive Google Login secret handling

### Documentation
- Comprehensive documentation cleanup
- Improved deployment process documentation

## Technical Details

### Backend Changes
- Automated lesson migration from `course-content/` directory to database on startup
- Simplified lesson loading logic
- Enhanced user role validation and assignment
- New Google OAuth2 authentication implementation with CI/CD support
- Improved Google login secret handling for multiple environments
- Fixed test suite for reliable CI/CD execution
- Debug logging improvements for authentication flows

### DevOps Improvements
- Container naming now includes IMAGE_TAG to distinguish prod and test deployments
- Improved environment isolation for multi-environment deployments

## Upgrade Notes

No breaking changes. Existing installations will automatically migrate lessons to database on first startup.

## Known Issues

None reported.

## System Requirements

Same as v0.0.1:
- Java 17 or higher
- Node.js 18 or higher (for frontend)
- Docker (optional)
- AI Provider account (OpenAI, Azure OpenAI, Anthropic, or Ollama)
