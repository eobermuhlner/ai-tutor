# Multi-stage build for AI Tutor Backend

# Stage 1: Build
FROM gradle:8.14.3-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Copy backend source
COPY backend/build.gradle backend/
COPY backend/src backend/src/

# Build the application (skip tests for faster builds, tests run in CI)
RUN ./gradlew :backend:bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

# Add metadata labels
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${VCS_REF}"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.title="AI Tutor Backend"
LABEL org.opencontainers.image.description="Spring Boot REST API for AI-powered language tutoring"
LABEL org.opencontainers.image.vendor="AI Tutor"

# Create non-root user for security
RUN groupadd --system appgroup \
 && useradd --system --create-home --gid appgroup appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/backend/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
