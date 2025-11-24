# AI Tutor v0.2.0

**Release Date:** November 24, 2025

Feature release with Spring AI 1.1.0 upgrade, persistent error corrections, enhanced mobile UI, user avatars, and improved testing infrastructure.

## What's Changed

### Core Framework

- **Spring AI 1.1.0 Upgrade:** Major version upgrade from Spring AI 1.0.1 to 1.1.0
- Improved AI provider compatibility and performance
- Enhanced model integration capabilities

### Chat Experience

- **Persistent Corrections:** Error corrections now persist in chat history, allowing learners to review past mistakes
- **Conversation Metadata Updates:** Conversation metadata (statistics, progress) now updates independently every n turns, improving performance
- **Separate Correction Actions:** "Use Correction" and "Reply" are now separate actions for better control over error correction workflow
- Enhanced chat reliability and user experience

### User Interface Improvements

#### Mobile Experience
- **Mobile-Optimized Chat:** Comprehensive mobile mode improvements for better usability on small screens
- **Enter Key Support:** Accept Enter key for newlines in mobile mode chat input
- **Improved Side Panel:** Enhanced chat page side panel with smooth animations for both mobile and desktop
- **UI Consistency:** Removed round borders from chat page for consistent design language
- **Compact Rate Limiting:** More compact rate limit display in chat interface

#### User Profile
- **User Avatar Support:** Users can now upload and display custom avatar images
- **OAuth Account Handling:** Hide "Change Password" button for OAuth-linked accounts (e.g., Google)
- **Language Proficiency Editor:** Improved interface for editing language proficiencies

#### Navigation
- Removed back button from chat for streamlined navigation

### Testing & Quality

- **Integration Test Coverage:** Added comprehensive controller integration tests
  - `CatalogControllerIntegrationTest`
  - Additional `*ControllerIntegrationTest` classes
  - Refactored `BaseControllerIntegrationTest` for better reusability
- **Backend Test Expansion:** Significantly increased backend test coverage
- Improved test reliability and maintainability

### Code Cleanup

- **Removed Obsolete Components:**
  - Removed testharness (moved to separate module earlier, now removed entirely)
  - Removed obsolete CLI client
- **Frontend Cleanup:** General frontend code cleanup and optimization

## Technical Details

### Backend Changes

- **Spring AI 1.1.0 Integration:**
  - Updated dependencies and configurations
  - Leveraged new Spring AI 1.1.0 features and improvements
  - Enhanced AI provider compatibility

- **Persistent Correction Storage:**
  - Corrections now stored in message history
  - Improved data model for correction tracking
  - Better correction retrieval and display logic

- **Conversation Metadata Management:**
  - Decoupled metadata updates from chat flow
  - Periodic metadata refresh (configurable interval)
  - Improved performance for long conversations

- **Test Infrastructure:**
  - New integration test suite for all major controllers
  - Shared base test classes for consistency
  - Better test fixtures and mocking strategies

### Frontend Changes

- **Mobile Responsiveness:**
  - Redesigned chat interface for mobile devices
  - Touch-optimized controls and interactions
  - Improved keyboard handling on mobile browsers

- **Avatar Management:**
  - New avatar upload component
  - Image preview and cropping support
  - Avatar display in user profile and chat interface

- **UI Polish:**
  - Smooth side panel animations using CSS transitions
  - Consistent spacing and borders across components
  - Improved visual feedback for user actions

### Removed Features

- **CLI Client:** Obsolete command-line interface removed (frontend is the primary interface)
- **Standalone Testharness:** Test harness module removed (functionality integrated into main test suite)

## Upgrade Notes

No breaking changes for end users. Existing installations will continue to work without modifications.

### For Developers

- Update to Spring AI 1.1.0 may affect custom AI provider configurations
- Test harness module removed - use backend integration tests instead
- CLI client removed - use frontend or REST API directly

## Known Issues

None reported.

## System Requirements

Same as v0.1.0:
- Java 17 or higher
- Node.js 18 or higher (for frontend)
- Docker (optional)
- AI Provider account (OpenAI, Azure OpenAI, Anthropic, or Ollama)

## API Changes

No breaking API changes in this release. All existing endpoints remain compatible.

## Performance Improvements

- **Conversation Metadata:** Reduced overhead by updating metadata independently of chat flow
- **Mobile UI:** Optimized rendering and animations for better mobile performance
- **Backend Tests:** Faster test execution with refactored integration test infrastructure

---

**Full Changelog:** https://github.com/eobermuhlner/ai-tutor/compare/v0.1.0...v0.2.0

For questions, feedback, or support, please visit our GitHub repository or contact us through the issue tracker.
