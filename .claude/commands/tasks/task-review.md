---
description: Perform strict review of task documentation to find bugs and issues
argument-hint: task_file=tasks/task-XXXX-feature-name.md
---

Perform a STRICT code review of the task documentation: $1

Review checklist:

## 1. Code Correctness
- [ ] All method calls use methods that exist (check repository)
- [ ] All imports are present
- [ ] Method signatures match actual definitions
- [ ] No undefined variables or parameters
- [ ] Type consistency (UUID vs String, etc.)

## 2. Architecture Issues
- [ ] No synchronous blocking in request path
- [ ] Async operations properly configured
- [ ] Database transactions properly scoped
- [ ] No N+1 query issues

## 3. Configuration
- [ ] No duplicate YAML keys
- [ ] Property names follow conventions
- [ ] All config values have sensible defaults

## 4. Database
- [ ] Migration script syntax correct
- [ ] Indexes on frequently queried columns
- [ ] Foreign key constraints present
- [ ] Backfill queries safe for production

## 5. Breaking Changes
- [ ] Existing features preserved
- [ ] Backward compatibility maintained
- [ ] Feature flags for gradual rollout

## 6. Testing
- [ ] Unit tests cover edge cases
- [ ] Integration tests for happy path
- [ ] Load tests for performance-critical paths

## 7. Security
- [ ] Authorization checks on all endpoints
- [ ] Input validation present
- [ ] SQL injection prevention (parameterized queries)
- [ ] No secrets in code/config

## 8. Completeness
- [ ] Monitoring/logging strategy defined
- [ ] Rollback plan documented
- [ ] Rollout phases defined
- [ ] Documentation updates listed

Output format:
1. **Critical Issues** (will cause failures): List with line numbers
2. **Warnings** (potential problems): List with suggestions
3. **Recommendations** (improvements): List with rationale
4. **Overall Assessment**: APPROVE / APPROVE WITH CHANGES / REJECT

Be harsh. 
Find every bug. 
Check actual codebase to verify method existence.
Keep it simple.
Avoid overengineering.
