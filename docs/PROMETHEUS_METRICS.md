# Prometheus Metrics Configuration

This document describes the Prometheus metrics configuration for the AI Tutor application.

## Overview

The AI Tutor backend includes Prometheus metrics support using Spring Boot Actuator and Micrometer. This enables monitoring of application performance, user interactions, and AI service usage.

## Configuration

### Dependencies

The following dependency was added to the `backend/build.gradle` file:

```gradle
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### Actuator Configuration

The application.yml includes the following management configuration:

```yaml
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true  # Enable Prometheus metrics export
  health:
    defaults:
      enabled: true
```

### Security Configuration

The `SecurityConfig` was updated to allow access to all actuator endpoints:

```kotlin
// Actuator endpoints (health, info, metrics, prometheus)
.requestMatchers(
    "/actuator",
    "/actuator/**"
).permitAll()
```

## Custom Metrics

The following custom business metrics are tracked:

- `ai_tutor.ai_requests_total` - Counter for AI requests
  - Tags: `provider` (openai, ollama, anthropic), `model` (optional)
  - Tracks which AI provider is handling requests

- `ai_tutor.ai_request_duration_seconds` - Timer for AI request duration
  - Tags: `provider`, `model` (optional)
  - Measures response time by provider

- `ai_tutor.error_detection_total` - Counter for error detections
  - Tags: `error_type` (GRAMMAR, SPELLING, etc.), `severity` (LOW, MEDIUM, HIGH, CRITICAL)
  - Tracks language errors detected by the tutor

- `ai_tutor.chat_messages_total` - Counter for chat messages
  - Tags: `role` (user, assistant)
  - Tracks message volume by role
  - **Note**: Does NOT include user_id or session_id to prevent high cardinality and privacy issues

These metrics are automatically recorded in:
- `TutorService` - AI requests and responses with provider detection
- `ChatService` - Chat message interactions
- `CorrectionService` - Error detection

All metrics use cached instances to prevent memory leaks and are wrapped in error handling to prevent business logic failures.

## Available Endpoints

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## Usage

### Development

In development, the Prometheus endpoint is available at:
- `http://localhost:8081/actuator/prometheus` (or your configured port)

### Production

**IMPORTANT**: In production, use a separate management port for security:

1. **Activate the production profile**:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   ```

2. **Management endpoints run on port 9090** (internal only):
   - Health: `http://localhost:9090/actuator/health`
   - Prometheus: `http://localhost:9090/actuator/prometheus`

3. **Configure Prometheus to scrape from internal port**:
   ```yaml
   # prometheus.yml
   scrape_configs:
     - job_name: 'ai-tutor'
       static_configs:
         - targets: ['ai-tutor-host:9090']  # Internal network only
   ```

4. **Ensure port 9090 is NOT exposed to the internet** - it should only be accessible from:
   - Prometheus server (internal network)
   - Monitoring infrastructure
   - Trusted internal services

### Security Considerations

- ✅ Management port 9090 is separate from application port
- ✅ No high-cardinality tags (user_id, session_id removed)
- ✅ Health details hidden in production (`show-details: never`)
- ✅ All metrics recording wrapped in error handling
- ✅ Metrics instances cached to prevent memory leaks

**Firewall Configuration**:
- Application port (8080/8081): Public access
- Management port (9090): Internal network only

## Grafana Dashboard

The collected metrics can be visualized using Grafana dashboards. Recommended queries:

### AI Request Rate by Provider
```promql
rate(ai_tutor_ai_requests_total[5m])
```

### AI Request Duration (95th percentile)
```promql
histogram_quantile(0.95, rate(ai_tutor_ai_request_duration_seconds_bucket[5m]))
```

### Error Detection Rate by Type
```promql
rate(ai_tutor_error_detection_total[5m])
```

### Chat Message Volume
```promql
rate(ai_tutor_chat_messages_total[5m])
```

## Verification

After deployment, verify the metrics are working correctly:

```bash
# Check that Prometheus endpoint is accessible (internal network only)
curl http://localhost:9090/actuator/prometheus

# Verify NO high-cardinality tags are present
curl http://localhost:9090/actuator/prometheus | grep -E "user_id|session_id"
# Should return NOTHING

# Verify provider detection is working
curl http://localhost:9090/actuator/prometheus | grep ai_tutor_ai_requests_total
# Should show provider="openai" or "ollama" or "anthropic", NOT "ai_model"
```