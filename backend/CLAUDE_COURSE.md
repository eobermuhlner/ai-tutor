# Course Lesson Writing Guidelines

## Overview
Course lessons are markdown files with YAML frontmatter located in `backend/src/main/resources/course-content/{course-id}/`. Each lesson follows a standardized structure to ensure pedagogical consistency and quality across all language courses.

## Directory Structure
```
backend/src/main/resources/course-content/
├── de-conversational-german/
│   ├── curriculum.yml              # Course structure and lesson sequence
│   ├── week-01-greetings.md        # Individual lesson files
│   ├── week-02-introductions.md
│   └── ...
├── es-conversational-spanish/
│   ├── curriculum.yml
│   └── ...
├── fr-conversational-french/
│   ├── curriculum.yml
│   └── ...
└── ja-conversational-japanese/
    ├── curriculum.yml
    └── ...
```

## File Naming Convention
- Pattern: `lesson-{NN}-{topic-slug}.md`
- Examples: `lesson-01-greetings.md`, `lesson-04-food-ordering.md`, `lesson-09-past-tense.md`
- Use lowercase, hyphen-separated topic names
- Lesson numbers are zero-padded (01, 02, ..., 10)

## Course Types and Pedagogical Approaches

Lessons are tailored to three distinct course types based on learning objectives and proven pedagogy:

### Conversational Courses
**Pedagogical foundation**: Communicative Language Teaching (CLT - Canale & Swain 1980), Output Hypothesis (Swain 1985)

**Content emphasis**: 60% vocabulary & scenarios, 40% grammar
- Balances form (grammar accuracy) and function (meaningful communication)
- Develops communicative competence through interaction
- Supports learner fluency with grammatical accuracy
- **Target CEFR**: A1-B2
- **Examples**: "Conversational German", "Conversational Spanish", "American English"

**Lesson characteristics**:
- Grammar sections: 40-60 lines with 2-3 core grammar points
- Vocabulary: 30-60 items organized by semantic categories
- Scenario count: 2-3 AI-friendly descriptions (not full dialogues)
- Practice focus: Production-oriented (meaningful communication)
- Size: 150-180 lines

**Rationale**: Research shows learners develop proficiency fastest when combining explicit grammar instruction with meaningful communicative practice (Long 1991).

---

### Grammar Courses
**Pedagogical foundation**: Focus on Form (Long 1991, Doughty & Williams 1998), Skill Acquisition Theory (DeKeyser 2007)

**Content emphasis**: 70% grammar, 30% supporting vocabulary
- Prioritizes explicit, systematic grammar instruction
- Develops metalinguistic awareness and grammatical accuracy
- Supports test preparation and formal learning contexts
- **Target CEFR**: A1-C2 (depends on curriculum sequence)
- **Examples**: "German Grammar Fundamentals", "Spanish Grammar Essentials", "Advanced French Grammar"

**Lesson characteristics**:
- Grammar sections: 80-120 lines with 4-6 grammar points
  - Include paradigm tables (conjugation tables, case declensions, gender charts)
  - Multiple example sentences per rule (minimum 5-7 examples)
  - Negative forms, questions, and irregular forms
  - Contrastive analysis ("use X here, use Y there")
- Vocabulary: 15-30 items (only high-frequency grammar-supporting words)
  - Include grammatical terminology in target language
  - Function words and particles
- Scenario count: 1-2 brief AI-friendly descriptions (grammar in context)
- Practice focus: Form-focused drills and controlled production
  - "Conjugate all pronouns in present tense"
  - "Transform sentences from present to past"
  - "Correct errors in these sentences"
- Size: 150-200 lines

