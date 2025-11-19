# Deployment Configuration

This document describes the configuration needed for the CI/CD pipeline to deploy the AI Tutor application.

## GitHub Secrets Required

Add these secrets in your GitHub repository settings (Settings > Secrets and variables > Actions):

### Required for Deployment
- `SSH_PRIVATE_KEY`: Private SSH key to access the deployment server
- `DEPLOY_HOST`: Hostname/IP of the deployment server
- `DEPLOY_USER`: Username for SSH access to the deployment server

### Required for Application Functionality
- `OPENAI_API_KEY`: OpenAI API key for runtime
- `JWT_SECRET_TEST`: JWT secret for test environment
- `JWT_SECRET_PROD`: JWT secret for production environment

### Optional for Admin/Demo Users
- `ADMIN_USERNAME`: Admin username (defaults to 'admin')
- `ADMIN_PASSWORD`: Admin password
- `DEMO_USERNAME`: Demo username (defaults to 'demo')
- `DEMO_PASSWORD`: Demo password

## GitHub Environments

Two environments are required for deployments:

### Test Environment
- Name: `test`
- Required variables: None beyond secrets
- Deploys to: `~/test` directory on the server
- Accessible via: `https://ai-tutor-test.obermuhlner.ch`

### Production Environment
- Name: `prod`
- Required variables: None beyond secrets
- Deploys to: `~/prod` directory on the server
- Accessible via: `https://ai-tutor.obermuhlner.ch`

## Deployment Process

### Test Deployment
- Triggered automatically on push/merge to `master` branch
- Builds and pushes images with `:test` tag
- Deploys to test environment

### Production Deployment
- Triggered automatically on release creation (tag pattern `v*`)
- Builds and pushes images with `:latest` and version tag
- Deploys to production environment

## Server Requirements

The deployment server must have:
- Docker and Docker Compose installed
- Nginx for reverse proxy (should be configured separately)
- Proper SSL certificates for HTTPS
- SSH access configured for the deployment user
- Required ports open (80, 443 for web, 22 for SSH)