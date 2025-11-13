# Unified Catalog Import Format

The AI Tutor supports importing languages, tutors, and courses from a unified YAML format. This document specifies the complete format and provides examples.

## Table of Contents

- [Overview](#overview)
- [Format Specification](#format-specification)
- [Entity Definitions](#entity-definitions)
  - [Tutor Archetypes](#tutor-archetypes)
  - [Languages](#languages)
  - [Tutors](#tutors)
  - [Courses](#courses)
  - [Lessons](#lessons)
- [Import Methods](#import-methods)
- [Examples](#examples)
- [Migration Guide](#migration-guide)

---

## Overview

The unified catalog format (`catalog.yml`) allows you to define languages, tutors, and courses in a single file. All sections are optional, enabling both complete catalog imports and partial updates.

**Key features:**
- Single-file catalog management
- Multilingual support throughout
- Optional tutor archetypes for DRY principle
- Flexible lesson content (embedded or file-referenced)
- Backward compatible with existing course-content structure

**Format version:** 1.0

---

## Format Specification

### Root Structure

```yaml
version: "1.0"                    # Format version (required)

tutorArchetypes: [...]            # Optional: Reusable tutor templates
languages: [...]                  # Optional: Language definitions
tutors: [...]                     # Optional: Tutor profiles
courses: [...]                    # Optional: Course definitions
```

All sections are **optional**. You can import:
- Just languages: `languages: [...]`
- Just tutors: `tutorArchetypes: [...], tutors: [...]`
- Just courses: `courses: [...]`
- Any combination of the above

---

## Entity Definitions

### Tutor Archetypes

Reusable tutor personality templates. Define once, reference multiple times.

**Schema:**

```yaml
tutorArchetypes:
  - id: string                              # Unique identifier (required)
    emoji: string                           # Display emoji (required)
    personaEnglish: string                  # Persona description in English (required)
    domainEnglish: string                   # Teaching domain in English (required)
    descriptionTemplateEnglish: string      # Description with {culturalNotes} placeholder (required)
    personality: enum                       # Encouraging | Strict | Casual | Academic (required)
    teachingStyle: enum                     # Reactive | Guided | Directive (required, default: Reactive)
    displayOrder: integer                   # Sort order (required, default: 0)
    voiceId: enum                           # Warm | Authoritative | Friendly | Professional | Calm (optional)
```

**Personality options:**
- `Encouraging` - Patient, supportive, builds confidence
- `Strict` - High standards, formal, disciplined
- `Casual` - Relaxed, friendly, conversational
- `Academic` - Scholarly, analytical, formal

**Teaching style options:**
- `Reactive` - Responds to learner's pace and interests
- `Guided` - Provides structure with gentle direction
- `Directive` - Structured curriculum with clear objectives

**Voice ID options:**
- `Warm` - Friendly and encouraging tone
- `Authoritative` - Confident and commanding
- `Friendly` - Casual and approachable
- `Professional` - Formal and polished
- `Calm` - Soothing and patient

**Example:**

```yaml
tutorArchetypes:
  - id: encouraging-general
    emoji: "👩‍🏫"
    personaEnglish: patient coach
    domainEnglish: general conversation, grammar, typography
    descriptionTemplateEnglish: "Patient teacher {culturalNotes} who loves helping beginners feel confident"
    personality: Encouraging
    teachingStyle: Guided
    displayOrder: 0
    voiceId: Warm
```

---

### Languages

Language definitions with multilingual metadata.

**Schema:**

```yaml
languages:
  - code: string                       # BCP 47 language code (required, e.g., "de-DE", "ja-JP")
    name: object                       # Multilingual language names (required)
      en: string                       # English name
      [locale]: string                 # Other language names
    flagEmoji: string                  # Flag emoji (required)
    nativeName: string                 # Native language name (required)
    difficulty: enum                   # Easy | Medium | Hard (required)
    description: object                # Multilingual descriptions (required)
      en: string                       # English description
      [locale]: string                 # Other descriptions
    isActive: boolean                  # Active status (optional, default: true)
    displayOrder: integer              # Sort order (optional, default: 0)
```

**Difficulty levels:**
- `Easy` - Similar to English, simple grammar (Spanish, Italian)
- `Medium` - Moderate differences, moderate complexity (German, French)
- `Hard` - Very different structure, complex writing system (Japanese, Chinese, Korean, Arabic)

**Example:**

```yaml
languages:
  - code: de-DE
    name:
      en: German (Germany)
      de: Deutsch (Deutschland)
    flagEmoji: "🇩🇪"
    nativeName: Deutsch (Deutschland)
    difficulty: Medium
    description:
      en: Standard German from Germany with precise grammar rules
      de: Hochdeutsch aus Deutschland mit präzisen Grammatikregeln
    isActive: true
    displayOrder: 0
```

---

### Tutors

Tutor profile definitions. Supports **two approaches**:

#### Approach 1: Reference Archetype (DRY)

```yaml
tutors:
  - name: string                       # Tutor name (required)
    targetLanguage: string             # BCP 47 language code (required)
    archetypeId: string                # Reference to archetype (required for this approach)
    culturalNotes: string              # Replaces {culturalNotes} in archetype template (required for this approach)

    # Optional overrides
    emoji: string                      # Override archetype emoji
    location: string                   # Geographic location
    gender: enum                       # Male | Female | Neutral
    age: integer                       # Age (default: 30)
    personality: enum                  # Override archetype personality
    teachingStyle: enum                # Override archetype teaching style
    voiceId: enum                      # Override archetype voice

    # Visibility
    isGlobal: boolean                  # Global (all users) or user-specific (default: true)
    displayOrder: integer              # Sort order (default: 0)
```

#### Approach 2: Direct Definition (No Archetype)

```yaml
tutors:
  - name: string                       # Tutor name (required)
    targetLanguage: string             # BCP 47 language code (required)

    # Required for direct definition
    emoji: string                      # Display emoji (required)
    persona: object                    # Multilingual persona (required)
      en: string
      [locale]: string
    domain: object                     # Multilingual domain (required)
      en: string
      [locale]: string
    description: object                # Multilingual description (required)
      en: string
      [locale]: string
    personality: enum                  # Personality type (required)
    teachingStyle: enum                # Teaching style (required)

    # Optional fields
    culturalBackground: object         # Multilingual cultural background
      en: string
      [locale]: string
    location: string                   # Geographic location
    voiceId: enum                      # Voice ID
    gender: enum                       # Male | Female | Neutral
    age: integer                       # Age (default: 30)
    isGlobal: boolean                  # Visibility (default: true)
    displayOrder: integer              # Sort order (default: 0)
```

**Example (Archetype Reference):**

```yaml
tutors:
  - name: Anna
    targetLanguage: de-DE
    archetypeId: encouraging-general
    culturalNotes: from Munich
    location: Munich, Germany
    gender: Female
    age: 28
    isGlobal: true
```

**Example (Direct Definition):**

```yaml
tutors:
  - name: Sofia
    targetLanguage: es-ES
    emoji: "😊"
    persona:
      en: friendly conversationalist
      es: conversadora amigable
    domain:
      en: everyday conversation, culture, travel
      es: conversación cotidiana, cultura, viajes
    description:
      en: Friendly tutor from Madrid who makes learning Spanish fun
      es: Tutora amigable de Madrid que hace que aprender español sea divertido
    personality: Casual
    teachingStyle: Reactive
    location: Madrid, Spain
    gender: Female
    age: 26
```

---

### Courses

Course definitions with optional embedded curriculum.

**Schema:**

```yaml
courses:
  - languageCode: string               # BCP 47 language code (required)
    name: object                       # Multilingual course names (required)
      en: string
      [locale]: string
    shortDescription: object           # Multilingual short descriptions (required)
      en: string
      [locale]: string
    description: object                # Multilingual full descriptions (required)
      en: string
      [locale]: string
    category: enum                     # Conversational | Travel | General | Grammar (required)
    targetAudience: object             # Multilingual target audience (required)
      en: string
      [locale]: string
    startingLevel: enum                # CEFR level: None | A1 | A2 | B1 | B2 | C1 | C2 (required)
    targetLevel: enum                  # CEFR level: None | A1 | A2 | B1 | B2 | C1 | C2 (required)
    estimatedWeeks: integer            # Estimated duration in weeks (optional)
    learningGoals: object              # Multilingual learning goals (required)
      en: [string]                     # Array of goal strings
      [locale]: [string]
    suggestedTutors: [string]          # Array of tutor names (optional)
    tags: [string]                     # Array of tags (optional)
    displayOrder: integer              # Sort order (optional, default: 0)
    requiresCurriculum: boolean        # Whether curriculum is required (optional, default: true)
    curriculum: object                 # Optional embedded curriculum (see below)
```

**Course categories:**
- `Conversational` - Natural conversation practice
- `Travel` - Tourist and traveler phrases
- `General` - Structured grammar + conversation
- `Grammar` - Grammar fundamentals

**CEFR levels:**
- `None` - No prerequisite or target
- `A1` - Beginner
- `A2` - Elementary
- `B1` - Intermediate
- `B2` - Upper Intermediate
- `C1` - Advanced
- `C2` - Proficient

**Example (Without Curriculum):**

```yaml
courses:
  - languageCode: de-DE
    name:
      en: Conversational German
      de: Konversationsdeutsch
    shortDescription:
      en: Master everyday conversations at your own pace
    description:
      en: Learn to communicate naturally in German through practical conversations
    category: Conversational
    targetAudience:
      en: Anyone wanting to speak German naturally
    startingLevel: None
    targetLevel: B2
    estimatedWeeks: 24
    learningGoals:
      en:
        - Greet people and introduce yourself
        - Talk about daily routines and activities
        - Express opinions and feelings
    suggestedTutors:
      - Anna
      - Hans
    tags:
      - beginner-friendly
      - conversation
    displayOrder: 1
    requiresCurriculum: true
```

---

### Lessons

Lessons can be defined within a course's curriculum. Supports **two approaches**:

#### Curriculum Structure

```yaml
courses:
  - # ... course fields ...
    curriculum:
      progressionMode: enum            # TIME_BASED | LINEAR (required)
      lessons: [...]                   # Array of lessons (required)
```

**Progression modes:**
- `TIME_BASED` - Lessons unlock based on time + turn requirements
- `LINEAR` - Sequential unlocking regardless of time

#### Approach 1: Embedded Content

```yaml
lessons:
  - id: string                         # Unique lesson ID (required)
    title: string                      # Lesson title (optional)
    minimumDays: integer               # Minimum days before next lesson (default: 0)
    requiredTurns: integer             # Required conversation turns (default: 0)
    content: string                    # Full markdown content with frontmatter (required for this approach)
```

#### Approach 2: File Reference

```yaml
lessons:
  - id: string                         # Unique lesson ID (required)
    title: string                      # Lesson title (optional)
    minimumDays: integer               # Minimum days before next lesson (default: 0)
    requiredTurns: integer             # Required conversation turns (default: 0)
    file: string                       # Markdown filename (required for this approach)
```

**Example (Embedded Content):**

```yaml
courses:
  - languageCode: de-DE
    name:
      en: Quick German Basics
    # ... other course fields ...
    curriculum:
      progressionMode: TIME_BASED
      lessons:
        - id: lesson-01-greetings
          title: Greetings and Introductions
          minimumDays: 3
          requiredTurns: 15
          content: |
            ---
            lessonId: lesson-01-greetings
            title: Greetings and Introductions
            targetCEFR: A1
            ---

            ## This Week's Goals
            - Master common German greetings
            - Introduce yourself

            ## Vocabulary
            - Hallo - Hello
            - Guten Tag - Good day
            - Wie heißt du? - What is your name?
```

**Example (File Reference):**

```yaml
courses:
  - languageCode: de-DE
    name:
      en: Conversational German
    # ... other course fields ...
    curriculum:
      progressionMode: TIME_BASED
      lessons:
        - id: week-01-greetings
          title: Greetings and Basic Expressions
          minimumDays: 7
          requiredTurns: 20
          file: week-01-greetings.md
        - id: week-02-introductions
          minimumDays: 7
          requiredTurns: 25
          file: week-02-introductions.md
```

**Note:** When using file references, the markdown files must be uploaded alongside the catalog.yml file via the import API.

---

## Import Methods

### 1. Complete Catalog Import

Import languages, tutors, and courses together.

**REST API:**
```bash
POST /api/v1/catalog/import
Content-Type: multipart/form-data

Files:
  - catalogFile: catalog.yml
  - lessonFiles: [lesson1.md, lesson2.md, ...] (optional, for file-referenced lessons)

Requires: ADMIN role

Response:
{
  "languagesImported": 17,
  "tutorsImported": 72,
  "coursesImported": 127,
  "lessonsImported": 450,
  "errors": [],
  "success": true
}
```

### 2. Language-Only Import

Import just language definitions.

**REST API:**
```bash
POST /api/v1/languages/import
Content-Type: multipart/form-data

Files:
  - catalogFile: catalog.yml (containing languages section)

Requires: ADMIN role

Response:
{
  "success": true,
  "languagesImported": 17,
  "created": 5,
  "updated": 12
}
```

### 3. Validation Before Import

Validate catalog file without importing.

**REST API:**
```bash
POST /api/v1/catalog/import/validate
Content-Type: multipart/form-data

Files:
  - catalogFile: catalog.yml
  - lessonFiles: [lesson1.md, ...] (optional)

Response:
{
  "valid": true,
  "errors": []
}
```

### 4. Application Startup Seeding

Place `catalog-seed.yml` in `backend/src/main/resources/` to automatically seed the database on startup.

**Precedence:**
1. Tries `catalog-seed.yml` (unified format) first
2. Falls back to `application-seed.yml` (legacy format) if not found

---

## Examples

### Example 1: Complete Catalog

```yaml
version: "1.0"

tutorArchetypes:
  - id: encouraging-general
    emoji: "👩‍🏫"
    personaEnglish: patient coach
    domainEnglish: general conversation, grammar
    descriptionTemplateEnglish: "Patient teacher {culturalNotes} who loves helping beginners"
    personality: Encouraging
    teachingStyle: Guided
    displayOrder: 0
    voiceId: Warm

languages:
  - code: de-DE
    name:
      en: German (Germany)
      de: Deutsch (Deutschland)
    flagEmoji: "🇩🇪"
    nativeName: Deutsch (Deutschland)
    difficulty: Medium
    description:
      en: Standard German from Germany
    isActive: true
    displayOrder: 0

tutors:
  - name: Anna
    targetLanguage: de-DE
    archetypeId: encouraging-general
    culturalNotes: from Munich
    location: Munich, Germany
    gender: Female
    age: 28
    isGlobal: true

courses:
  - languageCode: de-DE
    name:
      en: German for Beginners
    shortDescription:
      en: Start your German journey
    description:
      en: Complete beginner course for German language learners
    category: General
    targetAudience:
      en: Complete beginners
    startingLevel: None
    targetLevel: A2
    estimatedWeeks: 12
    learningGoals:
      en:
        - Master basic greetings
        - Form simple sentences
        - Understand basic conversations
    suggestedTutors:
      - Anna
    displayOrder: 1
    requiresCurriculum: true
    curriculum:
      progressionMode: TIME_BASED
      lessons:
        - id: lesson-01
          title: Greetings
          minimumDays: 3
          requiredTurns: 15
          file: lesson-01-greetings.md
```

### Example 2: Languages Only

```yaml
version: "1.0"

languages:
  - code: de-DE
    name:
      en: German (Germany)
      de: Deutsch (Deutschland)
    flagEmoji: "🇩🇪"
    nativeName: Deutsch (Deutschland)
    difficulty: Medium
    description:
      en: Standard German from Germany

  - code: ja-JP
    name:
      en: Japanese (Japan)
      ja: 日本語 (日本)
    flagEmoji: "🇯🇵"
    nativeName: 日本語 (日本)
    difficulty: Hard
    description:
      en: Japanese with kanji, hiragana, and katakana
```

### Example 3: Courses with Embedded Lessons

```yaml
version: "1.0"

courses:
  - languageCode: es-ES
    name:
      en: Quick Spanish
    shortDescription:
      en: Learn Spanish in 2 weeks
    description:
      en: Intensive Spanish crash course
    category: Conversational
    targetAudience:
      en: Busy professionals
    startingLevel: None
    targetLevel: A1
    estimatedWeeks: 2
    learningGoals:
      en:
        - Basic greetings
        - Order food
        - Ask directions
    displayOrder: 1
    curriculum:
      progressionMode: LINEAR
      lessons:
        - id: day-01
          title: Greetings
          requiredTurns: 10
          content: |
            ---
            lessonId: day-01
            title: Greetings
            targetCEFR: A1
            ---

            ## Today's Goals
            - Say hello and goodbye
            - Introduce yourself

            ## Vocabulary
            - Hola - Hello
            - Adiós - Goodbye
            - Me llamo... - My name is...
```

---

## Migration Guide

### From Legacy Format (application-seed.yml)

**Step 1: Extract structure**
```yaml
# OLD (application-seed.yml)
ai-tutor:
  catalog:
    tutorArchetypes: [...]
    languages: [...]
    courses: [...]
```

**Step 2: Remove wrapper and add version**
```yaml
# NEW (catalog-seed.yml)
version: "1.0"

tutorArchetypes: [...]
languages: [...]
tutors: [...]  # Flatten from languages[].tutorVariants
courses: [...]
```

**Step 3: Convert JSON strings to objects**
```yaml
# OLD
languages:
  - nameJson: '{"en": "German", "de": "Deutsch"}'
    descriptionJson: '{"en": "Standard German"}'

# NEW
languages:
  - name:
      en: German
      de: Deutsch
    description:
      en: Standard German
```

**Step 4: Flatten tutor variants**
```yaml
# OLD
languages:
  - code: de-DE
    tutorVariants:
      - archetypeId: encouraging-general
        name: Anna
        culturalNotes: from Munich

# NEW
languages:
  - code: de-DE
    # ... language fields ...

tutors:
  - name: Anna
    targetLanguage: de-DE
    archetypeId: encouraging-general
    culturalNotes: from Munich
```

**Step 5: Convert course fields**
```yaml
# OLD
courses:
  - nameEnglish: Conversational German
    shortDescriptionEnglish: Master everyday conversations
    learningGoalsEnglish:
      - Goal 1
      - Goal 2

# NEW
courses:
  - name:
      en: Conversational German
    shortDescription:
      en: Master everyday conversations
    learningGoals:
      en:
        - Goal 1
        - Goal 2
```

### Automated Conversion

A conversion script has already been used to migrate the existing `application-seed.yml` to `catalog-seed.yml`.

**To manually convert**:
1. Read application-seed.yml
2. Apply transformations above
3. Write to catalog-seed.yml
4. Test by starting the application (it will auto-load catalog-seed.yml)

---

## Validation Rules

The import system validates:

1. **Version**: Must be "1.0"
2. **Archetype references**: All `archetypeId` values must reference existing archetypes
3. **Tutor definitions**: Must have either `archetypeId` OR complete direct definition fields
4. **Language codes**: Must be valid BCP 47 codes (e.g., "de-DE", "ja-JP")
5. **Tutor references**: Course `suggestedTutors` must reference existing tutor names
6. **Lesson content**: Each lesson must have either `content` OR `file`
7. **File references**: All lesson files must be uploaded with the catalog
8. **CEFR levels**: Must be valid: None, A1, A2, B1, B2, C1, C2
9. **Enums**: All enum fields must use valid values

**Validation endpoint**: `POST /api/v1/catalog/import/validate`

---

## Best Practices

1. **Use archetypes for multiple similar tutors** - DRY principle
2. **Provide multilingual content** - At minimum, include English (`en`)
3. **File reference for large lessons** - Embed for small/quick lessons
4. **Progressive disclosure** - Import languages first, then tutors, then courses
5. **Test with validation endpoint** - Catch errors before importing
6. **Version your catalogs** - Use git to track changes to catalog.yml files
7. **Separate concerns** - Keep lesson markdown in course-content/ directories for readability

---

## Troubleshooting

### Import fails with "Archetype not found"
- Ensure `tutorArchetypes` section is defined before `tutors` section
- Check that `archetypeId` matches exactly (case-sensitive)

### Import fails with "Missing lesson file"
- Upload all referenced lesson .md files with the catalog.yml
- Ensure lesson `id` matches the parsed `lessonId` from markdown frontmatter

### Seeding doesn't use catalog-seed.yml
- Check file location: `backend/src/main/resources/catalog-seed.yml`
- Check file name spelling (must be exactly `catalog-seed.yml`)
- Check application logs for parsing errors

### JSON parsing errors
- Validate YAML syntax (use a YAML linter)
- Ensure proper indentation (2 spaces, no tabs)
- Ensure strings with special characters are quoted

---

## Reference Links

- **Backend Service**: `UnifiedCatalogImportService.kt`
- **REST Controller**: `CatalogImportController.kt`
- **Domain Models**: `UnifiedCatalogImport.kt`
- **Seed Service**: `SeedDataService.kt`
- **Example Catalog**: `backend/src/main/resources/catalog-seed.yml`

---

**Version**: 1.0
**Last Updated**: 2025-01-12
