# Deployment

This directory contains the deployment configuration and scripts for the AI Tutor application.

## Deployment Configuration

- `docker-compose.yml`: Docker Compose configuration for running the application
- `nginx.conf`: Nginx configuration for reverse proxy and SSL termination

## Deployment Script

The `deploy.sh` script allows for manual deployment to test or production environments via SSH.

### Usage

```bash
./deploy.sh [OPTIONS]
```

### Options

- `-e, --environment ENV`: Environment to deploy to (`test` or `prod`) - default: `test`
- `-h, --host HOST`: SSH host for deployment server
- `-u, --user USER`: SSH user for deployment server
- `-k, --key PATH`: Path to SSH private key file or SSH key content directly
- `-r, --repo-owner OWNER`: GitHub repository owner/organization name
- `-t, --token TOKEN`: GitHub token for accessing container registry
- `--help`: Display help message

### Example

```bash
./deploy.sh \
  --environment prod \
  --host your-server.com \
  --user deploy-user \
  --key ~/.ssh/deploy-key \
  --repo-owner your-github-username \
  --token your-github-token
```

### Prerequisites

- Docker and Docker Compose installed on the target server
- SSH access to the deployment server with appropriate permissions
- GitHub Container Registry access with proper authentication
- SSL certificates configured for HTTPS (in nginx.conf)