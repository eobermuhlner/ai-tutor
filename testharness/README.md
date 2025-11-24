# AI Tutor Test Harness

The AI Tutor Test Harness is a comprehensive testing framework designed to validate the functionality and pedagogical effectiveness of AI tutor conversations. It uses AI-as-judge methodology to evaluate tutor responses and simulate realistic learner interactions.

## Overview

The test harness allows for automated testing of the AI tutor's responses in various scenarios with different types of learners, error patterns, and pedagogical requirements.

### Key Features

- **Scenario-based Testing**: Supports YAML-based test scenarios with predefined conversation flows
- **AI-as-Judge Evaluation**: Uses AI to evaluate and score tutor responses based on pedagogical criteria
- **Simulated Learners**: AI-powered simulated learners that can make various types of mistakes
- **Comprehensive Reporting**: Generates detailed and summary reports with CSV exports
- **Multi-Provider Support**: Works with OpenAI, Azure OpenAI, Anthropic, and Ollama providers

## Architecture

The test harness consists of several key components:

- **Scenario Loader**: Loads and parses YAML scenario files from the `scenarios/` directory
- **Executor**: Executes test scenarios by coordinating with the backend API and AI judge
- **AI Judge**: Evaluates tutor responses using pedagogical criteria
- **Simulated Learner**: Generates realistic learner responses and intentional mistakes
- **API Client**: Interacts with the AI Tutor backend REST API
- **Report Generator**: Creates detailed test reports in multiple formats

## Scenario Format

Test scenarios are defined in YAML format with the following structure:

```yaml
id: unique-scenario-id
name: "Descriptive scenario name"
description: |
  Detailed description of the test scenario

learnerPersona:
  name: "Learner Name"
  cefrLevel: "A1"  # A1, A2, B1, B2, C1, C2
  sourceLanguage: "en"
  targetLanguage: "es"  # Language being learned
  commonErrors: 
    - "Common error type 1"
    - "Common error type 2"
  learningGoals:
    - "Learning goal 1"
    - "Learning goal 2"

tutorConfig:
  tutorName: "Tutor Name"
  initialPhase: "Auto"  # Free, Correction, Drill, Auto
  teachingStyle: "Guided"  # Reactive, Guided, Directive

conversationScript:
  - content: "Hello, how are you today?"
    intentionalErrors: []  # No errors in this message
    notes: "Initial greeting to establish context"
  - content: "Yo es Alex"
    intentionalErrors:
      - span: "es"
        errorType: "Agreement"
        expectedSeverity: "Medium"
        correctForm: "soy"
        reasoning: "First person 'yo' requires 'soy', not 'es'"
    notes: "Subject-verb agreement error"
    
expectedOutcomes:
  phaseTransitions: []
  shouldTriggerDrillPhase: false
  shouldMaintainFreePhase: true
  topicChanges: 0

evaluationFocus:
  - "ERROR_DETECTION"
  - "PHASE_APPROPRIATENESS"
```

## Running Tests

### Prerequisites

1. Start the AI Tutor backend server:
   ```bash
   ./gradlew :backend:bootRun
   ```

2. Ensure you have an LLM provider configured with appropriate API keys in environment variables.

### Execution

To run the test harness:

```bash
# With OpenAI
OPENAI_API_KEY=your-key ./gradlew :testharness:bootRun --args="--spring.profiles.active=ai-openai"

# With Azure OpenAI
AZURE_OPENAI_API_KEY=your-key ./gradlew :testharness:bootRun --args="--spring.profiles.active=ai-azure-openai"

# With Ollama
./gradlew :testharness:bootRun --args="--spring.profiles.active=ai-ollama"

# With Anthropic
ANTHROPIC_API_KEY=your-key ./gradlew :testharness:bootRun --args="--spring.profiles.active=ai-anthropic"
```

If using a different backend URL (not default localhost:8080):
```bash
TESTHARNESS_BACKEND_URL=http://localhost:8081 OPENAI_API_KEY=your-key ./gradlew :testharness:bootRun --args="--spring.profiles.active=ai-openai"
```

### Configuration

Configuration options can be set as environment variables:

- `TESTHARNESS_BACKEND_URL`: Backend server URL (default: http://localhost:8080)
- `TESTHARNESS_SCENARIOS_DIR`: Directory containing scenario files (default: scenarios)

## Reports

The test harness generates three types of reports:

1. **Summary Report** (`test_results_summary.md`): High-level test results
2. **Detailed Report** (`test_results_detailed.md`): Complete conversation history and evaluations
3. **CSV Report** (`test_results.csv`): Structured data for analysis

## Adding New Scenarios

To add new test scenarios:

1. Create a YAML file in the `scenarios/` directory
2. Follow the scenario format described above
3. Include diverse conversation patterns and error types
4. Define clear evaluation criteria

## Customization

The test harness can be customized by modifying:

- **Prompts**: Located in `src/main/resources/application-prompts-*.yml`
- **Configuration**: In `src/main/resources/application.yml`
- **Evaluation Criteria**: In the `JudgeService`

## Development

The test harness is built with:
- Spring Boot 3.5.6
- Kotlin 1.9.25
- Spring AI 1.0.1 for AI interactions
- Gradle for build management

## Troubleshooting

- **403 Errors**: Usually related to authentication issues. Ensure demo user exists and proper authentication headers are sent.
- **LLM Connection Errors**: Verify API keys are correct and provider is running.
- **Session Creation Failures**: Check that the backend is running and all required fields are provided.

## Integration with CI/CD

The test harness can be integrated into continuous integration pipelines to automate testing of AI tutor functionality with each deployment.