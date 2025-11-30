# Deployment Configuration

This document describes the configuration for the GitHub Actions CI/CD pipeline that deploys the AI Tutor application.

## CI/CD Pipeline Overview

The project uses GitHub Actions with the following workflows:

- **CI** (`.github/workflows/ci.yml`): Runs tests and builds on every push/PR to master/develop
- **Deploy to Test** (`.github/workflows/deploy-test.yml`): Automatically deploys to test environment on push to master
- **Deploy to Production** (`.github/workflows/deploy-prod.yml`): Automatically deploys to production on release creation
- **Docker Build** (`.github/workflows/docker-build.yml`): Manual workflow for building Docker images on-demand

## GitHub Secrets Required

Add these secrets in your GitHub repository settings (Settings > Secrets and variables > Actions):

### Required for Deployment
- `SSH_PRIVATE_KEY`: Private SSH key to access the deployment server
- `DEPLOY_HOST`: Hostname/IP of the deployment server (e.g., `example.com`)
- `DEPLOY_USER`: Username for SSH access to the deployment server (e.g., `deploy`)

### Required for Application Functionality
- `OPENAI_API_KEY`: OpenAI API key for runtime
- `JWT_SECRET_TEST`: JWT secret for test environment (32+ characters)
- `JWT_SECRET_PROD`: JWT secret for production environment (32+ characters)

### Optional for Admin/Demo Users
- `ADMIN_USERNAME`: Admin username (defaults to 'admin' if not set)
- `ADMIN_PASSWORD`: Admin password (required for production)
- `DEMO_USERNAME`: Demo username (defaults to 'demo' if not set)
- `DEMO_PASSWORD`: Demo password (required for production)

### Optional for Monitoring
- `GRAFANA_ADMIN_PASSWORD`: Grafana admin password (required for production, defaults to 'admin' if not set)

### Optional Secrets (Auto-Generated)
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions for GHCR authentication
- `CODECOV_TOKEN`: Optional, for uploading test coverage reports to Codecov

## GitHub Environments

Two environments must be configured in GitHub (Settings > Environments):

### Test Environment
- **Name**: `test`
- **Protection rules**: None (deploys automatically on master push)
- **Deployment target**: `~/test` directory on server
- **URLs**:
  - Application: `https://ai-tutor-test.obermuhlner.ch`
  - Grafana: `https://ai-tutor-test.obermuhlner.ch/grafana`
- **Ports**: Backend: 5100, Frontend: 5101, Grafana: 3101, Prometheus: 9101
- **Image tags**: `:test` and `:test-{sha}`

### Production Environment
- **Name**: `prod`
- **Protection rules**: Recommended (require approvals, restrict to release tags)
- **Deployment target**: `~/prod` directory on server
- **URLs**:
  - Application: `https://ai-tutor.obermuhlner.ch`
  - Grafana: `https://ai-tutor.obermuhlner.ch/grafana`
- **Ports**: Backend: 5000, Frontend: 5001, Grafana: 3001, Prometheus: 9091
- **Image tags**: `:latest`, `:prod-{sha}`, `:{version}`

## Deployment Process

### Automatic Test Deployment

**Trigger**: Push or merge to `master` branch

**Workflow**: `.github/workflows/deploy-test.yml`

**Steps**:
1. Checkout code from repository
2. Build backend Docker image (`backend/Dockerfile`)
3. Build frontend Docker image (`frontend/Dockerfile`)
4. Push images to GitHub Container Registry with `:test` tag
5. SSH into deployment server
6. Pull latest `:test` images
7. Recreate containers with new images
8. Verify all services are healthy

**Docker images pushed**:
- `ghcr.io/{owner}/ai-tutor-backend:test`
- `ghcr.io/{owner}/ai-tutor-backend:test-{sha}`
- `ghcr.io/{owner}/ai-tutor-frontend:test`
- `ghcr.io/{owner}/ai-tutor-frontend:test-{sha}`

### Automatic Production Deployment

**Trigger**: GitHub Release creation (e.g., tag `v1.0.0`)

**Workflow**: `.github/workflows/deploy-prod.yml`

**Steps**:
1. Checkout code from repository
2. Build backend Docker image
3. Build frontend Docker image with production API URL
4. Push images to GitHub Container Registry with multiple tags
5. SSH into deployment server
6. Pull latest `:latest` images
7. Recreate containers with new images
8. Verify all services are healthy

**Docker images pushed**:
- `ghcr.io/{owner}/ai-tutor-backend:latest`
- `ghcr.io/{owner}/ai-tutor-backend:prod-{sha}`
- `ghcr.io/{owner}/ai-tutor-backend:{version}` (e.g., `v1.0.0`)
- `ghcr.io/{owner}/ai-tutor-frontend:latest`
- `ghcr.io/{owner}/ai-tutor-frontend:prod-{sha}`
- `ghcr.io/{owner}/ai-tutor-frontend:{version}`

### Manual Docker Build

