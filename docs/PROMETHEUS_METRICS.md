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

- `ai_tutor.ai_requests_total` - Counter for AI requests with provider and model tags
- `ai_tutor.ai_request_duration_seconds` - Timer for AI request duration with provider and model tags
- `ai_tutor.error_detection_total` - Counter for error detections with error type and severity tags
- `ai_tutor.chat_messages_total` - Counter for chat messages with user ID, session ID, and role tags

These metrics are automatically recorded in:
- `TutorService` - For AI requests and responses
- `ChatService` - For chat message interactions
- `CorrectionService` - For error detection

## Available Endpoints

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## Usage

Once the application is running with appropriate profiles (e.g., `noauth` profile for development), the Prometheus endpoint will provide metrics in the standard Prometheus format.

For production environments, consider:
1. Using a separate management port for metrics
2. Implementing appropriate authentication for the metrics endpoint
3. Configuring the Prometheus server to scrape metrics from the endpoint

## Dashboard

The collected metrics can be visualized using Grafana dashboards or other Prometheus-compatible monitoring tools.