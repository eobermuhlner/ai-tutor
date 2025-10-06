---
description: Execute all task implementation phases with automatic commits
argument-hint: task_file=tasks/task-XXXX-feature-name.md
---

Execute ALL implementation phases for task: $1

## Batch Execution Protocol

### Before Starting
1. Read the task file completely
2. Identify all phases listed in the implementation plan
3. Check git status (must be clean or only untracked files)
4. Determine current phase (where to start)
5. Show user the full execution plan:
   - List of phases to execute
   - Expected file changes per phase
   - Commit strategy
6. **Wait for user approval before proceeding**

### For Each Phase
1. Execute phase following task-execute-phase protocol:
   - Create TODO list with all steps using TodoWrite
   - Mark each step as in_progress → completed
   - After each file change, verify: `./gradlew build`
   - Fix test failures immediately
2. Run full validation:
   - `./gradlew test` - all tests must pass
   - `./gradlew build` - build must succeed
3. Create git commit:
   - Commit message: "task-$TASK_NUMBER phase-$PHASE_NAME"
   - Body: Brief summary of what was implemented
   - **NO AI attribution** (follow Git Commit Guidelines in CLAUDE.md)
4. Update task file: "Status: Phase N completed ✅"
5. Continue to next phase automatically (no user prompt)

### After All Phases Complete
1. Run final validation: `./gradlew test && ./gradlew build`
2. Show summary:
   - List of commits created
   - Files changed
   - Test results
3. Update task file: "Status: ALL PHASES COMPLETED ✅"
4. Remind user to:
   - Review commits: `git log -n <number_of_phases> --oneline`
   - Update documentation (README.md, CLAUDE.md)
   - Add HTTP tests if REST endpoints added

### Error Handling
- **If any phase fails**: Stop immediately, show error, ask user for guidance
- **If compilation fails**: Fix immediately, don't continue to next phase
- **If tests fail**: Fix immediately, don't continue to next phase
- **If git commit fails**: Show error, ask user to resolve, then continue
- **If breaking change detected**: Warn user and get approval before continuing

### Commit Guidelines (CRITICAL)
Follow the project's Git Commit Guidelines exactly:
- ✅ First line: Concise summary (imperative mood, no period)
- ✅ Body (optional): Brief explanation
- ❌ **NEVER include "Generated with Claude Code" or "Co-Authored-By: Claude"**
- ✅ Blank line between subject and body

**CRITICAL**:
- Use TodoWrite tool to track all phases and steps
- Never skip phases
- Always commit between phases
- Stop on first error