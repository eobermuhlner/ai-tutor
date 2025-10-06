---
description: Execute task implementation phase with validation
argument-hint: task_file=tasks/task-XXXX-feature-name.md phase=next
---

Execute implementation phase for task: $1

Phase to execute: $2

## Execution Protocol

### Before Starting
1. Read the task file completely
2. Identify current implementation status (what's already done)
3. Determine next phase to execute
4. List prerequisites (is previous phase complete?)
5. Show user what WILL be done (file changes, commands)
6. **Wait for user approval before proceeding**

### During Execution
1. Create TODO list with all steps using TodoWrite
2. Mark each step as in_progress → completed
3. After each file change, verify compilation: `./gradlew build`
4. If tests fail, fix immediately before continuing
5. Log progress to user after each step

### After Completion
1. Run full test suite: `./gradlew test`
2. Verify no regressions: `./gradlew build`
3. Update task file with "Status: Phase N completed ✅"
4. Ask user if ready to proceed to next phase

### Error Handling
- If compilation fails: Fix immediately, don't continue
- If tests fail: Fix immediately, don't continue
- If stuck: Ask user for guidance
- If breaking change detected: Warn user and get approval

**CRITICAL**: Use TodoWrite tool to track all steps. Never skip steps.