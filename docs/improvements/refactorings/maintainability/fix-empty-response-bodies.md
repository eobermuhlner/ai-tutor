# Fix Empty Response Bodies with Custom Exceptions

**Category**: maintainability
**Priority**: High
**Estimated Effort**: Small (1-2 hours)
**Risk Level**: Low
**Affected Files**: 3 files (2 controllers + 1 new exception class)

## Value Assessment Summary

**Assessment Verdict:** ⭐️ Recommended (High-value alternative to deferred global exception handler)
**Assessed By:** Derived from improvement-value-assessor agent re-validation
**Assessment Date:** 2025-11-27

**Key Assessment Factors**: Quick win delivering 80% of global exception handler value with 20% of effort. Fixes REST API standards violation (empty response bodies), provides structured error responses for affected endpoints, and integrates seamlessly with existing domain-specific exception handlers.

**Assessor's Key Findings** (from global handler re-validation):

- **Targeted Fix**: Addresses specific problem (5 empty responses) without infrastructure complexity
- **Low Effort**: 1-2 hours vs 6-10 hours for full global handler
- **Low Risk**: No handler ordering concerns, no breaking changes to clients expecting structured responses
- **Proven Pattern**: Reuses existing AuthExceptionHandler ErrorResponse format and exception handling pattern
- **Immediate Value**: All 5 affected endpoints return structured JSON immediately

## Location and Description

**Primary File(s)**:

- `ChatController.kt`: 2 instances of empty `badRequest().build()`
- `LessonController.kt`: 5 instances of empty `notFound().build()` and 2 instances of empty `badRequest().build()`
- `VocabularyController.kt`: 1 instance of empty `notFound().build()`

**Code Excerpt** (before - examples of empty response bodies):

```kotlin
// ChatController.kt:71 - No tutor available for course
val tutorId = request.tutorProfileId ?: run {
    catalogService.getTutorsForCourse(request.courseTemplateId).firstOrNull()?.id
        ?: return ResponseEntity.badRequest().build()  // ❌ No error message body
}

// ChatController.kt:83 - Session creation failed
val session = chatService.createSessionFromCourse(...)
    ?: return ResponseEntity.badRequest().build()  // ❌ No error message body

// LessonController.kt:36, 62, 76, 89, 103, 116 - Resource not found
val curriculum = lessonContentService.getCurriculum(courseId)
    ?: return ResponseEntity.notFound().build()  // ❌ No error message body

// LessonController.kt:85, 112 - Session not course-based
if (session.courseTemplateId == null) {
    return ResponseEntity.badRequest().build()  // ❌ No error message body
}

// VocabularyController.kt:55 - Vocabulary item not found
val result = vocabularyQueryService.getVocabularyItemWithContexts(itemId, currentUserId)
    ?: return ResponseEntity.notFound().build()  // ❌ No error message body
```

**What needs refactoring**: Controllers return bare HTTP status codes (400, 404) with no error message body when validation fails or resources aren't found. This violates REST API standards, makes debugging difficult for frontend developers, and prevents users from seeing meaningful error messages.

## Current Issues

1. **REST API Standards Violation** (Usability + Developer Experience):
   - Specific problem: 10 controller methods return `ResponseEntity.badRequest().build()` or `ResponseEntity.notFound().build()` with NO JSON body
   - Impact: Frontend receives HTTP status code but no explanation; impossible to display meaningful error messages; developers must guess what went wrong

2. **Inconsistent Error Handling** (Developer Experience):
   - Specific problem: Some controllers use custom exceptions (caught by `AuthExceptionHandler` with structured `ErrorResponse`), others return empty responses
   - Impact: Inconsistent API behavior; frontend must handle two different error patterns

3. **Poor Debuggability** (Developer Experience):
   - Specific problem: Empty responses provide no context about what failed (which validation rule, which resource ID)
   - Impact: Frontend developers file vague bug reports; backend developers spend time reproducing issues to identify root cause

**SOLID Principles Violated**:

- [x] Single Responsibility Principle - Controllers manually format HTTP responses instead of using exception handlers
- [ ] Open/Closed Principle
- [ ] Liskov Substitution Principle
- [ ] Interface Segregation Principle
- [ ] Dependency Inversion Principle

## Proposed Solution

