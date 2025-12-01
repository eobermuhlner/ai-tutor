# Refactoring Proposals Overview

**Last Updated**: 2025-12-01
**Total Proposals**: 5
**Completed**: 1
**In Progress**: 0
**Pending**: 3
**Deferred**: 1

## Quick Stats by Category

- **Architecture**: 1 proposal
- **Performance**: 0 proposals
- **Maintainability**: 4 proposals (1 completed, 1 deferred, 2 pending)
- **Testing**: 0 proposals
- **Security**: 0 proposals

## Priority Summary

### Completed (1 proposal)

| Title | Category | Effort | Actual Time | Status | Completed | File |
|-------|----------|--------|-------------|--------|-----------|------|
| Fix Empty Response Bodies with Custom Exceptions | maintainability | Small (1-2 hours) | 3 hours | ✅ Completed | 2025-11-27 | [fix-empty-response-bodies.md](maintainability/fix-empty-response-bodies.md) |

### High Priority (3 proposals)

| Title | Category | Effort | Risk | Status | Proposed | Updated | File |
|-------|----------|--------|------|--------|----------|---------|------|
| Extract Duplicate toUserResponse() Mapping Logic to Shared Mapper Service | maintainability | Small (1-2 hours) | Low | Proposed | 2025-12-01 | N/A | [extract-user-response-mapper.md](maintainability/extract-user-response-mapper.md) |
| Extract Shared Tutor Orchestration Logic to Eliminate Code Duplication | maintainability | Small (2-4 hours) | Low | Proposed | 2025-11-26 | N/A | [extract-tutor-orchestration-duplication.md](maintainability/extract-tutor-orchestration-duplication.md) |
| Decompose ChatService God Class into Domain-Focused Services | architecture | Large (4-6 days) | Medium | Proposed | 2025-11-26 | N/A | [decompose-chatservice-god-class.md](architecture/decompose-chatservice-god-class.md) |

### Medium Priority (0 proposals)

_No medium priority proposals at this time._

### Low Priority (0 proposals)

_No low priority proposals at this time._

### Deferred (1 proposal)

| Title | Category | Original Effort | Deferred Date | File |
|-------|----------|----------------|---------------|------|
| Add Global Exception Handler for Unhandled System Exceptions | maintainability | Small (2-4 hours) → Medium (6-10 hours) | 2025-11-27 | [add-global-exception-handler.md](maintainability/add-global-exception-handler.md) |

---

## Detailed Proposals by Category

### Maintainability (4 proposals: 3 active, 1 deferred)

#### Extract Duplicate toUserResponse() Mapping Logic to Shared Mapper Service

**File**: `refactorings/maintainability/extract-user-response-mapper.md`
**Priority**: High
**Effort**: Small (1-2 hours)
**Risk**: Low
**Status**: Proposed
**Proposed Date**: 2025-12-01
**Updated Date**: N/A

**Summary**: Extract identical 18-line UserEntity -> UserResponse mapping logic duplicated across 4 locations (AuthService, AdminController, AuthController x2) to a centralized UserMapper service. Eliminates 76 lines of duplication, reduces schema evolution burden from 4 locations to 1, and establishes reusable mapping pattern for 5+ other entities.

**Key Benefits**:

- Eliminates 76 lines of code duplication (DRY violation)
- Single source of truth prevents inconsistent API responses
- Future UserEntity schema changes require updating only 1 location instead of 4
- Foundation refactoring that establishes mapping service pattern for entire codebase

**Affected Areas**:
- `AuthService.kt` (lines 451-469: remove toUserResponse method)
- `AdminController.kt` (lines 322-340: remove toUserResponse method)
- `AuthController.kt` (lines 72-91, 113-131: replace inline mappings)
- NEW: `user/service/UserMapper.kt` (create centralized mapper service)

**Assessment**: ⭐️ **Recommended** by improvement-value-assessor agent. Textbook justified refactoring with immediate ROI. Low risk (no API contract changes), comprehensive test coverage exists. Should be first or second in queue based on value/effort ratio.

