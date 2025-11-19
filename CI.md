# CI/CD Documentation

This document describes the Continuous Integration and Continuous Deployment setup for the AI Tutor project.

## Overview

The project uses GitHub Actions for CI/CD automation with two main workflows:

1. **CI Pipeline** (`.github/workflows/ci.yml`) - Automated testing and validation on every push/PR
2. **Docker Build Pipeline** (`.github/workflows/docker-build.yml`) - Manual Docker image building and publishing

## CI Pipeline

### Triggers

The CI pipeline runs automatically on:
- Pull requests to `master` or `develop` branches
- Direct pushes to `master` or `develop` branches

### Jobs

#### Backend CI

Runs on Ubuntu with Java 17 and includes:

1. **Build**: Compiles the Kotlin code and runs Gradle build
2. **Test**: Executes all unit tests
3. **Coverage**: Generates JaCoCo coverage report
4. **Coverage Verification**: Ensures 80% minimum coverage threshold
5. **Upload**: Sends coverage report to Codecov
6. **Artifacts**: Uploads backend JAR and test results

**Commands:**
```bash
./gradlew :backend:build
./gradlew :backend:jacocoTestReport
./gradlew :backend:jacocoTestCoverageVerification
```

#### Frontend CI

Runs on Ubuntu with Node.js 20 and includes:

1. **Install**: Installs npm dependencies with `npm ci`
2. **Lint**: Runs ESLint checks
3. **Test**: Executes Vitest tests with coverage
4. **Build**: Creates production build with Vite
5. **Upload**: Sends coverage report to Codecov
6. **Artifacts**: Uploads build artifacts and test results

**Commands:**
```bash
npm ci
npm run lint
npm run test:coverage
npm run build
```

### Caching

Both jobs use caching to speed up builds:
- **Backend**: Gradle wrapper and dependency cache
- **Frontend**: npm dependency cache

### Status Check

The `status-check` job aggregates results from both backend and frontend jobs, ensuring all CI checks pass before allowing merge.

## Docker Build Pipeline

### Triggers

This pipeline is manually triggered via GitHub Actions UI with two inputs:

- **Environment**: Choose `development`, `staging`, or `production`
- **Push Images**: Boolean flag to push images to Docker Hub (default: false)

### Jobs

#### Build Backend

Builds a multi-stage Docker image for the Spring Boot backend:

1. **Stage 1 (Builder)**: Gradle build with JDK 17
2. **Stage 2 (Runtime)**: Eclipse Temurin JRE 17 Alpine with optimizations

**Image Tags:**
- `${DOCKERHUB_USERNAME}/ai-tutor-backend:${environment}`
- `${DOCKERHUB_USERNAME}/ai-tutor-backend:${environment}-${git-sha}`
- `${DOCKERHUB_USERNAME}/ai-tutor-backend:latest` (production only)

#### Build Frontend

Builds a multi-stage Docker image for the React frontend:

1. **Stage 1 (Builder)**: Node 20 Alpine with npm build
2. **Stage 2 (Runtime)**: nginx Alpine with custom configuration

**Image Tags:**
- `${DOCKERHUB_USERNAME}/ai-tutor-frontend:${environment}`
- `${DOCKERHUB_USERNAME}/ai-tutor-frontend:${environment}-${git-sha}`
- `${DOCKERHUB_USERNAME}/ai-tutor-frontend:latest` (production only)

### Multi-Architecture Support

Both images are built for `linux/amd64` and `linux/arm64` architectures using Docker Buildx.

## Required GitHub Secrets

Configure these secrets in your GitHub repository settings (Settings > Secrets and variables > Actions):

### Required for Docker Build

- **`GITHUB_TOKEN`**: GitHub token with package access permissions (read:packages, write:packages)
  - Automatically provided by GitHub Actions with appropriate permissions

### Required for Coverage Reporting

- **`CODECOV_TOKEN`**: Codecov upload token
  - Get from: https://codecov.io/ (after connecting your repository)

### Optional for Deployment

- **`OPENAI_API_KEY`**: OpenAI API key for runtime tests
- **`JWT_SECRET_PROD`**: Production JWT secret
- **`AWS_ACCESS_KEY_ID`**: AWS credentials (if deploying to AWS)
- **`AWS_SECRET_ACCESS_KEY`**: AWS secret key

## Local Docker Development

### Prerequisites

- Docker Desktop installed
- Docker Compose v2+
- `.env` file in project root with required variables

### Environment Setup

Create a `.env` file in the project root:

```bash
# Required
OPENAI_API_KEY=your-openai-api-key

# Optional (uses defaults if not set)
JWT_SECRET=your-jwt-secret
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
DEMO_USERNAME=demo
DEMO_PASSWORD=demo
```

### Commands

**Start all services:**
```bash
docker-compose up
```

**Start in detached mode:**
```bash
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

**Rebuild after code changes:**
```bash
docker-compose up --build
```

**Stop all services:**
```bash
docker-compose down
```

**Stop and remove volumes:**
```bash
docker-compose down -v
```

### Access URLs

When running with Docker Compose:

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## Frontend Testing

### Running Tests Locally

```bash
cd frontend

# Run tests in watch mode
npm run test