**Rationale**: Explicit form-focused instruction helps learners notice grammatical patterns (Schmidt's Noticing Hypothesis 1990) and proceduralize knowledge (DeKeyser 2007). Most effective for learners with explicit learning preferences or test preparation goals.

---

### Travel Courses
**Pedagogical foundation**: Task-Based Language Teaching (Ellis 2003), Notional-Functional Syllabus (Wilkins 1976)

**Content emphasis**: 75% vocabulary & scenarios, 25% minimal grammar
- Prioritizes pragmatic competence and task completion
- Focuses on survival communication in authentic travel scenarios
- Emphasizes fluency and functional effectiveness over grammatical perfection
- **Target CEFR**: A1-A2 (survivalist level)
- **Examples**: "German for Travelers", "Spanish for Travelers", "Mandarin for Travelers"

**Lesson characteristics**:
- Grammar sections: 20-40 lines with 1-2 essential grammar points ONLY
  - Only include grammar necessary for immediate survival communication
  - Simple, practical explanations (no extensive paradigms)
  - Examples: question formation, basic word order, polite verb forms
- Vocabulary: 50-80 items (high-frequency phrases and chunks)
  - Organized by communicative function/task
  - Formulaic chunks: "Where is the...", "I would like...", "How much does it cost?", "Can you help me?"
  - Pronunciation guidance for critical phrases
- Scenario count: 3-4 task-based descriptions (real-world situations)
  - Focus on practical tasks: ordering food, asking directions, emergencies
  - Emphasize "successful communication" over accuracy
- Cultural Notes: EXPANDED (6-10 items covering pragmatic competence)
  - Local customs, politeness norms, non-verbal communication
  - What to do/avoid to show respect
- Practice focus: Task completion and functional communication
  - "Successfully order a meal at a restaurant"
  - "Navigate from hotel to train station"
  - "Handle emergency situations (lost passport, medical issue, police)"
- Size: 120-160 lines

**Rationale**: Travel learners have immediate, high-stakes communicative needs. Task-based approaches prioritize pragmatic success (communicating intent and understanding context) over grammatical accuracy (Ellis 2003). Formulaic chunks accelerate practical competence for urgent real-world situations.

---

## Lesson Structure
Every lesson must include these sections in order:

### 1. YAML Frontmatter (Required)
```yaml
---
lessonId: lesson-01-greetings
title: Grüße und erste Worte            # In target language
lessonNumber: 1
focusAreas:
  - Basic greetings                     # 2-4 focus areas
  - Polite expressions
  - Common questions
targetCEFR: A1                          # A1, A2, B1, B2, C1, or C2
---
```

### 2. Lesson Goals
- 3-5 bullet points describing learning objectives
- Use active, learner-focused language
- Example: "Greet people in formal and informal contexts"

### 3. Grammar Focus
- **CRITICAL**: All grammar explanations must be linguistically accurate
- Use clear rules with examples for each grammar point
- Use bold for rules: `**Rule:** Subject + verb + object`

**Conversational Courses:**
- Include 2-3 core grammar points per lesson
- Provide 3-5 examples per rule
- Include negative and question forms when relevant
- Show conjugation tables for high-frequency verbs
- Focus on communicative functionality

**Grammar Courses:**
- Include 4-6 grammar points per lesson with systematic depth
- Provide 5-7+ examples per rule (minimum)
- **Mandatory**: Include complete paradigm tables (conjugations, declensions, gender charts)
- **Mandatory**: Include negative forms, question forms, and irregular variations
- Add contrastive analysis: "Use X in situation Y, but Z in situation W"
- Include examples of common learner errors (bridge to Common Mistakes section)

**Travel Courses:**
- Include ONLY 1-2 essential grammar points (survival-critical)
- Focus on immediate communicative utility
- Provide 2-3 practical examples per rule
- Minimize paradigms and metalinguistic explanations
- Example: "How to ask questions" (not "subjunctive mood")

Example structure (Conversational):
```markdown
### Present Tense Regular Verbs

**Rule:** Regular verbs follow this pattern: stem + ending

Conjugation of hablar (to speak):
- Yo hablo (I speak)
- Tú hablas (You speak)
...

Examples:
- Yo hablo español (I speak Spanish)
- ¿Hablas inglés? (Do you speak English?)
```

Example structure (Grammar Course - expanded):
```markdown
### Present Tense Regular Verbs

**Rule:** German weak verbs add endings to the stem. The conjugation depends on person and number.

Complete Conjugation Table:
| Person | Singular | Plural |
|--------|----------|--------|
| 1st | ich mache | wir machen |
| 2nd (formal) | Sie machen | Sie machen |
...

**Negative form:** Ich mache das nicht.
**Question form:** Machst du das?

**Irregular variations:**
- haben (to have): ich habe, du hast, er hat
- sein (to be): ich bin, du bist, er ist

Examples of errors:
- "Ich make das" → "Ich mache das" (wrong stem form)
```

Example structure (Travel Course - minimal):
```markdown
### Asking Questions

**Rule:** To ask questions, use Wo (where), Wann (when), Wie viel (how much)

Examples:
- "Wo ist der Bahnhof?" (Where is the train station?)
- "Wie viel kostet das?" (How much does it cost?)
- "Wann kommt der Bus?" (When does the bus come?)
```

### 4. Vocabulary
- Use bold for target language: `**Hola** - Hello`
- Organize by semantic categories (e.g., "Greetings", "Time Expressions", "Food Items")
- **CRITICAL**: All translations must be accurate
- Include common phrases and collocations

**Conversational Courses:**
- 30-60 vocabulary items total
- Balance nouns, verbs, adjectives, and phrases
- Include practical, high-frequency words for communication

**Grammar Courses:**
- 15-30 vocabulary items total (MINIMAL)
- Focus only on words needed to demonstrate grammar points
- Include grammatical function words (particles, prepositions, conjunctions)
- Include grammatical terminology in target language

**Travel Courses:**
- 50-80 vocabulary items total (MAXIMUM)
- Organize by communicative function/task (Ordering, Directions, Emergencies, etc.)
- Prioritize formulaic chunks: "Where is...", "I would like...", "How much...", "Can you..."
- Include pronunciation notes for critical phrases
- Focus on high-frequency survival vocabulary

Example (Conversational):
```markdown
### Greetings
- **Hola** - Hello
- **Buenos días** - Good morning
- **Buenas tardes** - Good afternoon
```

Example (Grammar Course - minimal):
```markdown
### Supporting Vocabulary
- **der/die/das** - the (definite articles)
- **ein/eine** - a/an (indefinite articles)
- **und** - and
- **aber** - but
- **oder** - or
```

Example (Travel Course - task-organized):
```markdown
### Ordering Food
- **Ich möchte...** - I would like...
- **Das kostet...** - That costs...
- **Die Rechnung, bitte** - The bill, please
- **Vegetarisch** - Vegetarian
- **Allergisch gegen...** - Allergic to...

### Asking Directions
- **Wo ist...?** - Where is...?
- **Nächste Haltestelle** - Next stop
- **Geradeaus** - Straight ahead
- **Links/Rechts** - Left/Right
```

### 5. Conversation Scenarios
- **CRITICAL**: Do NOT write full scripted dialogues
- **AI-FIRST DESIGN**: The AI tutor will conduct actual conversations; provide guidance, not scripts
- Provide brief scenario descriptions (2-5 sentences depending on course type)
- Describe situations the AI will help learners navigate
- Include sample phrases and key vocabulary to practice

**Conversational Courses:**
- 2-3 scenarios per lesson
- Focus on communicative goals and meaningful interaction
- Provide 3-5 key phrases per scenario
- Examples: "Greeting a business contact", "Ordering at a restaurant", "Making small talk"

**Grammar Courses:**
- 1-2 scenarios per lesson (minimal)
- Focus on opportunities to use the grammar points being studied
- Provide 2-4 key phrases demonstrating the target grammar
- Examples: "Describe your family using possessive forms", "Narrate a past event using past tense"

**Travel Courses:**
- 3-4 scenarios per lesson (MAXIMUM)
- Focus on real-world survival tasks
- Provide 4-6 essential phrases per scenario
- Prioritize emergency/high-stakes situations
- Examples: "Ordering a meal", "Asking directions", "Medical emergency", "Lost baggage"

**CORRECT Approach (Conversational):**
```markdown
### Ordering at a Café

Practice ordering food and drinks in a café setting. The tutor will play the role of a café staff member.

Topics to cover:
- "Ich möchte einen Kaffee, bitte" (I'd like a coffee, please)
- "Was kostet das?" (How much does that cost?)
- "Die Rechnung, bitte" (The bill, please)

Tutor will prompt: "What beverage would you like?"
```

**CORRECT Approach (Grammar Course):**
```markdown
### Using Possessive Forms

Practice describing your family and personal belongings using German possessive adjectives (mein, dein, sein, ihr, etc.).

Key grammar to practice:
- "Das ist mein Bruder" (That's my brother)
- "Wer ist deine Schwester?" (Who is your sister?)
- "Das ist sein Haus" (That's his house)
```

**CORRECT Approach (Travel Course):**
```markdown
### Ordering at a Restaurant

Order a meal, ask about ingredients/allergies, and request the bill. Practice real-world ordering scenarios.

Key phrases:
- "Ich möchte..." (I would like...)
- "Vegetarisch" / "Allergisch gegen..." (Vegetarian / Allergic to...)
- "Die Rechnung, bitte" (The bill, please)
- "Kreditkarte" / "Bargeld" (Credit card / Cash)
```

**WRONG Approach (too scripted - avoid this for ALL course types):**
```markdown
### Ordering at a Café

Waiter: Guten Tag! Was möchten Sie?
Student: Ich möchte einen Kaffee.
Waiter: Gerne. Mit Milch?
Student: Ja, mit Milch, bitte.
...
```

### 6. Practice Patterns
- Include specific, actionable practice activities
- Focus on production (output), not passive comprehension

**Conversational Courses:**
- 4-6 activities total
- Mix comprehension checks and production practice
- Examples: "Ask your tutor about their weekend", "Describe your daily routine", "Ask for clarification politely"

**Grammar Courses:**
- 4-8 activities total (MAXIMUM)
- Focus on controlled form production and drills
- Examples: "Conjugate all pronouns in present tense", "Transform sentences from present to past", "Identify which case to use in these sentences", "Correct common errors"

**Travel Courses:**
- 3-5 activities total
- Focus on task completion and functional communication
- Examples: "Successfully order a meal avoiding dietary restrictions", "Navigate from hotel to train station", "Handle a medical emergency", "Negotiate a price at a market"

### 7. Common Mistakes to Watch
- Show incorrect → correct with explanation
- Format: `Wrong form → Correct form (explanation)`

**Conversational Courses:**
- 5-10 common errors per lesson
- Focus on mistakes that impede communication or sound unnatural
- Include pronunciation errors that change meaning
- Example: `"Yo soy hambre" → "Yo tengo hambre" (use tener, not ser for hunger!)`

**Grammar Courses:**
- 5-12 common errors per lesson (MAXIMUM)
- Focus on grammatical accuracy issues
- Include paradigm errors (wrong conjugation, declension, gender agreement)
- Include errors that demonstrate the grammar point being taught
- Format systematically to bridge grammar concepts
- Example: `"Der Frau" → "Die Frau" (neuter article error, die is feminine!)`

**Travel Courses:**
- 3-8 common errors per lesson
- Focus on comprehension breakdowns, not minor accuracy issues
- Include errors that would prevent the traveler from achieving their goal
- Prioritize misunderstandings and safety-critical mistakes
- Example: `"Ich bin allergisch Milch" → "Ich bin allergisch gegen Milch" (wrong preposition, could cause allergic reaction!)`

### 8. Cultural Notes
- Include social norms, regional variations, usage tips
- Connect language to real-world situations

**Conversational Courses:**
- 3-6 bullet points about cultural context
- Focus on communication norms and social conventions
- Include regional variations in usage
- Example: "In Spain, lunch is the main meal and typically eaten 2-3pm"

**Grammar Courses:**
- 3-5 bullet points about cultural/linguistic context
- Focus on how grammar reflects cultural values and communication patterns
- Example: "German capitalization of nouns reflects precision and formality in language"

**Travel Courses:**
- 6-10 bullet points about pragmatic/survival competence (EXPANDED)
- Focus on cultural norms for survival and respect
- Include non-verbal communication (gestures, personal space, eye contact)
- Include what to do/avoid to show respect in your destination
- Include tipping customs, dress codes, holiday closures
- Example: "In Germany, service charge is usually included; small tips (1-2%) are optional but appreciated for good service"

## Size Requirements by Course Type
Size requirements vary based on course type to maintain appropriate content density:

**Conversational Courses:**
- **Target**: 150-180 lines
- **Minimum**: 140 lines
- **Maximum**: 200 lines
- Rationale: Balanced vocabulary and grammar requires moderate density

**Grammar Courses:**
- **Target**: 150-200 lines
- **Minimum**: 145 lines
- **Maximum**: 220 lines
- Rationale: Extensive grammar explanations and paradigms require more space; vocabulary is minimal

**Travel Courses:**
- **Target**: 120-160 lines
- **Minimum**: 110 lines
- **Maximum**: 180 lines
- Rationale: Prioritize vocabulary density and task scenarios; minimal grammar

**Line counting**: Include YAML frontmatter, headers, code blocks, tables, bullet points, and blank lines.

## Quality Standards

### Grammar and Vocabulary Accuracy
- **CRITICAL**: All grammar explanations must be linguistically correct
- Verify verb conjugations, noun genders, case systems, etc.
- Use authoritative references for grammar rules
- Double-check vocabulary translations
- Be consistent with terminology throughout the course

### Pedagogical Completeness
- Each lesson should be self-contained and focused on specific learning objectives
- Progressive difficulty: build on previous lessons
- Balance comprehension (input) and production (output)
- Include recognition and production vocabulary
- Cover all major grammar points for the CEFR level

### AI-First Design
- Remember: The AI tutor will conduct actual conversations
- Scenario descriptions guide the AI, not the learner directly
- Focus on communicative goals and key phrases
- Let the AI adapt to learner level and interests
- Avoid rigid dialogue scripts

## Pedagogical Research Foundations

This lesson framework is grounded in modern applied linguistics research and proven instructional design principles:

**Conversational Courses** are based on:
- **Communicative Language Teaching (CLT)** - Canale & Swain (1980): Developing communicative competence through meaningful interaction
- **Output Hypothesis** - Swain (1985, 2005): Learners develop proficiency through producing language, not just consuming it
- **Interaction Hypothesis** - Long (1996): Negotiation of meaning through interaction facilitates language acquisition

**Grammar Courses** are based on:
- **Focus on Form (FonF)** - Long (1991), Doughty & Williams (1998): Targeted, deliberate attention to form within communicative contexts
- **Noticing Hypothesis** - Schmidt (1990): Learners acquire grammatical forms they consciously notice
- **Skill Acquisition Theory** - DeKeyser (2007): Explicit grammar instruction supports proceduralization of knowledge, especially for adult learners
- **Paradigm-based instruction** - Ellis (2006): Learning systematic patterns (paradigm tables) accelerates acquisition

**Travel Courses** are based on:
- **Task-Based Language Teaching (TBLT)** - Ellis (2003), Long (2015): Learners acquire language through completing meaningful tasks in the target language
- **Notional-Functional Syllabus** - Wilkins (1976): Organizing language around communicative functions and real-world situations (not grammatical structures)
- **Lexical Approach** - Lewis (1993): Formulaic chunks and collocations are primary units of language acquisition
- **Pragmatic Competence** - Hymes (1972): Knowing what to say, when to say it, and to whom—not just grammatical correctness

**General principles across all types:**
- **Comprehensible Input (i+1)** - Krashen (1985): Input should be slightly beyond current competence level
- **Spaced Repetition** - Cepeda et al. (2006): Spacing practice over time improves retention
- **Retrieval Practice** - Roediger & Butler (2011): Producing language from memory strengthens learning
- **Authentic Context** - Nunan (1989): Language should be practiced in realistic, meaningful contexts

## Connecting Lessons to Courses

Lessons are connected to courses via **explicit declaration** in `curriculum.yml`:

```yaml
courseId: de-conversational-german
progressionMode: COMPLETION_BASED
lessons:
  - id: lesson-01-greetings            # Must match lessonId in frontmatter
    file: lesson-01-greetings.md       # Filename in same directory
    requiredTurns: 20                  # Minimum conversation turns to complete
  - id: lesson-02-introductions
    file: lesson-02-introductions.md
    requiredTurns: 25                  # Minimum conversation turns to complete
  # ... additional lessons
```

**Important:**
- `lessonId` in YAML frontmatter must match `id` in curriculum.yml
- `file` path is relative to the course directory
- `progressionMode` is always COMPLETION_BASED (progression based on completion, not calendar time)
- `requiredTurns` sets minimum engagement before lesson completion

## Reference Examples

**High-quality reference lessons:**
- Spanish: `backend/src/main/resources/course-content/es-conversational-spanish/lesson-01-greetings.md`
- German: `backend/src/main/resources/course-content/de-conversational-german/lesson-01-greetings.md`
- French: `backend/src/main/resources/course-content/fr-conversational-french/lesson-02-introductions.md`

Study these files for examples of:
- Clear grammar explanations with multiple examples
- Well-organized vocabulary sections
- AI-friendly conversation scenario descriptions
- Balanced lesson length and content density

## Checklist Before Adding a New Lesson

**Basic Requirements (All Course Types):**
- [ ] YAML frontmatter is complete and valid
- [ ] lessonId matches the pattern `lesson-NN-topic`
- [ ] Course type (conversational/grammar/travel) is identified
- [ ] All 8 required sections are present and in order
- [ ] Grammar explanations are linguistically accurate with authoritative sources checked
- [ ] All vocabulary translations are accurate
- [ ] Conversation scenarios are brief descriptions, NOT full scripted dialogues
- [ ] No AI-first design violations (no dialogue scripts)
- [ ] Common mistakes show "wrong → correct (explanation)" format
- [ ] Lesson is added to curriculum.yml with correct id and file path

**Conversational Courses:**
- [ ] Lesson is 150-180 lines
- [ ] Grammar: 2-3 core points, 40-60 lines total
- [ ] Vocabulary: 30-60 items organized by semantic categories
- [ ] Scenarios: 2-3 descriptions with 3-5 key phrases each
- [ ] Practice patterns: 4-6 production-focused activities
- [ ] Common mistakes: 5-10 focused on communication clarity
- [ ] Cultural notes: 3-6 points on communication norms

**Grammar Courses:**
- [ ] Lesson is 150-200 lines
- [ ] Grammar: 4-6 points with 80-120 lines total
- [ ] Include complete paradigm tables (conjugations/declensions/cases)
- [ ] Minimum 5-7 examples per grammar rule
- [ ] Include negative, question, and irregular forms
- [ ] Vocabulary: 15-30 items (grammar-supporting words only)
- [ ] Scenarios: 1-2 descriptions focused on grammar application
- [ ] Practice patterns: 4-8 form-focused drills
- [ ] Common mistakes: 5-12 focused on grammatical accuracy
- [ ] Cultural notes: 3-5 points on grammar/cultural connections

**Travel Courses:**
- [ ] Lesson is 120-160 lines
- [ ] Grammar: 1-2 essential points only, 20-40 lines total
- [ ] Focus on survival communication, not meta-linguistic explanations
- [ ] Vocabulary: 50-80 items organized by communicative function/task
- [ ] Include formulaic chunks and pronunciation guidance for critical phrases
- [ ] Scenarios: 3-4 real-world task descriptions
- [ ] Practice patterns: 3-5 task completion activities
- [ ] Common mistakes: 3-8 focused on comprehension breakdowns/safety
- [ ] Cultural notes: 6-10 points on pragmatic competence and respect
