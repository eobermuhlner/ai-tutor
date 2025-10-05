---
description: Validate that implementation matches task specification
argument-hint: task_file=tasks/task-XXXX-feature-name.md
---

Validate implementation against specification: $1

## Validation Checklist

### 1. Database Schema
- [ ] All tables created
- [ ] All columns present with correct types
- [ ] Indexes created
- [ ] Foreign keys present
- [ ] Migration ran successfully

Verification: Query database schema and compare to spec

### 2. Domain Entities
- [ ] All entity classes exist
- [ ] All fields present with correct annotations
- [ ] Relationships correctly mapped

Verification: Glob for entity files, read and compare

### 3. Repositories
- [ ] All repository methods implemented
- [ ] Query methods have correct signatures
- [ ] Custom queries annotated correctly

Verification: Read repository files, check method signatures

### 4. Services
- [ ] All service methods implemented
- [ ] Business logic matches spec
- [ ] Error handling present
- [ ] Logging statements present

Verification: Read service files, check against spec

### 5. Controllers
- [ ] All endpoints implemented
- [ ] HTTP methods correct (GET/POST/etc)
- [ ] Path variables correct
- [ ] Authorization checks present
- [ ] DTOs match spec

Verification: Read controller files, check against endpoint table

### 6. Configuration
- [ ] All config properties present
- [ ] Default values set
- [ ] Property names match code

Verification: Read application.yml, grep for @Value annotations

### 7. Tests
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Tests passing
- [ ] Coverage adequate (>70%)

Verification: Run ./gradlew test, check output

### 8. Documentation
- [ ] README updated (if needed)
- [ ] CLAUDE.md updated (if needed)
- [ ] HTTP tests added

Verification: Check if files modified

## Output Format

**Overall Status**: ✅ PASS / ⚠️ PARTIAL / ❌ FAIL

**Completed**:
- [List what's implemented correctly]

**Missing**:
- [List what's not implemented]

**Incorrect**:
- [List what's implemented but doesn't match spec]

**Next Steps**:
1. [What needs to be done to achieve PASS status]
2. [...]