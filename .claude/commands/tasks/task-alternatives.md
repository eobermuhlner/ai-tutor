---
description: Generate and compare multiple alternative solutions for a problem
argument-hint: problem_description="..." count=4
---

Problem: $1

Generate $2 distinct alternative solutions.

For each alternative:

## Alternative N: [Descriptive Name]

**Concept**: One-sentence summary

**Approach**:
- Key design decision 1
- Key design decision 2
- Key design decision 3

**Pros**:
- ✅ Benefit 1 (with metric if applicable)
- ✅ Benefit 2

**Cons**:
- ❌ Drawback 1 (with impact assessment)
- ❌ Drawback 2

**Complexity**: [LOC estimate, files changed, external dependencies]

**Risks**: [Critical risks that could cause failure]

---

After listing all alternatives, provide:

## Recommendation Matrix

| Criteria | Alt 1 | Alt 2 | Alt 3 | Alt 4 | Winner |
|----------|-------|-------|-------|-------|--------|
| Complexity | 7/10 | 4/10 | ... | ... | Alt 2 |
| Maintainability | ... | ... | ... | ... | ... |
| Performance | ... | ... | ... | ... | ... |
| Scalability | ... | ... | ... | ... | ... |
| **Overall** | ... | ... | ... | ... | **Alt X** |

## Final Recommendation

**Selected**: Alternative X

**Rationale**: 2-3 sentences explaining why this is the best choice given the constraints.

**When NOT to use this**: 1-2 sentences on scenarios where another alternative would be better.
