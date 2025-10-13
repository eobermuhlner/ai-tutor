# AI Tutor Test Scenarios

This directory contains test scenarios for the pedagogical test harness. Each scenario simulates a realistic learner conversation with intentional errors to evaluate the tutor's pedagogical behavior.

## Available Scenarios

### 1. beginner-agreement-errors.yml
**Focus**: Fossilization detection and Drill phase transition

Tests whether the system correctly identifies repeated subject-verb agreement errors as a fossilization risk and transitions to Drill phase for explicit correction work.

- **Learner Level**: A1 (Beginner)
- **Language**: Spanish
- **Error Pattern**: Repeated first-person verb conjugation errors
- **Expected Outcome**: Transition to Drill phase after 3-4 repeated high-severity errors

### 2. advanced-fluency-focus.yml
**Focus**: Free phase maintenance for fluent learners

Tests whether the system correctly maintains Free phase (no error tracking) for an advanced learner with only minor typography errors, focusing on fluency and confidence.

- **Learner Level**: C1 (Advanced)
- **Language**: French
- **Error Pattern**: Minor missing accents (acceptable in casual chat)
- **Expected Outcome**: Stay in Free phase throughout conversation

### 3. intermediate-mixed-errors.yml
**Focus**: Correction phase with varied error types

Tests whether the system correctly handles an intermediate learner making various error types (articles, cases, word order) while staying in Correction phase with passive feedback.

- **Learner Level**: B1 (Intermediate)
- **Language**: German
- **Error Pattern**: Mixed article gender, case, and word order errors
- **Expected Outcome**: Stay in Correction phase, detect all errors passively

### 4. topic-management-test.yml
**Focus**: Topic hysteresis and variety

Tests whether the system manages conversation topics appropriately, allowing smooth transitions, maintaining variety, and avoiding repetition.

- **Learner Level**: B2 (Upper Intermediate)
- **Language**: Spanish
- **Focus**: Multiple topic changes (vacation → work → climate change)
- **Expected Outcome**: Smooth topic transitions, no thrashing

### 5. critical-comprehension-errors.yml
**Focus**: Immediate intervention for critical errors

Tests whether the system immediately transitions to Drill phase when learner makes critical errors that completely block comprehension.

- **Learner Level**: A2 (Elementary)
- **Language**: Italian
- **Error Pattern**: Critical vocabulary/tense confusion blocking meaning
- **Expected Outcome**: Immediate transition to Drill phase after first critical error

## Creating Custom Scenarios

### Scenario File Structure

```yaml
id: unique-scenario-id
name: Human-Readable Scenario Name
description: |
  Multi-line description explaining what this scenario tests
  and why it's important for pedagogical evaluation.

learnerPersona:
  name: Learner's name
  cefrLevel: A1|A2|B1|B2|C1|C2
  sourceLanguage: en  # Learner's native language
  targetLanguage: es  # Language being learned
  commonErrors:
    - Error pattern 1
    - Error pattern 2
  learningGoals:
    - Learning goal 1
    - Learning goal 2

tutorConfig:
  tutorName: Tutor's name
  initialPhase: Auto|Free|Correction|Drill
  teachingStyle: Reactive|Guided|Directive

conversationScript:
  - content: "Learner's message in target language"
    intentionalErrors:
      - span: "exact error text"
        errorType: "Agreement|TenseAspect|WordOrder|Lexis|Morphology|Articles|Pronouns|Prepositions|Typography"
        expectedSeverity: "Critical|High|Medium|Low"
        correctForm: "corrected version"
        reasoning: "Why this is an error and why it has this severity"
    notes: "Optional notes explaining context or learner intent"

  - content: "Another learner message"
    intentionalErrors: []  # Correct message

  # ... more conversation turns

expectedOutcomes:
  phaseTransitions:
    - afterMessageIndex: 3
      toPhase: Drill
      reason: "Expected reason for phase transition"
  minimumCorrectionsDetected: 4
  topicChanges: 2
  vocabularyItems: 5
  shouldTriggerDrillPhase: true|false
  shouldMaintainFreePhase: true|false

evaluationFocus:
  - ERROR_DETECTION
  - PHASE_APPROPRIATENESS
  - CORRECTION_QUALITY
  - ENCOURAGEMENT_BALANCE
  - TOPIC_MANAGEMENT
  - VOCABULARY_TEACHING
  - COMPREHENSIBILITY
  - FOSSILIZATION_DETECTION
```

### Error Types

- **TenseAspect**: Wrong tense or aspect (e.g., present instead of past)
- **Agreement**: Subject-verb, gender, or number agreement issues
- **WordOrder**: Syntax errors, misplaced words or clauses
- **Lexis**: Wrong vocabulary, false friends, register issues
- **Morphology**: Incorrect endings, cases, conjugations
- **Articles**: Missing, wrong, or unnecessary articles/determiners
- **Pronouns**: Wrong pronoun form or reference
- **Prepositions**: Wrong or missing prepositions
- **Typography**: Spelling, diacritics, capitalization, punctuation