# Run tests with coverage
npm run test:coverage

# Run tests with UI
npm run test:ui
```

### Coverage Thresholds

The project enforces minimum coverage thresholds:
- **Lines**: 80%
- **Branches**: 70%
- **Functions**: 70%
- **Statements**: 80%

### Writing Tests

Example test structure (see `frontend/src/components/ui/Button.test.tsx`):

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Component from './Component';

describe('Component', () => {
  it('renders correctly', () => {
    render(<Component />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });
});
```

## Backend Testing

### Running Tests Locally

```bash
# Run all tests
./gradlew :backend:test

# Run with coverage
./gradlew :backend:test :backend:jacocoTestReport

# Run specific test class
./gradlew :backend:test --tests ChatControllerTest

# Run test harness (pedagogical quality checks)
./gradlew :backend:runTestHarness
```

### Coverage Requirements

JaCoCo enforces 80% minimum coverage with exclusions for:
- DTOs and domain models
- CLI and conversation service (covered by integration tests)

## CI Badge Integration

Add status badges to your README.md:

### CI Status Badge

```markdown
![CI](https://github.com/YOUR_USERNAME/ai-tutor/workflows/CI/badge.svg)
```

### Codecov Badge

```markdown
[![codecov](https://codecov.io/gh/YOUR_USERNAME/ai-tutor/branch/master/graph/badge.svg)](https://codecov.io/gh/YOUR_USERNAME/ai-tutor)
```

## Troubleshooting

### CI Failures

**Backend build fails:**
1. Check Java version is 17
2. Verify Gradle wrapper permissions: `chmod +x gradlew`
3. Check for test failures in logs

**Frontend build fails:**
1. Check Node.js version is 20
2. Verify package-lock.json is committed
3. Check for linting errors

**Coverage below threshold:**
1. Review coverage report in artifacts
2. Add tests for uncovered code
3. Check exclusions in JaCoCo config

### Docker Build Failures

**Out of memory:**
1. Increase Docker memory limit in Docker Desktop settings
2. Use `--no-daemon` flag in Gradle build

**Image push fails:**
1. Verify DOCKERHUB_USERNAME and DOCKERHUB_TOKEN secrets
2. Ensure Docker Hub repository exists
3. Check token has push permissions

**Multi-arch build fails:**
1. Ensure Docker Buildx is enabled
2. Run `docker buildx create --use` locally

### Local Docker Issues

**Backend doesn't start:**
1. Check OPENAI_API_KEY is set in .env
2. Verify port 8080 is not in use
3. Check backend logs: `docker-compose logs backend`

**Frontend can't reach backend:**
1. Ensure backend health check passes
2. Verify VITE_API_URL is correct
3. Check network connectivity: `docker-compose ps`

**Course content not loading:**
1. Verify course-content volume mount in docker-compose.yml
2. Check file permissions in mounted volume

## Deployment Workflows

The project now includes automated deployment workflows:

1. **Test Environment Deployment** (`.github/workflows/deploy-test.yml`)
   - Trigger: Push/merge to `master` branch
   - Target: Test environment at `ai-tutor-test.obermuhlner.ch`
   - Directory: `~/test` on deployment server
   - Images: Tagged with `:test` suffix

2. **Production Environment Deployment** (`.github/workflows/deploy-prod.yml`)
   - Trigger: Release creation (tag pattern like `v*`)
   - Target: Production environment at `ai-tutor.obermuhlner.ch`
   - Directory: `~/prod` on deployment server
   - Images: Tagged with `:latest` and version tag

### Required GitHub Secrets for Deployment

Configure these secrets in your GitHub repository settings:

- **`SSH_PRIVATE_KEY`**: Private SSH key for server access
- **`DEPLOY_HOST`**: Hostname of the deployment server
- **`DEPLOY_USER`**: SSH username for the deployment server
- **`JWT_SECRET_TEST`**: JWT secret for test environment
- **`JWT_SECRET_PROD`**: JWT secret for production environment
- **`OPENAI_API_KEY`**: OpenAI API key for runtime
- **`ADMIN_USERNAME`**: Admin username (optional)
- **`ADMIN_PASSWORD`**: Admin password (optional)
- **`DEMO_USERNAME`**: Demo username (optional)
- **`DEMO_PASSWORD`**: Demo password (optional)

### Required GitHub Environments

- **`test`**: For test environment deployment
- **`prod`**: For production environment deployment

## Future Enhancements

Potential additions to the CI/CD pipeline:

1. **Integration Tests**
   - Run pedagogical test harness in CI
   - Add Cypress/Playwright E2E tests

2. **Security Scanning**
   - Add Trivy for container vulnerability scanning
   - Add Snyk for dependency scanning
   - Add SonarQube for code quality

3. **Performance Testing**
   - Lighthouse CI for frontend performance
   - Load testing for backend API

4. **Additional Deployment Options**
   - AWS ECS/Fargate deployment
   - Azure App Service deployment
   - Kubernetes manifests

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Documentation](https://docs.docker.com/)
- [Codecov Documentation](https://docs.codecov.com/)
- [Vitest Documentation](https://vitest.dev/)
- [JaCoCo Documentation](https://www.jacoco.org/)
