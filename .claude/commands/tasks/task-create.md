---
description: Create a structured task documentation file with comprehensive design analysis
argument-hint: task_number=XXXX title="feature-name" problem_description="..."
---

You are creating a task documentation file for task $1: $2.

Problem: $3

Follow this structured approach:

## Phase 1: Analysis (DO THIS FIRST)
1. Understand the current codebase related to this problem
2. Read relevant files (use Glob/Grep to find them)
3. Identify constraints (existing architecture, dependencies)
4. List what needs to change vs what should stay unchanged

## Phase 2: Solution Design
1. Propose 3-5 alternative solutions
2. For each alternative:
   - Describe the approach
   - List pros and cons
   - Estimate complexity (LOC, files changed)
   - Identify risks
3. Recommend ONE solution with clear rationale

## Phase 3: Critical Review (BEFORE writing detailed plan)
1. Self-review for common mistakes:
   - Missing imports/dependencies
   - Non-existent methods called
   - Synchronous blocking in request path
   - Configuration conflicts
   - Database migration issues
2. Check for breaking changes to existing features
3. Validate consistency (method signatures, return types)

## Phase 4: Implementation Plan
Write comprehensive implementation plan with:
- Database schema changes
- New/modified files
- Configuration updates
- Testing strategy
- Rollout phases
- Monitoring plan
- Rollback strategy

## Phase 5: REST API Design (if applicable)
- Endpoints table
- DTOs
- Access control
- Example requests/responses

Save to: tasks/task-$1-$2.md

IMPORTANT: Complete Phase 1-3 BEFORE writing Phase 4. Present alternatives and get user approval after Phase 3.

Use TodoWrite to track your progress through phases.