### Error Severity Guidelines

- **Critical**: Meaning completely lost, comprehension impossible
  - Example: Using wrong verb entirely ("morire" instead of "avere")
  - Example: Complete vocabulary confusion blocking understanding

- **High**: Significant comprehension barrier (global error)
  - Example: Repeated fossilized errors indicating systematic misunderstanding
  - Example: Major tense confusion changing meaning

- **Medium**: Noticeable grammar issue but meaning clear from context
  - Example: Wrong article gender
  - Example: Subject-verb agreement error (meaning still clear)

- **Low**: Minor issue or acceptable in casual chat/texting
  - Example: Missing accents (café → cafe)
  - Example: No end punctuation in chat message
  - Example: Capitalization errors

### Evaluation Focus Areas

Choose 2-4 focus areas most relevant to your scenario:

- **ERROR_DETECTION**: Tests accuracy of error identification and classification
- **PHASE_APPROPRIATENESS**: Tests whether phase selection matches error patterns
- **CORRECTION_QUALITY**: Tests clarity and appropriateness of corrections
- **ENCOURAGEMENT_BALANCE**: Tests balance between feedback and motivation
- **TOPIC_MANAGEMENT**: Tests topic selection, transitions, and variety
- **VOCABULARY_TEACHING**: Tests vocabulary introduction and reinforcement
- **COMPREHENSIBILITY**: Tests whether tutor maintains comprehensible input
- **FOSSILIZATION_DETECTION**: Tests detection of repeated systematic errors

## Best Practices

### 1. Realistic Learner Errors

Make errors that actual learners at that CEFR level would make:
- **A1/A2**: Basic conjugation, article confusion, word order
- **B1/B2**: Complex tenses, subjunctive, prepositions, register
- **C1/C2**: Subtle nuances, idioms, advanced register, minor typos

### 2. Clear Expected Outcomes

Define specific, measurable expected outcomes:
- Number of errors that should be detected
- Expected phase transitions with timing and reasoning
- Topic changes expected
- Vocabulary introductions

### 3. Representative Conversations

Create conversations that feel natural and realistic:
- Use appropriate topics for the learner level
- Mix correct and incorrect messages
- Include some correct usage of challenging structures
- Vary error types (not just one error pattern)

### 4. Appropriate Scenario Length

- **Short scenarios** (3-5 turns): Test specific behaviors (phase transition, error detection)
- **Medium scenarios** (6-10 turns): Test conversation flow and multiple behaviors
- **Long scenarios** (10+ turns): Test topic management, sustained patterns

### 5. Test Edge Cases

Create scenarios that test:
- **Boundary conditions**: Just enough errors to trigger Drill, just below threshold
- **Error severity**: Critical vs. Low errors and appropriate responses
- **Topic stability**: Too-frequent changes vs. appropriate variety
- **Mixed patterns**: Improving vs. worsening error patterns

## Running Scenarios

### List Available Scenarios

```bash
./gradlew runTestHarness --args="--list"
```

This displays a formatted table showing all available scenarios with their ID, CEFR level, target language, and primary evaluation focus.

### Run All Scenarios

```bash
./gradlew runTestHarness
```

### Run Specific Scenario

```bash
./gradlew runTestHarness --args="--scenario beginner-errors"
```

### Run Multiple Related Scenarios

```bash
# Run all scenarios with "phase" in the name/ID
./gradlew runTestHarness --args="--scenario phase"

# Run all beginner scenarios
./gradlew runTestHarness --args="--scenario beginner"
```

## Interpreting Results

The test harness generates a detailed report in `test-reports/` with:

- **Overall Score** (0-100): Weighted average of all evaluation dimensions
- **Dimension Scores**: Individual scores for each of the 6 evaluation areas
- **Technical Metrics**: Error detection accuracy, phase transitions, vocabulary
- **Judge Feedback**: Detailed qualitative feedback from the LLM judge
- **Strengths**: What the tutor did well
- **Improvements**: Specific recommendations for improvement

### Passing Threshold

Default threshold is **70.0** (configurable in `testharness-config.yml`).

Scenarios below this threshold indicate pedagogical issues that should be investigated.

## Contributing New Scenarios

When contributing scenarios:

1. **Test realistic situations** you've observed or that represent important pedagogical cases
2. **Document clearly** what the scenario tests and why it's important
3. **Validate manually** first by running the scenario and reviewing the conversation
4. **Include in PR description** what pedagogical behavior the scenario validates
5. **Follow naming conventions**: `<level>-<focus>-<variation>.yml`
   - Example: `beginner-agreement-errors.yml`
   - Example: `advanced-subjunctive-mastery.yml`

## Questions?

See the main project README.md or CLAUDE.md for more information about the test harness architecture and evaluation criteria.
