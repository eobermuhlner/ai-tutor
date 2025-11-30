#!/bin/bash

# Deployment script for AI Tutor
# Used to deploy the application to test or production environment via SSH

set -e  # Exit on any error

# Default values
ENVIRONMENT="test"
SSH_HOST=""
SSH_USER=""
SSH_KEY_PATH=""
REPO_OWNER=""
GITHUB_TOKEN=""

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  -e, --environment ENV    Environment to deploy to (test/prod) - default: test"
    echo "  -h, --host HOST         SSH host for deployment server"
    echo "  -u, --user USER         SSH user for deployment server"
    echo "  -k, --key PATH          Path to SSH private key"
    echo "  -r, --repo-owner OWNER  GitHub repository owner/organization name"
    echo "  -t, --token TOKEN       GitHub token for accessing container registry"
    echo "  --help                  Display this help message"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -h|--host)
            SSH_HOST="$2"
            shift 2
            ;;
        -u|--user)
            SSH_USER="$2"
            shift 2
            ;;
        -k|--key)
            SSH_KEY_PATH="$2"
            shift 2
            ;;
        -r|--repo-owner)
            REPO_OWNER="$2"
            shift 2
            ;;
        -t|--token)
            GITHUB_TOKEN="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate required parameters
if [ -z "$SSH_HOST" ] || [ -z "$SSH_USER" ] || [ -z "$SSH_KEY_PATH" ] || [ -z "$REPO_OWNER" ] || [ -z "$GITHUB_TOKEN" ]; then
    echo "Error: Missing required parameters"
    usage
fi

# Validate environment
if [ "$ENVIRONMENT" != "test" ] && [ "$ENVIRONMENT" != "prod" ]; then
    echo "Error: Environment must be 'test' or 'prod'"
    exit 1
fi

# Set environment-specific variables
if [ "$ENVIRONMENT" = "test" ]; then
    DEPLOY_DIR="~/test"
    IMAGE_TAG="test"
    BACKEND_PORT=5100
    FRONTEND_PORT=5101
    GRAFANA_PORT=3101
    PROMETHEUS_PORT=9101
    FRONTEND_URL="https://ai-tutor-test.obermuhlner.ch"
    BACKEND_URL="http://localhost:5100/api/v1"
    GRAFANA_ROOT_URL="https://ai-tutor-test.obermuhlner.ch/grafana"
else
    DEPLOY_DIR="~/prod"
    IMAGE_TAG="latest"
    BACKEND_PORT=5000
    FRONTEND_PORT=5001
    GRAFANA_PORT=3001
    PROMETHEUS_PORT=9091
    FRONTEND_URL="https://ai-tutor.obermuhlner.ch"
    BACKEND_URL="http://localhost:5000/api/v1"
    GRAFANA_ROOT_URL="https://ai-tutor.obermuhlner.ch/grafana"
fi

echo "Starting deployment to $ENVIRONMENT environment..."

# Create temporary SSH key file if it's provided as content
TEMP_KEY_FILE=""
if [ -f "$SSH_KEY_PATH" ]; then
    # If SSH_KEY_PATH is already a file path
    chmod 600 "$SSH_KEY_PATH"
    TEMP_KEY_FILE="$SSH_KEY_PATH"
else
    # If SSH_KEY_PATH is the actual key content, write to temporary file
    TEMP_KEY_FILE=$(mktemp)
    echo "$SSH_KEY_PATH" > "$TEMP_KEY_FILE"
    chmod 600 "$TEMP_KEY_FILE"
fi

# Create SSH command with proper options
SSH_CMD="ssh -i $TEMP_KEY_FILE -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"

echo "Connecting to $SSH_USER@$SSH_HOST..."
echo "Deploying to $DEPLOY_DIR"

# Copy deployment files to server
echo "Copying deployment files to server..."
scp -i "$TEMP_KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    ../deployment/docker-compose.yml \
    ../deployment/nginx.conf \
    ../deployment/prometheus.yml \
    "$SSH_USER@$SSH_HOST:$DEPLOY_DIR/"

# Copy Grafana provisioning files (create directory structure first)
echo "Copying Grafana provisioning files..."
ssh -i "$TEMP_KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    "$SSH_USER@$SSH_HOST" "mkdir -p $DEPLOY_DIR/grafana/provisioning/dashboards $DEPLOY_DIR/grafana/provisioning/datasources"

scp -i "$TEMP_KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    ../grafana/provisioning/dashboards/dashboard.yml \
    ../grafana/provisioning/dashboards/ai-tutor-dashboard.json \
    "$SSH_USER@$SSH_HOST:$DEPLOY_DIR/grafana/provisioning/dashboards/"

scp -i "$TEMP_KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    ../grafana/provisioning/datasources/prometheus.yml \
    "$SSH_USER@$SSH_HOST:$DEPLOY_DIR/grafana/provisioning/datasources/"

# Create environment file with the proper configuration
echo "Creating environment file on server..."
cat > .env.tmp << EOL
GITHUB_USERNAME=$REPO_OWNER
IMAGE_TAG=$IMAGE_TAG
UID=1000
GID=1000
BACKEND_PORT=$BACKEND_PORT
FRONTEND_PORT=$FRONTEND_PORT
GRAFANA_PORT=$GRAFANA_PORT
PROMETHEUS_PORT=$PROMETHEUS_PORT
BACKEND_URL=$BACKEND_URL
FRONTEND_URL=$FRONTEND_URL
GRAFANA_ROOT_URL=$GRAFANA_ROOT_URL
JWT_SECRET=\${JWT_SECRET}
OPENAI_API_KEY=\${OPENAI_API_KEY}
ADMIN_USERNAME=\${ADMIN_USERNAME}
ADMIN_PASSWORD=\${ADMIN_PASSWORD}
GRAFANA_ADMIN_PASSWORD=\${GRAFANA_ADMIN_PASSWORD}
DEMO_USERNAME=\${DEMO_USERNAME}
DEMO_PASSWORD=\${DEMO_PASSWORD}
EOL

scp -i "$TEMP_KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
    .env.tmp "$SSH_USER@$SSH_HOST:$DEPLOY_DIR/.env"

# Remove local temp file
rm .env.tmp

# Execute deployment commands on remote server
echo "Executing deployment commands on remote server..."
$SSH_CMD "$SSH_USER@$SSH_HOST" << EOF
set -e
cd $DEPLOY_DIR

# Login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u $REPO_OWNER --password-stdin

# Pull latest images
docker compose pull

# Start or restart containers
docker compose up -d --force-recreate

# Wait for services to be healthy
sleep 30

# Check if services are running
if docker compose ps | grep -E "(backend|frontend).*healthy"; then
    echo "Deployment to $ENVIRONMENT successful!"
    docker compose ps
else
    echo "Some services are not healthy"
    docker compose ps
    exit 1
fi
EOF

# Cleanup temporary key file if we created one
if [ "$TEMP_KEY_FILE" != "$SSH_KEY_PATH" ]; then
    rm "$TEMP_KEY_FILE"
fi

echo "Deployment to $ENVIRONMENT environment completed successfully!"