---

#### Fix Empty Response Bodies with Custom Exceptions

**File**: `refactorings/maintainability/fix-empty-response-bodies.md`
**Priority**: High
**Effort**: Small (1-2 hours)
**Risk**: Low
**Status**: Proposed
**Proposed Date**: 2025-11-27
**Updated Date**: N/A

**Summary**: Create two simple custom exceptions (`ResourceNotFoundException`, `BadRequestException`) following the existing `AuthExceptions.kt` pattern. Replace 10 instances of empty `ResponseEntity.badRequest().build()` and `ResponseEntity.notFound().build()` calls across 3 controllers (ChatController, LessonController, VocabularyController) with throw statements that provide meaningful error messages. Existing `AuthExceptionHandler` will catch these exceptions and return structured `ErrorResponse` JSON automatically.

**Key Benefits**:

- Eliminates 10 empty response bodies (REST API standards violation)
- Provides structured JSON error responses for all affected endpoints
- Improves API debugging (error messages include context like course IDs, session IDs)
- Delivers 80% of deferred global handler value with 20% of effort

**Affected Areas**:
- `ChatController.kt` (2 instances: lines 71, 83)
- `LessonController.kt` (7 instances: lines 36, 62, 76, 85, 89, 103, 112, 116)
- `VocabularyController.kt` (1 instance: line 55)
- `AuthExceptionHandler.kt` (add 2 new handlers) or new `CoreExceptionHandler.kt`

**Relationship**: High-value alternative to deferred "Add Global Exception Handler" proposal. Addresses actual user-facing problem (empty responses) without infrastructure complexity.

---

#### Extract Shared Tutor Orchestration Logic to Eliminate Code Duplication

**File**: `refactorings/maintainability/extract-tutor-orchestration-duplication.md`
**Priority**: High
**Effort**: Small (2-4 hours)
**Risk**: Low
**Status**: Proposed
**Proposed Date**: 2025-11-26
**Updated Date**: N/A

**Summary**: Extract 170+ duplicated lines of orchestration logic from `sendMessage()` and `initiateTutorMessage()` methods into a single `orchestrateTutorResponse()` method with helper functions. This is a surgical refactoring that eliminates 85% code duplication, prevents bugs from divergent behavior (historical issue: vocabulary review mode bug), and delivers the main value of the larger ChatService decomposition in 2-4 hours instead of 6-10 days.

**Key Benefits**:

- Eliminates 170+ duplicated lines (85% similarity between sendMessage and initiateTutorMessage)
- Single source of truth for tutor orchestration prevents divergent behavior bugs
- Low-risk internal refactoring with comprehensive test coverage
- Foundation for future decomposition if needed (but delivers value now)

