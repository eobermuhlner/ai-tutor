# Refactoring Proposals Overview

**Last Updated**: 2025-11-27
**Total Proposals**: 3
**Completed**: 0
**In Progress**: 0
**Pending**: 2
**Deferred**: 1

## Quick Stats by Category

- **Architecture**: 1 proposal
- **Performance**: 0 proposals
- **Maintainability**: 2 proposals (1 deferred)
- **Testing**: 0 proposals
- **Security**: 0 proposals

## Priority Summary

### High Priority (2 proposals)

| Title | Category | Effort | Risk | Status | Proposed | Updated | File |
|-------|----------|--------|------|--------|----------|---------|------|
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

### Maintainability (2 proposals: 1 active, 1 deferred)

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

**Phase 1: Quick Wins - Code Quality** (High value, low risk, no dependencies)
1. **Extract Shared Tutor Orchestration Logic** (2-4 hours) - Eliminates 170+ duplicated lines, prevents bugs, delivers 70% of full decomposition value with minimal risk

**Phase 2: Alternative Quick Win - Error Handling** (Optional, evidence-based)
2. **Fix Empty Response Bodies** (1-2 hours) - Replace 5 instances of `ResponseEntity.badRequest().build()` with custom `BadRequestException` for structured error responses (80% of deferred global handler value with 20% of effort)

**Phase 3: Evaluate Need for Full Decomposition** (After monitoring for 1 month)
- Monitor merge conflicts, bugs, and developer complaints in ChatService
- If pain points emerge: Proceed with full **Decompose ChatService God Class** refactoring
- If no pain: Accept current structure and reinvest time in user-facing features

**Phase 4: Enabled by Full Decomposition** (If Phase 3 is implemented)
- ChatController decomposition (follows service boundaries)
- Interface extraction for services
- Testing improvements (reduced mocking complexity)

### Notes on Sequencing

- **Evidence-Based Approach**: Tutor orchestration extraction delivers proven value now; global exception handler deferred until production evidence justifies complexity
- **Quick Win Strategy**: Focus on tutor orchestration extraction as primary quick win (2-4 hours, proven ROI)
- **Alternative Path**: If error handling is priority, fix 5 empty responses directly (1-2 hours) instead of full global handler
- **Foundation Refactoring**: If full decomposition happens, it enables 3 other high-value improvements
- **Parallel Development**: Tutor orchestration extraction can run in parallel with any other work (low coordination overhead)

### Lessons Learned from Deferred Proposal

The **Global Exception Handler** proposal demonstrates importance of re-validation:

- **Measure before fixing**: No production evidence of problems (zero git commits about errors)
- **Prefer targeted fixes**: Fix 5 empty responses directly (1-2 hours) vs building infrastructure (6-10 hours)
- **Avoid premature optimization**: 4 existing domain handlers + 11 custom exceptions already handle most cases
- **Challenge assumptions**: "45+ uncaught exceptions" sounds alarming but most are intentional fail-fast initialization code

**Re-evaluation criteria ensure future decisions are evidence-based, not assumption-based.**
