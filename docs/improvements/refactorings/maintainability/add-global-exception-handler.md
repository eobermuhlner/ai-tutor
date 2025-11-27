# Add Global Exception Handler for Unhandled System Exceptions

**Category**: maintainability
**Priority**: Deferred
**Status**: Deferred (2025-11-27)
**Original Effort Estimate**: Small (2-4 hours)
**Revised Effort Estimate**: Medium (6-10 hours)
**Risk Level**: Moderate (revised from Low)
**Affected Files**: 2 files (1 new handler + 1 test file)

## Deferral Reasoning (2025-11-27)

This proposal has been **deferred** based on re-validation assessment that found:

1. **No Production Evidence**: Zero git commits about 500 errors, stack trace leaks, or unhandled exceptions in last 3 months
2. **Effort Underestimated**: Actual effort 6-10 hours (integration tests, handler ordering, refactoring empty responses) vs claimed 2-4 hours
3. **Risk Underestimated**: Breaking changes (5 endpoints with empty responses → JSON), handler ordering complexity, logging duplication
4. **Better Alternative Exists**: Fix 5 empty `ResponseEntity.badRequest().build()` calls with custom exceptions (1-2 hours, 80% of value)
5. **Premature Optimization**: Codebase already has 4 domain-specific handlers + 11 custom exceptions handling most cases effectively

**Recommended Instead**:
- Quick win: Fix 5 empty responses with custom `BadRequestException` (1-2 hours)
- Add monitoring: Log uncaught exceptions to measure if this becomes a real problem (30 min)
- Document patterns: Update CLAUDE.md with exception handling guidelines (1 hour)

**Re-evaluation Criteria** (defer until these conditions met):
- [ ] Production monitoring shows frequent unhandled exceptions (add logging first to measure)
- [ ] User complaints about poor error messages (no evidence currently)
- [ ] Frontend team requests structured error responses (appears working fine)
- [ ] Security audit flags stack trace leakage (no findings mentioned)

**Original Proposal** (archived for future reference):

## Value Assessment Summary

**Assessment Verdict:** ⭐️ Recommended
**Assessed By:** improvement-value-assessor agent
**Assessment Date:** 2025-11-27

**Key Assessment Factors**: High-value, low-effort improvement that addresses genuine gap in API error handling. Directly impacts developer experience and production reliability by providing consistent error responses, preventing stack trace leakage, and establishing centralized logging point.

**Assessor's Key Findings**:

- **Security Benefit**: Stack trace leakage exposes internal system details in production (moderate severity vulnerability)
- **Production Bug Fix**: Empty response bodies violate REST standards and break API contracts
- **Broad Impact**: Affects all endpoints, not just specific domains; establishes foundational hygiene for production REST API
- **Developer Value**: Eliminates per-controller error handling boilerplate; centralized logging improves debugging
- **Low Risk**: Spring's handler resolution guarantees domain-specific handlers take precedence; rollback is trivial

**Critical Challenges Identified**:

- **Development mode detection fragility**: Proposed `System.getProperty()` approach unreliable; should inject `Environment` bean
- **Breaking change understatement**: Controllers returning empty response bodies will now return JSON; acknowledge in release notes
- **Testing complexity**: Must verify domain-specific handlers retain precedence through integration tests
- **Scope conflation**: Silent exception catching and string-based error detection are separate issues, not solved by this handler

## Location and Description

**Primary File(s)**:

- 45+ throw statements across multiple services/controllers without corresponding exception handlers

**Code Excerpt** (before - examples of unhandled exceptions):

```kotlin
// UserChatModelFactory.kt:109
throw IllegalStateException("Failed to create OpenAI ChatModel for user ${user.id}", e)

// UserChatModelFactory.kt:118
?: throw IllegalStateException("Azure OpenAI endpoint not configured for user ${user.id}")

// SpringAiAudioService.kt:59, 76, 79
throw UnsupportedOperationException("TTS is not enabled. Check application-ai-*.yml configuration.")
throw UnsupportedOperationException("TTS not supported with Ollama provider. Use OpenAI or Azure OpenAI.")
throw UnsupportedOperationException("No TTS provider configured. Check application-ai-*.yml configuration.")

// SpringAiAudioService.kt:147, 153
val audioBytes = response.body ?: throw RuntimeException("Empty response from OpenAI TTS API")
throw RuntimeException("Failed to synthesize speech with OpenAI: ${e.message}", e)

// ChatController.kt:71, 83
?: return ResponseEntity.badRequest().build()  // Returns 400 with NO error body

// AdminController.kt:237
throw IllegalArgumentException("Email already in use")  // Not caught by DuplicateEmailException handler

// LanguageController.kt:51, 62
throw IllegalArgumentException("Language with code ${language.code} already exists")
throw IllegalArgumentException("Language with code $code not found")
```

