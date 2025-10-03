# REST API Implementation Tasks

## ✅ ALL ENDPOINTS IMPLEMENTED

### 1. ✅ UserLanguageController - User Language Management
**Priority: HIGH** - Required for user profile and language selection features
**Status: COMPLETE** - All endpoints implemented

#### Implemented endpoints:
- ✅ `GET /api/v1/users/{userId}/languages`
  - Query params: `type` (Native/Learning)
  - Returns: List of user's languages with proficiency levels
  - Implementation: UserLanguageController.kt:21-36

- ✅ `POST /api/v1/users/{userId}/languages`
  - Body: `{ languageCode, type, cefrLevel, isNative }`
  - Returns: Created language proficiency record
  - Implementation: UserLanguageController.kt:38-53

- ✅ `PATCH /api/v1/users/{userId}/languages/{languageCode}`
  - Body: `{ cefrLevel }`
  - Returns: Updated language proficiency record
  - Implementation: UserLanguageController.kt:55-64

- ✅ `POST /api/v1/users/{userId}/languages/{languageCode}/primary`
  - Sets the primary target language for learning
  - Returns: Updated language proficiency record
  - Implementation: UserLanguageController.kt:66-78

- ✅ `DELETE /api/v1/users/{userId}/languages/{languageCode}`
  - Removes a language from user's profile
  - Returns: 204 No Content
  - Implementation: UserLanguageController.kt:80-89

### 2. ✅ CatalogController - Tutor Browsing
**Priority: MEDIUM** - Enhances course selection experience
**Status: COMPLETE** - All endpoints implemented

#### Implemented endpoints:
- ✅ `GET /api/v1/catalog/languages/{languageCode}/tutors`
  - Query params: `locale` (default: "en")
  - Returns: List of tutors for the language
  - Implementation: CatalogController.kt:115-136

- ✅ `GET /api/v1/catalog/tutors/{tutorId}`
  - Query params: `locale` (default: "en")
  - Returns: Detailed tutor information
  - Implementation: CatalogController.kt:138-159

### 3. ✅ AuthController - Password Management
**Priority: MEDIUM** - Standard security feature
**Status: COMPLETE** - Endpoint implemented

#### Implemented endpoint:
- ✅ `POST /api/v1/auth/password`
  - Body: `{ currentPassword, newPassword }`
  - Requires authentication
  - Returns: 204 No Content
  - Implementation: AuthController.kt:68-78

### 4. ✅ ChatController - Additional Session Management
**Priority: LOW** - Alternative flows, not critical path
**Status: COMPLETE** - All endpoints implemented

#### Implemented endpoints:
- ✅ `POST /api/v1/chat/sessions`
  - Body: `{ userId, tutorProfileId, targetLanguageCode, sourceLanguageCode, courseTemplateId?, customName? }`
  - Manual session creation (alternative to from-course)
  - Returns: Created session
  - Implementation: ChatController.kt:22-28

- ✅ `POST /api/v1/chat/sessions/{sessionId}/deactivate`
  - Marks session as inactive without deleting
  - Returns: Updated session
  - Implementation: ChatController.kt:68-86

- ✅ `PATCH /api/v1/chat/sessions/{sessionId}/topic`
  - Body: `{ currentTopic }`
  - Updates the current topic being discussed
  - Returns: Updated session
  - Implementation: ChatController.kt:134-143

- ✅ `GET /api/v1/chat/sessions/{sessionId}/topics/history`
  - Returns: Topic progression history for the session
  - Implementation: ChatController.kt:145-151

- ✅ `POST /api/v1/chat/sessions/{sessionId}/messages`
  - Body: `{ content }`
  - Non-streaming message endpoint (synchronous alternative)
  - Returns: Complete message with response
  - Implementation: ChatController.kt:153-162

## ✅ Implementation Complete

### ✅ Phase 1 (Critical for MVP) - COMPLETE
1. ✅ User Language Management endpoints
   - Required for user onboarding
   - Needed for language selection in courses

### ✅ Phase 2 (Enhanced Experience) - COMPLETE
1. ✅ Tutor browsing endpoints
   - Allows users to choose preferred tutors
   - Better course customization

2. ✅ Password management
   - Standard security requirement
   - User account management

### ✅ Phase 3 (Optional Enhancements) - COMPLETE
1. ✅ Additional chat session management
   - Alternative flows
   - Advanced session control

## Notes

### Current Working Endpoints
All endpoints in the following controllers are correctly implemented and working:
- ✅ `AuthController`: register, login, refresh, logout, me
- ✅ `VocabularyController`: getUserVocabulary, getVocabularyItemWithContexts
- ✅ `CatalogController`: listLanguages, listCourses, getCourseDetail
- ✅ `ChatController`: createSessionFromCourse, getSessions, getActiveSessions, getSession, deleteSession, updatePhase, streamMessages

### Query Parameter Discrepancies
The frontend currently sends `sourceLanguage` as a query parameter for catalog endpoints, but the backend expects `locale`. Review and align these parameter names.

### Authentication
All endpoints (except auth/register and auth/login) require Bearer token authentication, which is correctly handled by the frontend's axios interceptor.
