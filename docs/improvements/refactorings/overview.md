# Refactoring Proposals Overview

**Last Updated**: 2025-11-26
**Total Proposals**: 2
**Completed**: 0
**In Progress**: 0
**Pending**: 2

## Quick Stats by Category

- **Architecture**: 1 proposal
- **Performance**: 0 proposals
- **Maintainability**: 1 proposal
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

---

## Detailed Proposals by Category

### Maintainability (1 proposal)

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

**Phase 1: Quick Wins** (High value, low risk, no dependencies)
1. **Extract Shared Tutor Orchestration Logic** (2-4 hours) - Eliminates 170+ duplicated lines, prevents bugs, delivers 70% of full decomposition value with minimal risk

**Phase 2: Evaluate Need for Full Decomposition** (After monitoring for 1 month)
- Monitor merge conflicts, bugs, and developer complaints in ChatService
- If pain points emerge: Proceed with full **Decompose ChatService God Class** refactoring
- If no pain: Accept current structure and reinvest time in user-facing features

**Phase 3: Enabled by Full Decomposition** (If Phase 2 is implemented)
- ChatController decomposition (follows service boundaries)
- Interface extraction for services
- Testing improvements (reduced mocking complexity)

### Notes on Sequencing

- **Quick Win Strategy**: The orchestration extraction is a pragmatic first step that delivers immediate value
- **Evidence-Based Decision**: Full decomposition should only proceed if metrics show actual pain (not theoretical SOLID concerns)
- **Foundation Refactoring**: If full decomposition happens, it enables 3 other high-value improvements
- **Parallel Development**: The small orchestration refactoring can run in parallel with any other work (low coordination overhead)