**High-Level Approach**:
Create two simple custom exceptions (`ResourceNotFoundException`, `BadRequestException`) that follow the existing pattern from `AuthExceptions.kt`. The existing `AuthExceptionHandler` will catch these new exceptions and return structured `ErrorResponse` JSON automatically.

**Alternative approach**: Extend `AuthExceptionHandler` to handle generic exceptions, OR create a new `CoreExceptionHandler` in `core/exception/` package.

**Code Excerpt** (after refactoring - conceptual):

```kotlin
// Step 1: Create new exception classes (backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/CoreExceptions.kt)
package ch.obermuhlner.aitutor.core.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)
```

```kotlin
// Step 2: Add exception handlers (extend existing AuthExceptionHandler or create new handler)
// Option A: Extend AuthExceptionHandler.kt
@ExceptionHandler(ResourceNotFoundException::class)
fun handleResourceNotFoundException(
    ex: ResourceNotFoundException,
    request: WebRequest
): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.NOT_FOUND.value(),
        error = "Not Found",
        message = ex.message ?: "Resource not found",
        path = request.getDescription(false).removePrefix("uri=")
    )
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
}

@ExceptionHandler(BadRequestException::class)
fun handleBadRequestException(
    ex: BadRequestException,
    request: WebRequest
): ResponseEntity<ErrorResponse> {
    val errorResponse = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.BAD_REQUEST.value(),
        error = "Bad Request",
        message = ex.message ?: "Invalid request",
        path = request.getDescription(false).removePrefix("uri=")
    )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
}
```

```kotlin
// Step 3: Replace empty responses in controllers

// ChatController.kt:71 - BEFORE
val tutorId = request.tutorProfileId ?: run {
    catalogService.getTutorsForCourse(request.courseTemplateId).firstOrNull()?.id
        ?: return ResponseEntity.badRequest().build()
}

// ChatController.kt:71 - AFTER
val tutorId = request.tutorProfileId ?: run {
    catalogService.getTutorsForCourse(request.courseTemplateId).firstOrNull()?.id
        ?: throw BadRequestException("No tutors available for course ${request.courseTemplateId}")
}

// LessonController.kt:36 - BEFORE
val curriculum = lessonContentService.getCurriculum(courseId)
    ?: return ResponseEntity.notFound().build()

// LessonController.kt:36 - AFTER
val curriculum = lessonContentService.getCurriculum(courseId)
    ?: throw ResourceNotFoundException("Curriculum not found for course: $courseId")

// LessonController.kt:85 - BEFORE
if (session.courseTemplateId == null) {
    return ResponseEntity.badRequest().build()
}

// LessonController.kt:85 - AFTER
if (session.courseTemplateId == null) {
    throw BadRequestException("Session $sessionId is not associated with a course")
}
```

**New Files/Modules to Create**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/CoreExceptions.kt` - New exception classes
- OPTIONAL: `backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/CoreExceptionHandler.kt` - New handler (if not extending AuthExceptionHandler)

**Files to Modify**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt` (2 instances)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/lesson/controller/LessonController.kt` (7 instances)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/vocabulary/controller/VocabularyController.kt` (1 instance)
- OPTIONAL: `backend/src/main/kotlin/ch/obermuhlner/aitutor/auth/exception/AuthExceptionHandler.kt` (extend with 2 new handlers)

## Benefits

**Immediate Benefits**:

- ✅ Eliminates 10 empty response bodies across 3 controllers (ChatController: 2, LessonController: 7, VocabularyController: 1)
- ✅ Provides structured JSON error responses for all affected endpoints (consistent with existing auth errors)
- ✅ Frontend can display meaningful error messages to users (e.g., "No tutors available for this course")
- ✅ Improves API debugging (error messages include context like course IDs, session IDs)

**Long-term Benefits**:

- 🔄 Establishes reusable exception pattern for future controllers (no need to repeat error handling)
- 📈 Reduces frontend error handling complexity (single consistent error format across all APIs)
- 🛡️ Prevents future developers from returning empty responses (pattern is obvious and easy to follow)

**Metrics** (how to measure success):

- [ ] Empty response bodies reduced: From 10 to 0 (100% elimination)
- [ ] API error consistency: 100% of error responses return structured JSON (currently ~92% - 4 handlers cover most cases)
- [ ] Frontend error handling: Count of "generic error occurred" messages vs specific error messages (should increase specificity)
- [ ] Developer debugging time: Anecdotal - track time to identify root cause of 404/400 errors before/after

