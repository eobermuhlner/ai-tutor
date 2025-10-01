# AI Tutor

An intelligent language learning platform powered by AI that provides personalized conversational tutoring with real-time error correction and vocabulary tracking.

## 🎯 Features

- **Conversational AI Tutoring**: Natural language conversations with adaptive AI tutors
- **Real-time Error Detection**: Automatic correction of grammar, typography, and word choice errors
- **CEFR Level Tracking**: Monitors learner progress from A1 to C2 levels
- **Vocabulary Management**: Tracks new vocabulary with context and exposure frequency
- **Dual-Language Explanations**: Provides corrections in both source and target languages
- **Conversation Modes**:
  - **Free Mode**: Natural conversation without interruptions
  - **Drill Mode**: Focused practice with immediate error corrections
- **Session Persistence**: Save and resume learning sessions
- **RESTful API**: Full REST API for integration with web and mobile applications

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Gradle 8.x
- OpenAI API key

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd ai-tutor
```

2. Set your OpenAI API key:
```bash
export OPENAI_API_KEY=your-api-key-here
```

3. Build the project:
```bash
./gradlew build
```

4. Run the application:
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`.

## 📡 API Usage

### Create a Learning Session

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "00000000-0000-0000-0000-000000000001",
    "tutorName": "Maria",
    "sourceLanguage": "English",
    "targetLanguage": "Spanish",
    "estimatedCEFRLevel": "A1"
  }'
```

### Send a Message

```bash
curl -X POST http://localhost:8080/api/v1/chat/sessions/{sessionId}/messages \
  -H "Content-Type: application/json" \
  -d '{"content": "Hola, como estas?"}'
```

### Get Session History

```bash
curl http://localhost:8080/api/v1/chat/sessions/{sessionId}
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/chat/sessions` | Create new learning session |
| GET | `/api/v1/chat/sessions?userId={uuid}` | List user's sessions |
| GET | `/api/v1/chat/sessions/{id}` | Get session with full message history |
| POST | `/api/v1/chat/sessions/{id}/messages` | Send message (JSON response) |
| POST | `/api/v1/chat/sessions/{id}/messages/stream` | Send message (SSE streaming) |
| DELETE | `/api/v1/chat/sessions/{id}` | Delete session |

## 🧪 Testing with IntelliJ HTTP Client

The project includes HTTP request examples in `src/test/http/http-client-requests.http`.

1. Open the file in IntelliJ IDEA
2. Select environment: `local 8080`
3. Run "Create Chat Session" to generate a session
4. The session ID is automatically captured for subsequent requests
5. Execute other requests to interact with the API

## 🏗️ Architecture

### Technology Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Database**: H2 (in-memory, JPA)
- **AI Integration**: Spring AI 1.0.1 with OpenAI
- **Build Tool**: Gradle

### Core Components

```
ch.obermuhlner.aitutor
├── chat/
│   ├── controller/     # REST API endpoints
│   ├── service/        # Business logic & orchestration
│   ├── repository/     # Data access layer
│   ├── domain/         # JPA entities (sessions, messages)
│   └── dto/            # API request/response objects
├── tutor/
│   └── service/        # Core tutoring logic with AI
├── conversation/
│   └── service/        # AI chat integration & streaming
├── vocabulary/
│   ├── service/        # Vocabulary tracking
│   ├── repository/     # Vocabulary persistence
│   └── domain/         # Vocabulary entities
└── core/
    └── model/          # Domain models (Tutor, Correction, etc.)
```

### Key Design Patterns

- **Repository Pattern**: Clean separation of data access
- **Service Layer**: Business logic isolation
- **DTO Pattern**: API contract separation from domain models
- **Strategy Pattern**: Pluggable AI chat service implementations

## 🧑‍💻 Development

### Configuration

Application configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ai-tutor
  profiles:
    group:
      dev: h2

spring.ai.openai.chat.options.model: gpt-4o
```

Database configuration in `src/main/resources/application-h2.yml`:

```yaml
spring.datasource.url: jdbc:h2:mem:testdb
spring.h2.console.enabled: true
spring.jpa.hibernate.ddl-auto: create-drop
```

### H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

### Building

```bash
# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Create bootable JAR
./gradlew bootJar
```

### Running

```bash
# Development mode with hot reload
./gradlew bootRun

# Run JAR
java -jar build/libs/ai-tutor-0.0.1-SNAPSHOT.jar
```

## 🧪 Testing

The project includes comprehensive test coverage with unit, controller, and integration tests.

### Test Structure

```
src/test/kotlin/
├── chat/
│   ├── controller/    # REST API tests (@WebMvcTest)
│   └── service/       # Business logic tests (MockK)
├── vocabulary/
│   └── controller/    # Vocabulary API tests
├── config/            # Test configuration
└── fixtures/          # Test data factories
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ChatControllerTest

# Run with coverage report
./gradlew test jacocoTestReport
```

### Test Categories

- **Controller Tests** (`@WebMvcTest`): Test REST endpoints with MockMvc
- **Service Tests** (MockK): Test business logic with mocked dependencies
- **Integration Tests**: Test complete flows with real database

### Mocking Strategy

- ✅ OpenAI API is mocked in tests (configured in `TestConfig`)
- ✅ Services are mocked in controller tests
- ✅ Use `TestDataFactory` for consistent test data

## 📊 Data Model

### Chat Session
- User ID, tutor configuration (name, persona, domain)
- Language pair (source → target)
- Conversation state (phase, CEFR level)
- Timestamps

### Chat Message
- Session reference
- Role (USER/ASSISTANT)
- Content
- Corrections (JSON)
- Vocabulary (JSON)
- Timestamp

### Vocabulary Item
- User ID, language, lemma
- Exposure count
- Last seen timestamp
- Context examples

## 🎓 Learning Concepts

### CEFR Levels

The system tracks language proficiency using the Common European Framework of Reference:
- **A1/A2**: Beginner
- **B1/B2**: Intermediate
- **C1/C2**: Advanced

### Conversation Phases

- **Free**: Natural conversation, no interruptions (errors tracked silently)
- **Drill**: Active practice with immediate corrections

### Error Types

- **Grammar**: Verb conjugation, agreement, syntax
- **Typography**: Spelling, punctuation, accents
- **WordChoice**: Better word alternatives, register

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with [Spring AI](https://spring.io/projects/spring-ai)
- Powered by [OpenAI](https://openai.com/)
- Language framework based on [CEFR](https://www.coe.int/en/web/common-european-framework-reference-languages)

## 📧 Contact

For questions or support, please open an issue on GitHub.