**What needs refactoring**: The backend has domain-specific exception handlers (`@RestControllerAdvice`) for auth, payments, AI services, and rate limiting - but lacks a **global handler for uncaught exceptions**. This creates three critical problems:

1. **Inconsistent Error Responses**: Some endpoints return structured `ErrorResponse`, others return bare HTTP status codes with no body
2. **Stack Trace Leakage**: Unhandled exceptions expose internal system details in production (security risk)
3. **No Centralized Logging**: Scattered exception handling makes debugging difficult

## Current Issues

1. **Unhandled Exception Types** (Security + Usability):
   - Specific problem: 45+ exceptions (`IllegalStateException`, `UnsupportedOperationException`, `RuntimeException`, `IllegalArgumentException`) thrown but not caught by existing handlers
   - Impact: Production systems return generic 500 errors with full stack traces, exposing internal implementation details (file paths, database schemas, API keys in error messages)

2. **Empty Response Bodies** (API Standards Violation):
   - Specific problem: Controllers use `ResponseEntity.badRequest().build()` which returns HTTP 400/500 with NO error message body
   - Impact: Frontend/clients receive empty responses; impossible to display meaningful error messages to users; violates REST API conventions

3. **Fragmented Error Logging** (Developer Experience):
   - Specific problem: No centralized logging point for uncaught exceptions; each controller may or may not log errors
   - Impact: Debugging production issues requires searching logs across multiple services; difficult to track error patterns

**SOLID Principles Violated**:

- [x] Single Responsibility Principle - Controllers handle both business logic AND error response formatting (should be centralized)
- [ ] Open/Closed Principle
- [ ] Liskov Substitution Principle
- [ ] Interface Segregation Principle
- [ ] Dependency Inversion Principle

## Proposed Solution

**High-Level Approach**:
Create a `GlobalExceptionHandler` using Spring's `@RestControllerAdvice` that catches all uncaught exceptions and provides consistent, structured error responses. This handler will:

1. Catch system-level exceptions (`IllegalStateException`, `UnsupportedOperationException`, `IllegalArgumentException`, generic `Exception`)
2. Map exceptions to appropriate HTTP status codes
3. Return structured `ErrorResponse` JSON with timestamp, status, error type, message, path, and optional details
4. Log all errors centrally with full stack traces for debugging
5. Prevent stack trace leakage in production by conditionally including details only in development mode

**Code Excerpt** (after refactoring - conceptual):

```kotlin
package ch.obermuhlner.aitutor.core.exception

import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.validation.FieldError

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: String? = null
)

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Handle IllegalStateException - Internal configuration/state errors
     * These indicate programming errors or misconfigurations
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Illegal state error: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "System is in an invalid state. Please contact support.",
            path = request.getDescription(false).removePrefix("uri="),
            details = if (isDevelopmentMode()) ex.message else null
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * Handle UnsupportedOperationException - Feature not enabled/configured
     */
    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(
        ex: UnsupportedOperationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Unsupported operation attempted: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_IMPLEMENTED.value(),
            error = "Not Implemented",
            message = ex.message ?: "This feature is not available",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse)
    }

    /**
     * Handle IllegalArgumentException - Invalid request parameters
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid argument: ${ex.message}")

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid request parameter",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * Handle Bean Validation errors (@Valid annotations)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.joinToString("; ") { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            "$fieldName: ${error.defaultMessage}"
        }

        logger.warn("Validation failed: $errors")

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "Invalid request data",
            path = request.getDescription(false).removePrefix("uri="),
            details = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * Catch-all handler for any uncaught exceptions
     * This ensures consistent error responses even for unexpected errors
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please try again later.",
            path = request.getDescription(false).removePrefix("uri="),
            details = if (isDevelopmentMode()) "${ex.javaClass.simpleName}: ${ex.message}" else null
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * CRITICAL FIX: Use Spring Environment injection instead of System.getProperty
     * The original proposal used System.getProperty("spring.profiles.active") which is unreliable.
     */
    private fun isDevelopmentMode(): Boolean {
        return environment.activeProfiles.any { it in setOf("dev", "local", "test") }
    }
}
```

**New Files/Modules to Create**:

- `backend/src/main/kotlin/ch/obermuhlner/aitutor/core/exception/GlobalExceptionHandler.kt` - New global exception handler
- `backend/src/test/kotlin/ch/obermuhlner/aitutor/core/exception/GlobalExceptionHandlerTest.kt` - Unit tests
- Consider extracting `ErrorResponse` to shared file if not already present (currently duplicated in auth/payment/AI handlers)

## Benefits

**Immediate Benefits**:

- ✅ Eliminates 45+ uncaught exceptions (IllegalStateException, UnsupportedOperationException, RuntimeException, IllegalArgumentException)
- ✅ Provides structured JSON error responses for all API endpoints (currently some return empty bodies)
- ✅ Prevents stack trace leakage in production environments (security improvement)
- ✅ Centralizes error logging (all uncaught exceptions logged in one place with full context)

**Long-term Benefits**:

- 🔄 Foundation for centralized error monitoring integration (Sentry, DataDog, CloudWatch)
- 📈 Easier debugging with consistent error logging patterns across all endpoints
- 🛡️ Reduces controller complexity (no manual error response building required)
- 🎯 Enables API error analytics and pattern detection

**Metrics** (how to measure success):

- [ ] Code complexity reduced: Remove error handling boilerplate from controllers (estimate: 20+ lines of code removed across controllers)
- [ ] Test coverage increased: From current state to 100% coverage of exception handlers
- [ ] Error response consistency: 100% of API endpoints return structured JSON error bodies (currently ~80%)
- [ ] Security audit: Zero stack traces leaked in production logs after deployment
- [ ] Logging centralization: All uncaught exceptions logged through GlobalExceptionHandler (verify via log analysis)

## Risks and Considerations

**Technical Risks**:

- ⚠️ **Handler Ordering**: Spring resolves most-specific handler first; global handler must not override domain-specific handlers (auth, payments, AI services) - Mitigation: Integration tests verify domain handlers take precedence; Spring's `@Order` annotation can explicitly set precedence if needed
- ⚠️ **Development Mode Detection**: Using `Environment.activeProfiles` instead of `System.getProperty` prevents false positives - Mitigation: Add unit test to verify isDevelopmentMode() returns correct value for each profile
- ⚠️ **Error Message Exposure**: Stack traces in development mode could leak if profile detection fails - Mitigation: Fail-safe default to production mode if profile detection is ambiguous; add integration test

**Business Risks**:

- ⚠️ **Breaking Client Contracts**: Clients expecting empty HTTP 400/500 responses may break if suddenly receiving JSON bodies - Mitigation: This fixes a REST API standards violation; document change in release notes; monitor client error rates post-deployment; consider phased rollout with feature flag if critical clients identified

**Breaking Changes**: Yes - Controllers previously returning empty response bodies will now return structured JSON error bodies

**Rollback Strategy**:
1. Remove `@RestControllerAdvice` annotation from `GlobalExceptionHandler` class (disables handler immediately)
2. If using feature flag (optional): Set `app.error-handling.global-handler-enabled=false` in application.yml
3. Redeploy with rollback commit
4. Old behavior (empty response bodies, unhandled exceptions) resumes within minutes

## Implementation Plan

**Prerequisites**:

1. [ ] Review existing domain-specific exception handlers to understand handler precedence
2. [ ] Identify all controllers returning `ResponseEntity.badRequest().build()` or `ResponseEntity.notFound().build()` (grep pattern: `\.build\(\)`)
3. [ ] Check if `ErrorResponse` data class already exists in existing handlers (may need to move to shared location)

**Step-by-Step Implementation**:

1. **Phase 1: Create Global Handler** (Estimated: 1.5 hours)
   - [ ] Create `GlobalExceptionHandler.kt` in `core/exception/` package
   - [ ] Inject `Environment` bean for profile detection
   - [ ] Implement 5 exception handler methods (IllegalStateException, UnsupportedOperationException, IllegalArgumentException, MethodArgumentNotValidException, generic Exception)
   - [ ] Add comprehensive KDoc comments explaining when each handler is used
   - [ ] Implement `isDevelopmentMode()` helper using `Environment.activeProfiles`

2. **Phase 2: Write Unit Tests** (Estimated: 1 hour)
   - [ ] Create `GlobalExceptionHandlerTest.kt`
   - [ ] Test each exception handler returns correct HTTP status code
   - [ ] Test error response structure matches `ErrorResponse` schema
   - [ ] Test `isDevelopmentMode()` returns true for dev/local/test profiles, false for prod
   - [ ] Test details field is populated in dev mode, null in production mode
   - [ ] Mock `Environment` and `WebRequest` for unit tests