**Affected Areas**:
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` (lines 325-727)
- Existing ChatServiceTest validates behavior (no test changes needed)

---

#### Add Global Exception Handler for Unhandled System Exceptions (DEFERRED)

**File**: `refactorings/maintainability/add-global-exception-handler.md`
**Priority**: Deferred
**Status**: Deferred
**Deferred Date**: 2025-11-27
**Original Effort**: Small (2-4 hours)
**Revised Effort**: Medium (6-10 hours)

**Deferral Reason**: Re-validation found no production evidence of problems (zero git commits about 500 errors or stack leaks), effort underestimated (6-10 hours vs claimed 2-4), and better alternative exists (fix 5 empty responses with custom exceptions for 80% of value in 20% of effort). Deferred until production monitoring shows actual need.

**Re-evaluation Criteria**:
- Production monitoring shows frequent unhandled exceptions
- User complaints about poor error messages
- Frontend team requests structured error responses
- Security audit flags stack trace leakage

**Recommended Alternative**: Fix 5 empty `ResponseEntity.badRequest().build()` calls with custom `BadRequestException` (1-2 hours)

---

### Architecture (1 proposal)

#### Decompose ChatService God Class into Domain-Focused Services

**File**: `refactorings/proposals/architecture/decompose-chatservice-god-class.md`
**Priority**: High
**Effort**: Large (4-6 days realistically)
**Risk**: Medium
**Status**: Proposed
**Proposed Date**: 2025-11-26
**Updated Date**: N/A

**Summary**: Refactor the 1,033-line ChatService God Class into 4 domain-focused services (SessionManagementService, MessageOrchestrationService, SessionConfigurationService, SessionProgressService) to eliminate SOLID violations, reduce 14+ service dependencies to 3-6 per service, and remove 350+ duplicated lines between sendMessage and initiateTutorMessage.

**Key Benefits**:

- Eliminates 350+ duplicated lines (85% similarity between sendMessage and initiateTutorMessage)
- Reduces test complexity by 70% (3-6 dependencies per service vs 14+ for God Service)
- Establishes foundation pattern for decomposing other God Services (TutorService, UnifiedCatalogImportService, AuthService)
- Enables parallel development (multiple developers can work on SessionConfiguration vs MessageOrchestration simultaneously)

**Affected Areas**:
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/service/ChatService.kt` (1,033 lines)
- `backend/src/main/kotlin/ch/obermuhlner/aitutor/chat/controller/ChatController.kt` (540 lines)
- 14 service dependencies remain unchanged
- Test suite split into 4 focused test files

---

## Implementation Recommendations

### Recommended Implementation Order

**Phase 1: Quick Wins - Highest ROI Improvements** (Prioritized by value/effort ratio)

1. **Extract Duplicate toUserResponse() Mapping Logic** (1-2 hours) - **NEW HIGHEST PRIORITY** - Eliminates 76 lines of duplication across 4 locations, establishes mapping service pattern, zero dependencies, immediate schema evolution benefits
2. **Fix Empty Response Bodies** (1-2 hours) - Eliminates 10 REST API violations, provides structured errors, 80% value of deferred global handler with 20% effort
3. **Extract Shared Tutor Orchestration Logic** (2-4 hours) - Eliminates 170+ duplicated lines, prevents bugs, delivers 70% of full decomposition value with minimal risk

**Note**: All Phase 1 refactorings can run in parallel or sequentially based on availability. Total effort: 4-8 hours.

**Phase 2: Evaluate Need for Full Decomposition** (After monitoring for 1 month)
- Monitor merge conflicts, bugs, and developer complaints in ChatService
- If pain points emerge: Proceed with full **Decompose ChatService God Class** refactoring
- If no pain: Accept current structure and reinvest time in user-facing features

**Phase 3: Enabled by Full Decomposition** (If Phase 2 is implemented)
- ChatController decomposition (follows service boundaries)
- Interface extraction for services
- Testing improvements (reduced mocking complexity)

### Notes on Sequencing

- **Evidence-Based Approach**: Quick wins (user mapping, empty responses, tutor orchestration) deliver proven value now; global exception handler deferred until production evidence justifies complexity
- **Highest ROI First**: Extract UserMapper (1-2 hours) provides immediate maintenance burden reduction and establishes reusable pattern
- **Foundation Pattern**: UserMapper refactoring establishes mapping service pattern for 5+ other entities
- **Quick Win Strategy**: All Phase 1 refactorings (4-8 hours total) can be done in parallel or sequentially
- **Parallel Development**: All quick wins can run in parallel with any other work (low coordination overhead)

### Lessons Learned from Deferred Proposal

The **Global Exception Handler** proposal demonstrates importance of re-validation:

- **Measure before fixing**: No production evidence of problems (zero git commits about errors)
- **Prefer targeted fixes**: Fix 5 empty responses directly (1-2 hours) vs building infrastructure (6-10 hours)
- **Avoid premature optimization**: 4 existing domain handlers + 11 custom exceptions already handle most cases
- **Challenge assumptions**: "45+ uncaught exceptions" sounds alarming but most are intentional fail-fast initialization code

**Re-evaluation criteria ensure future decisions are evidence-based, not assumption-based.**