## Risks and Considerations

**Technical Risks**:

- ⚠️ **Breaking Changes**: Endpoints currently return empty bodies; clients may expect HTTP status code only - Mitigation: Very unlikely - clients typically parse JSON error responses if present; empty bodies are anti-pattern
- ⚠️ **Exception Handler Conflicts**: If extending AuthExceptionHandler, ensure new exceptions don't conflict with auth exceptions - Mitigation: Use distinct exception names (ResourceNotFoundException vs UserNotFoundException)

**Business Risks**:

- ⚠️ **None** - This is a bug fix/improvement with no business impact beyond better UX

**Breaking Changes**: Potentially yes - endpoints will return JSON error bodies instead of empty responses

**Rollback Strategy**:
1. Revert controller changes (throw exception → return ResponseEntity.build())
2. Remove new exception handlers from AuthExceptionHandler
3. Delete CoreExceptions.kt file
4. Redeploy (rollback takes <5 minutes)

## Implementation Plan

**Prerequisites**:

1. [ ] Review existing AuthExceptionHandler to understand ErrorResponse format
2. [ ] Verify frontend can handle structured error responses (check API client error parsing)
3. [ ] Identify if any integration tests expect empty responses (will need updates)

**Step-by-Step Implementation**:

1. **Phase 1: Create Exception Classes** (Estimated: 15 minutes)
   - [ ] Create `backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/CoreExceptions.kt`
   - [ ] Define `ResourceNotFoundException(message: String) : RuntimeException(message)`
   - [ ] Define `BadRequestException(message: String) : RuntimeException(message)`
   - [ ] Follow existing pattern from `AuthExceptions.kt` (simple, no additional fields)

2. **Phase 2: Add Exception Handlers** (Estimated: 20 minutes)
   - [ ] **Option A** (Recommended): Extend `AuthExceptionHandler.kt`
     - Add `@ExceptionHandler(ResourceNotFoundException::class)` method
     - Add `@ExceptionHandler(BadRequestException::class)` method
     - Reuse existing `ErrorResponse` data class
   - [ ] **Option B**: Create `CoreExceptionHandler.kt` (if want separation)
     - Copy ErrorResponse to shared location
     - Create new @RestControllerAdvice handler

3. **Phase 3: Replace Empty Responses in ChatController** (Estimated: 10 minutes)
   - [ ] Line 71: `throw BadRequestException("No tutors available for course ${request.courseTemplateId}")`
   - [ ] Line 83: `throw BadRequestException("Failed to create session for course ${request.courseTemplateId}")`
   - [ ] Add import: `import ch.obermuhlner.aitutor.core.exception.BadRequestException`

4. **Phase 4: Replace Empty Responses in LessonController** (Estimated: 20 minutes)
   - [ ] Line 36: `throw ResourceNotFoundException("Curriculum not found for course: $courseId")`
   - [ ] Line 62: `throw ResourceNotFoundException("Lesson not found: $lessonId in course: $courseId")`
   - [ ] Line 76: `throw ResourceNotFoundException("Session not found: $sessionId")`
   - [ ] Line 85: `throw BadRequestException("Session $sessionId is not associated with a course")`
   - [ ] Line 89: `throw ResourceNotFoundException("No lesson progression available for session: $sessionId")`
   - [ ] Line 103: `throw ResourceNotFoundException("Session not found: $sessionId")`
   - [ ] Line 112: `throw BadRequestException("Session $sessionId is not associated with a course")`
   - [ ] Line 116: `throw ResourceNotFoundException("No next lesson available for session: $sessionId")`
   - [ ] Add imports

5. **Phase 5: Replace Empty Responses in VocabularyController** (Estimated: 5 minutes)
   - [ ] Line 55: `throw ResourceNotFoundException("Vocabulary item not found: $itemId")`
   - [ ] Add import

6. **Phase 6: Update Tests** (Estimated: 15 minutes)
   - [ ] Search for tests expecting HTTP 400/404 with empty bodies
   - [ ] Update assertions to expect structured ErrorResponse JSON
   - [ ] Example: `response.statusCode shouldBe HttpStatus.BAD_REQUEST` → `response.body?.message shouldContain "No tutors available"`

**Testing Requirements**:

- [ ] Unit tests: Verify new exceptions are thrown in correct scenarios
- [ ] Integration tests: Verify exceptions are caught and return ErrorResponse JSON with correct status codes
- [ ] Manual testing: Trigger each error scenario via HTTP client and verify structured error response
  - Trigger "no tutors available" error
  - Trigger "curriculum not found" error
  - Trigger "session not course-based" error
  - Trigger "vocabulary item not found" error

**Validation Checklist**:

- [ ] All existing tests pass (`./gradlew :backend:test`)
- [ ] New exception handlers registered and functioning
- [ ] All 10 empty response instances replaced with throw statements
- [ ] Build succeeds (`./gradlew build`)
- [ ] Manual API test: GET /api/v1/lessons/{invalid-course-id}/curriculum returns 404 with JSON body
- [ ] Manual API test: POST /api/v1/chat/sessions/from-course with course having no tutors returns 400 with JSON body

## Dependencies & Sequencing

**Must Complete Before This**: None - Can be implemented immediately

**Should Complete After This**:
- [ ] Optional: Add monitoring to track 400/404 error rates (measure if problem is worse than expected)
- [ ] Optional: Update API documentation to show structured error response examples

**Can Run in Parallel With**:
- [ ] "Extract Shared Tutor Orchestration Logic" (maintainability/extract-tutor-orchestration-duplication.md)
- [ ] "Decompose ChatService God Class" (architecture/decompose-chatservice-god-class.md)
- [ ] Any other refactoring work

**Foundation Refactoring**: No - Standalone improvement with no dependencies

## Related Files

**Files to Modify**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt` - Replace 2 empty responses (lines 71, 83)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/lesson/controller/LessonController.kt` - Replace 7 empty responses (lines 36, 62, 76, 85, 89, 103, 112, 116)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/vocabulary/controller/VocabularyController.kt` - Replace 1 empty response (line 55)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/auth/exception/AuthExceptionHandler.kt` - Add 2 new exception handlers (ResourceNotFoundException, BadRequestException)

**Files to Create**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/CoreExceptions.kt` - New exception classes

**Files with Dependencies** (won't modify but are affected):

- All frontend API clients - Will receive structured error responses instead of empty bodies
- Integration tests expecting empty 400/404 responses - Will need updates

**Documentation to Update**:

- `backend/CLAUDE.md` - Add section on custom exception pattern (when to throw ResourceNotFoundException vs BadRequestException)
- `README.md` - Update API error response examples to show structured ErrorResponse format

## Historical Context

**Why This Issue Exists**:
The project likely started with controllers returning `ResponseEntity.badRequest().build()` as quick implementations during initial development. As the project matured, domain-specific exception handlers (auth, payments, AI) were added for their specific needs, but generic validation/not-found cases were never refactored to use the same pattern.

This is a common evolution in Spring Boot projects - domain exceptions are added reactively when specific error types emerge (authentication failures, payment errors), but generic HTTP status code responses remain as technical debt.

**Previous Attempts**:
No previous attempts to standardize error responses for these endpoints. The `AuthExceptionHandler` and `ErrorResponse` pattern established the foundation, but it was never extended to generic controller errors.

## Relationship to Deferred Proposal

This proposal is the **high-value alternative** recommended by the improvement-value-assessor agent when the "Add Global Exception Handler" proposal was deferred. Key differences:

| Aspect | Global Exception Handler (Deferred) | Fix Empty Responses (This Proposal) |
|--------|-------------------------------------|-------------------------------------|
| Effort | 6-10 hours | 1-2 hours |
| Scope | Catch ALL uncaught exceptions | Fix 10 specific empty responses |
| Risk | Moderate (handler ordering, breaking changes) | Low (targeted fix, existing pattern) |
| Value | Addresses 68 exception patterns | Addresses 10 empty responses (the actual UX problem) |
| Complexity | New infrastructure (development mode detection, handler ordering) | Reuses existing AuthExceptionHandler pattern |
| Testing | Complex integration tests for handler precedence | Simple controller tests |

**Why this delivers 80% of value with 20% of effort:**
- The 10 empty responses are the actual user-facing problem (frontend can't display error messages)
- The 68 uncaught exceptions are mostly intentional fail-fast code (initialization errors)
- Fixing the empty responses solves the REST API standards violation without infrastructure complexity
