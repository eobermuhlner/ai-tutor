---
description: Create or validate a language course lesson following pedagogical standards
---

# Course Lesson Writing Assistant

Help the user write or validate a language course lesson following the comprehensive guidelines in **backend/CLAUDE_COURSE.md**.

## Quick Reference

### Three Course Types

**Conversational Courses** (60% vocab, 40% grammar)
- Target: 150-180 lines
- Grammar: 2-3 core points (40-60 lines)
- Vocabulary: 30-60 items by semantic categories
- Scenarios: 2-3 AI-friendly descriptions
- Practice: 4-6 production-focused activities
- Common mistakes: 5-10
- Cultural notes: 3-6

**Grammar Courses** (70% grammar, 30% vocab)
- Target: 150-200 lines
- Grammar: 4-6 points (80-120 lines) with paradigm tables
- Vocabulary: 15-30 grammar-supporting items
- Scenarios: 1-2 grammar-focused descriptions
- Practice: 4-8 form-focused drills
- Common mistakes: 5-12
- Cultural notes: 3-5

**Travel Courses** (75% vocab, 25% minimal grammar)
- Target: 120-160 lines
- Grammar: 1-2 survival-critical points (20-40 lines)
- Vocabulary: 50-80 task-organized items
- Scenarios: 3-4 real-world task descriptions
- Practice: 3-5 task completion activities
- Common mistakes: 3-8
- Cultural notes: 6-10 (expanded for survival)

### Required Lesson Structure (8 Sections)

1. **YAML Frontmatter** - lessonId, title (target language), weekNumber, estimatedDuration, focusAreas (2-4), targetCEFR
2. **This Week's Goals** - 3-5 learner-focused bullet points
3. **Grammar Focus** - Rules with examples (varies by course type)
4. **Vocabulary** - Bold target language, organized by categories
5. **Conversation Scenarios** - AI-friendly descriptions (NOT scripted dialogues)
6. **Practice Patterns** - Specific, actionable production activities
7. **Common Mistakes to Watch** - Wrong → Correct (explanation)
8. **Cultural Notes** - Social norms, regional variations, usage tips

### Critical Rules

- **Grammar accuracy**: All explanations must be linguistically correct
- **NO dialogue scripts**: Use AI-friendly scenario descriptions only
- **Vocabulary precision**: Double-check all translations
- **AI-first design**: The tutor conducts conversations; lessons provide guidance
- **Standard quotes**: Use ASCII quotes/apostrophes (not curly quotes)

### Validation Checklist

Before finalizing any lesson:
- [ ] All 8 sections present in correct order
- [ ] YAML frontmatter complete and valid
- [ ] lessonId matches pattern `week-NN-topic`
- [ ] Line count within range for course type
- [ ] Grammar explanations linguistically accurate
- [ ] Vocabulary translations verified
- [ ] NO scripted dialogues (only scenario descriptions)
- [ ] Common mistakes use "wrong → correct (explanation)" format
- [ ] Added to curriculum.yml with correct id and file path

## Workflow

1. **Ask the user**:
   - Which course type? (Conversational/Grammar/Travel)
   - Target language and week number?
   - Topic for this lesson?
   - Is this a new lesson or validation of existing?

2. **If creating new lesson**:
   - Generate YAML frontmatter
   - Create all 8 sections following course type guidelines
   - Ensure line count matches course type requirements
   - Reference backend/CLAUDE_COURSE.md for detailed requirements

3. **If validating existing lesson**:
   - Read the lesson file
   - Check against validation checklist
   - Verify course type requirements
   - Suggest improvements for any issues

4. **Before finalizing**:
   - Run through complete validation checklist
   - Verify curriculum.yml entry
   - Confirm file location: `backend/src/main/resources/course-content/{course-id}/week-{NN}-{topic}.md`

## Reference

Full guidelines: **backend/CLAUDE_COURSE.md**
- Course directory structure
- Pedagogical research foundations
- Detailed examples for each course type
- Complete checklists
- Reference lesson examples
