---
description: Create a git commit following project commit guidelines
---

Create a git commit following these MANDATORY rules:

## Commit Message Format (REQUIRED)

**First line**: Concise summary in imperative mood, no period
**Body** (optional): Brief explanation of what and why (one sentence per line)

⚠️ **ABSOLUTELY NO AI ATTRIBUTION** ⚠️
- ❌ NEVER include "Generated with Claude Code"
- ❌ NEVER include "Co-Authored-By: Claude"
- ❌ NEVER include ANY AI attribution or references

## Examples

✅ **CORRECT:**
```
Add effectivePhase to separate user preference from active phase

User-controlled conversationPhase (Auto/Free/Correction/Drill) now separate from effectivePhase (actual active phase).
LLM suggestions only update effectivePhase when in Auto mode, never override manual user choices.
```

❌ **WRONG:**
```
Add effectivePhase to separate user preference from active phase

User-controlled conversationPhase now separate from effectivePhase.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Pre-Commit Checklist

Before committing, verify:
1. ✅ No "Generated with" or "Co-Authored-By" lines
2. ✅ Imperative mood ("Add" not "Added", "Fix" not "Fixed")
3. ✅ No period at end of first line
4. ✅ Blank line between subject and body (if body exists)
5. ✅ All tests pass: `./gradlew test`
6. ✅ Build succeeds: `./gradlew build`

## Commit Process

1. Review changes: `git status` and `git diff`
2. Stage files: `git add <files>`
3. Create commit with proper message format
4. NO push unless explicitly requested by user