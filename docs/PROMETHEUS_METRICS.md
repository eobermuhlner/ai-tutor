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

- `ai_tutor.tokens_total` - Counter for token usage
  - Tags: `provider`, `model` (optional), `token_type` (prompt, completion)
  - Tracks token consumption for cost analysis
  - Extracted from AI provider response metadata
  - **Important for cost tracking**: Multiply by provider pricing to calculate costs

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

## Docker Compose Monitoring Setup

### Local Development

The project includes a complete monitoring stack in `docker-compose.yml`:

```bash
docker-compose up -d
```

**Services**:
- **Backend**: `http://localhost:8080` (application) + `http://localhost:8080/actuator/prometheus` (metrics)
- **Prometheus**: `http://localhost:9090` (metrics collection and queries)
- **Grafana**: `http://localhost:3000` (dashboards and visualization)
  - Default credentials: `admin` / `admin` (or set via `GRAFANA_ADMIN_PASSWORD` in `.env`)

**Pre-configured**:
- Prometheus automatically scrapes backend metrics every 30s
- Grafana includes pre-provisioned AI Tutor dashboard with 6 panels:
  - AI Request Rate by Provider
  - AI Request Duration (p95)
  - Token Usage Rate (tokens/minute)
  - Estimated AI Costs (GPT-4o)
  - Chat Message Rate
  - Error Detection Rate by Type

### Production Deployment

For production deployment, use `deployment/docker-compose.yml`:

```bash
cd deployment
docker-compose up -d
```

**Key differences**:
- Backend management port (9090) exposed internally only
- Prometheus on port 9091 (external) to avoid conflicts
- Grafana on port 3001 (external)
- 30-day metrics retention configured
- Management endpoints restricted (health details hidden)

**Environment variables** (create `deployment/.env`):
```bash
GRAFANA_ADMIN_PASSWORD=your-secure-password
PROMETHEUS_RETENTION=30d  # Optional: adjust retention period
```

## Usage

### Development

In development (default), all actuator endpoints are on the main application port:
- `http://localhost:8081/actuator/prometheus` (or your configured port)
- `http://localhost:8081/actuator/health`
- `http://localhost:8081/actuator/metrics`

**This is the current configuration** when running without the `prod` profile.

**With Docker Compose**:
- Backend metrics: `http://localhost:8080/actuator/prometheus`
- Prometheus queries: `http://localhost:9090`
- Grafana dashboards: `http://localhost:3000`

### Production

**IMPORTANT**: In production, use a separate management port for security.

1. **Activate the production profile** (required for port 9090):
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   ./gradlew :backend:bootRun
   ```

2. **With prod profile, management endpoints run on port 9090** (internal only):
   - Health: `http://localhost:9090/actuator/health`
   - Prometheus: `http://localhost:9090/actuator/prometheus`
   - Main application remains on port 8080/8081

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

## Grafana Dashboards

### Pre-Configured Dashboard

When using Docker Compose, Grafana is automatically provisioned with the "AI Tutor Monitoring" dashboard. Access it at:
- Local: `http://localhost:3000` (username: `admin`, password: `admin`)
- Production: `http://your-host:3001` (username: `admin`, password: from `GRAFANA_ADMIN_PASSWORD`)

The dashboard includes 6 pre-configured panels tracking all key metrics. No manual configuration needed!

### Custom Queries

For custom dashboards or panels, use these PromQL queries:

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

### Token Usage by Provider (for cost analysis)
```promql
# Total tokens per provider
sum(ai_tutor_tokens_total) by (provider)

# Prompt vs Completion tokens
sum(ai_tutor_tokens_total) by (provider, token_type)

# Token usage rate (tokens/minute)
rate(ai_tutor_tokens_total[5m]) * 60
```

### Estimated Cost Calculation
```promql
# Example for OpenAI GPT-4o pricing ($5/1M input, $15/1M output)
(
  sum(ai_tutor_tokens_total{provider="openai",model="gpt-4o",token_type="prompt"}) * 0.000005 +
  sum(ai_tutor_tokens_total{provider="openai",model="gpt-4o",token_type="completion"}) * 0.000015
)
```

## Verification

After deployment, verify the metrics are working correctly:

```bash
# DEVELOPMENT (direct backend)
curl http://localhost:8081/actuator/prometheus

# DEVELOPMENT (Docker Compose)
curl http://localhost:8080/actuator/prometheus

# PRODUCTION (port 9090 - requires prod profile)
curl http://localhost:9090/actuator/prometheus

# Verify NO high-cardinality tags are present
curl http://localhost:8080/actuator/prometheus | grep -E "user_id|session_id"
# Should return NOTHING

# Verify provider detection is working
curl http://localhost:8080/actuator/prometheus | grep ai_tutor_ai_requests_total
# Should show provider="openai" or "ollama" or "anthropic", NOT "ai_model"

# Verify token usage is being tracked
curl http://localhost:8080/actuator/prometheus | grep ai_tutor_tokens_total
# Should show token_type="prompt" and token_type="completion" with counts

# Check Prometheus is scraping successfully
curl http://localhost:9090/api/v1/query?query=up
# Should show "value": [timestamp, "1"] for ai-tutor-backend job

# Verify Grafana dashboard is accessible
curl -I http://localhost:3000
# Should return HTTP 200
```

## Cost Monitoring

Token usage metrics enable cost tracking and budget management:

1. **Track daily costs**:
   - Monitor `ai_tutor_tokens_total` by provider and token_type
   - Apply provider pricing (e.g., OpenAI: $5/1M input, $15/1M output)

2. **Set up alerts**:
   - Alert when daily token usage exceeds budget
   - Alert when cost per request is unusually high

3. **Optimize costs**:
   - Compare costs across providers
   - Identify expensive queries/sessions
   - Track impact of prompt engineering changes

## Troubleshooting

### Prometheus Not Scraping Metrics

**Symptom**: Prometheus UI shows target as "DOWN" or "no data"

**Solutions**:
1. Check backend is running: `curl http://localhost:8080/actuator/prometheus`
2. Check Prometheus config: `docker exec ai-tutor-prometheus cat /etc/prometheus/prometheus.yml`
3. Check Prometheus logs: `docker logs ai-tutor-prometheus`
4. Verify network connectivity: `docker exec ai-tutor-prometheus wget -O- http://backend:8080/actuator/prometheus`

### Grafana Dashboard Shows No Data

**Symptom**: Grafana panels show "No Data"

**Solutions**:
1. Check Prometheus datasource: Grafana → Configuration → Data Sources → Test
2. Verify Prometheus has data: `curl http://localhost:9090/api/v1/query?query=ai_tutor_ai_requests_total`
3. Check time range in dashboard (default is last 1 hour)
4. Generate some traffic to create metrics: send chat messages in the application
5. Check Grafana logs: `docker logs ai-tutor-grafana`

### Metrics Endpoint Returns 403

**Symptom**: `/actuator/prometheus` returns HTTP 403 Forbidden

**Solutions**:
1. Verify `management.metrics.export.prometheus.enabled: true` in application.yml
2. Check SecurityConfig permits actuator endpoints
3. Restart backend after configuration changes

### Token Metrics Always Zero

**Symptom**: `ai_tutor_tokens_total` shows 0 for all providers

**Solutions**:
1. Verify AI provider returns token usage in response metadata
2. Check logs for "Could not extract token usage" warnings
3. Some providers (Ollama) may not report token usage - this is expected
4. Send test messages and check: `curl http://localhost:8080/actuator/prometheus | grep ai_tutor_tokens_total`

### Grafana Dashboard Not Auto-Loading

**Symptom**: Dashboard doesn't appear in Grafana after Docker Compose start

**Solutions**:
1. Verify provisioning files are mounted: `docker exec ai-tutor-grafana ls /etc/grafana/provisioning/dashboards`
2. Check Grafana logs for provisioning errors: `docker logs ai-tutor-grafana | grep -i provision`
3. Manually import dashboard from `grafana/provisioning/dashboards/ai-tutor-dashboard.json`
4. Restart Grafana: `docker-compose restart grafana`

### Port Conflicts

**Symptom**: Docker Compose fails with "port is already in use"

**Solutions**:
1. Local development ports in use:
   - Change ports in `docker-compose.yml` or `.env` file
   - Stop conflicting services: `lsof -i :9090` to find process

2. Production deployment ports in use:
   - Set custom ports in `deployment/.env`:
     ```bash
     PROMETHEUS_PORT=9091
     GRAFANA_PORT=3001
     ```

## Additional Resources

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Query Documentation](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/best-practices/)