3. **Phase 3: Integration Testing** (Estimated: 1 hour)
   - [ ] Create integration test that triggers each exception type through actual REST endpoints
   - [ ] Verify domain-specific handlers still take precedence (e.g., trigger `InvalidCredentialsException` and confirm `AuthExceptionHandler` catches it, not global handler)
   - [ ] Verify global handler catches previously unhandled exceptions (e.g., trigger `IllegalStateException` in a test controller)
   - [ ] Test with both dev and prod profiles to verify stack trace behavior

4. **Phase 4: Documentation and Deployment** (Estimated: 0.5 hours)
   - [ ] Update CLAUDE.md backend documentation to mention global exception handler
   - [ ] Add release notes entry explaining breaking change (empty responses now return JSON bodies)
   - [ ] Deploy to staging environment and monitor error logs
   - [ ] Verify no regressions in existing error handling (domain handlers still work)

**Testing Requirements**:

- [ ] Unit tests: Each exception handler method, isDevelopmentMode() logic
- [ ] Integration tests: Handler precedence (domain-specific vs global), actual REST endpoint error responses
- [ ] Manual testing: Trigger unhandled exceptions in dev environment, verify structured error response
- [ ] Regression testing: Verify existing domain exception handlers (auth, payments, AI) still work correctly

**Validation Checklist**:

- [ ] All existing tests pass (`./gradlew :backend:test`)
- [ ] New tests added and passing (GlobalExceptionHandlerTest)
- [ ] Build succeeds (`./gradlew build`)
- [ ] Integration test confirms domain handlers take precedence
- [ ] Manual test: Trigger `IllegalStateException` in dev mode, verify response contains details
- [ ] Manual test: Trigger `IllegalStateException` in prod mode, verify response does NOT contain stack trace
- [ ] Manual test: Existing auth error (e.g., invalid credentials) still returns `AuthExceptionHandler` response format
- [ ] Code review: Verify `Environment` injection, not `System.getProperty`

## Dependencies & Sequencing

**Must Complete Before This**: None - Can be implemented immediately

**Should Complete After This**:
- [ ] Optional: Refactor controllers to remove explicit error handling boilerplate (e.g., replace `?: return ResponseEntity.badRequest().build()` with throwing exceptions)
- [ ] Optional: Integrate centralized error monitoring tool (Sentry, DataDog) that consumes logs from GlobalExceptionHandler

**Can Run in Parallel With**:
- [ ] "Extract Shared Tutor Orchestration Logic" (maintainability/extract-tutor-orchestration-duplication.md)
- [ ] "Decompose ChatService God Class" (architecture/decompose-chatservice-god-class.md)
- [ ] Any other refactoring work

**Foundation Refactoring**: No - This is a targeted improvement that establishes hygiene for error handling but does not enable other large-scale refactorings

## Related Files

**Files to Modify**:

- None initially - Global handler is additive, no modifications to existing code required

**Files with Dependencies** (won't modify but are affected):

- All controllers in `*/controller/` packages - Will benefit from centralized error handling
- All service classes throwing unhandled exceptions - Errors now consistently formatted
- Frontend API clients - Will receive structured error responses instead of empty bodies

**Documentation to Update**:

- `backend/CLAUDE.md` - Add section on global exception handling in "Core Components" or "Architecture"
- `README.md` - If API error response format is documented, update examples
- Release notes - Document breaking change: "Controllers previously returning empty error responses now return structured JSON error bodies. Clients should parse response bodies for error details."

## Historical Context

**Why This Issue Exists**:
The project implemented domain-specific exception handlers (auth, payments, AI, rate limiting) following Spring's `@RestControllerAdvice` pattern, which is correct. However, the global fallback handler for uncaught system exceptions was never added. This is likely because:

1. Early development focused on happy-path functionality
2. Domain-specific handlers were added reactively as specific error types emerged (auth failures, payment errors)
3. Generic exceptions (IllegalStateException, UnsupportedOperationException) were treated as "shouldn't happen" cases and not prioritized
4. Stack trace leakage risk may not have been identified during security reviews

This is a common pattern in evolving REST APIs - domain-specific error handling is implemented first, and global fallback handling is added later as production debugging and security concerns surface.

**Previous Attempts**:
No previous attempts to implement a global exception handler. The existing domain-specific handlers suggest the team understands the `@RestControllerAdvice` pattern; this refactoring extends that approach to cover all uncaught exceptions.