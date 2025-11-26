# Refactoring Proposals Overview

**Last Updated**: 2025-11-26
**Total Proposals**: 1
**Completed**: 0
**In Progress**: 0
**Pending**: 1

## Quick Stats by Category

- **Architecture**: 1 proposal
- **Performance**: 0 proposals
- **Maintainability**: 0 proposals
- **Testing**: 0 proposals
- **Security**: 0 proposals

## Priority Summary

### High Priority (1 proposal)

| Title | Category | Effort | Risk | Status | Proposed | Updated | File |
|-------|----------|--------|------|--------|----------|---------|------|
| Decompose ChatService God Class into Domain-Focused Services | architecture | Large (4-6 days) | Medium | Proposed | 2025-11-26 | N/A | [decompose-chatservice-god-class.md](architecture/decompose-chatservice-god-class.md) |

### Medium Priority (0 proposals)

_No medium priority proposals at this time._

### Low Priority (0 proposals)

_No low priority proposals at this time._

---

## Detailed Proposals by Category

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

**Phase 1: Foundation Refactorings** (No dependencies)
1. **Decompose ChatService God Class** - Establishes service decomposition pattern and reduces technical debt in core conversation flow

**Phase 2: Enabled by Phase 1** (Can run in parallel after ChatService decomposition)
- ChatController decomposition (follows service boundaries)
- Interface extraction for services
- Testing improvements (reduced mocking complexity)

### Notes on Sequencing

- **Foundation Refactoring**: The ChatService decomposition is a foundation refactoring that enables 3 other high-value improvements
- **Parallel Development**: After Phase 1, multiple frontend and backend refactorings can proceed in parallel
- **Risk Mitigation**: Use phased implementation strategy (SessionProgress → SessionManagement → SessionConfiguration → MessageOrchestration) to minimize regression risk