**Trigger**: Manual workflow dispatch from GitHub Actions UI

**Workflow**: `.github/workflows/docker-build.yml`

**Options**:
- **Environment**: Choose development/staging/production
- **Push images**: Whether to push to registry or build only

**Use case**: Testing Docker builds without deployment

## CI Workflow

**Trigger**: Push or PR to `master` or `develop` branches

**Workflow**: `.github/workflows/ci.yml`

**Backend CI**:
1. Set up JDK 17
2. Build backend with Gradle
3. Run tests with JaCoCo coverage
4. Verify coverage thresholds
5. Upload coverage to Codecov (optional)
6. Upload JAR artifacts

**Frontend CI**:
1. Set up Node.js 20
2. Install dependencies
3. Run linter
4. Run tests with coverage
5. Upload coverage to Codecov (optional)
6. Build production bundle
7. Upload build artifacts

## Server Requirements

The deployment server must have:

### Software
- **OS**: Ubuntu 20.04 LTS or later (or any Linux with systemd)
- **Docker**: Version 20.10.0+ with Docker Compose V2
- **Nginx**: For reverse proxy and SSL termination
- **Certbot**: For Let's Encrypt SSL certificate management
- **SSH**: OpenSSH server configured

### Network
- **Ports open**: 22 (SSH), 80 (HTTP), 443 (HTTPS)
- **Internal ports**: 5000, 5001 (prod), 5100, 5101 (test)
- **DNS**: Domain names pointing to server IP

### Permissions
- Deployment user must be in `docker` group
- SSH key authentication configured
- Write access to `~/test` and `~/prod` directories

## Container Registry

Docker images are stored in **GitHub Container Registry (ghcr.io)**:
- Public visibility (can be changed to private)
- Authenticated via `GITHUB_TOKEN` during workflows
- Server pulls images without authentication (public images)

## Environment Variables on Server

The GitHub Actions workflows automatically create `.env` files on the server with these variables:

```bash
GITHUB_USERNAME={repository_owner}
IMAGE_TAG=test|latest
UID=1000
GID=1000
BACKEND_PORT=5100|5000
FRONTEND_PORT=5101|5001
BACKEND_URL=http://localhost:{port}/api/v1
FRONTEND_URL=https://{domain}
JWT_SECRET={from_secrets}
OPENAI_API_KEY={from_secrets}
ADMIN_USERNAME={from_secrets}
ADMIN_PASSWORD={from_secrets}
DEMO_USERNAME={from_secrets}
DEMO_PASSWORD={from_secrets}
```

## Deployment Files on Server

The workflows automatically download deployment configuration from GitHub:

- **`docker-compose.yml`**: Downloaded from `deployment/docker-compose.yml`
- **`nginx.conf`**: Downloaded from `deployment/nginx.conf` (reference only)
- **`.env`**: Generated dynamically by workflow

**Note**: Nginx is configured system-wide (`/etc/nginx/nginx.conf`), not per-deployment.

## Health Checks

After deployment, the workflow verifies services are healthy:

```bash
docker compose ps | grep -E "(backend|frontend).*healthy"
```

Services must pass their health checks:
- **Backend**: `http://localhost:8080/actuator/health`
- **Frontend**: `http://localhost/` (curl check)

If health checks fail after 30 seconds, deployment is considered failed.

## Rollback Procedure

To rollback a failed deployment:

1. **SSH into server**:
   ```bash
   ssh {DEPLOY_USER}@{DEPLOY_HOST}
   cd ~/prod  # or ~/test
   ```

2. **Update .env to use previous version**:
   ```bash
   # For production, change IMAGE_TAG
   nano .env
   # Change: IMAGE_TAG=latest
   # To: IMAGE_TAG=v1.0.0  (previous version)
   ```

3. **Restart services**:
   ```bash
   docker compose pull
   docker compose up -d --force-recreate
   ```

## Monitoring Deployments

### View Workflow Runs
- GitHub Repository → Actions tab
- View logs for each workflow step
- Download artifacts (JAR, test results, coverage reports)

### View Deployment Status
- GitHub Repository → Environments
- See deployment history per environment
- View active deployments

### Check Server Status
```bash
ssh {DEPLOY_USER}@{DEPLOY_HOST}
cd ~/prod  # or ~/test
docker compose ps
docker compose logs -f
```

## Troubleshooting

### Deployment fails with "Permission denied"
- Verify SSH key is correct in GitHub Secrets
- Check deployment user has docker group membership
- Verify write access to deployment directories

### Images fail to pull
- Check GitHub Container Registry visibility (should be public)
- Verify image names match in docker-compose.yml
- Check network connectivity from server

### Services unhealthy after deployment
- Check backend logs: `docker compose logs backend`
- Check frontend logs: `docker compose logs frontend`
- Verify environment variables in `.env`
- Test health endpoints manually

### SSL certificate errors
- Verify nginx configuration points to correct certificate paths
- Check Let's Encrypt certificate renewal
- Ensure domains resolve to correct server IP