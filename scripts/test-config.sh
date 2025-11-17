#!/bin/bash
# Test script to validate the configuration files

echo "Testing configuration files..."

# Test docker-compose syntax
echo "Testing docker-compose.prod.yml syntax..."
docker-compose -f docker-compose.prod.yml config --quiet
if [ $? -eq 0 ]; then
    echo "✓ docker-compose.prod.yml syntax is valid"
else
    echo "✗ docker-compose.prod.yml syntax error"
    exit 1
fi

# Check if required files exist
FILES=(
    "docker-compose.prod.yml"
    ".env.prod.example" 
    "DEPLOYMENT_PROCESS.md"
    "tasks/task-0014-production-pipeline.md"
    ".github/workflows/docker-build.yml"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
        exit 1
    fi
done

echo "All configuration files validated successfully!"
echo ""
echo "Summary of changes made:"
echo "1. Updated docker-compose.prod.yml to use ghcr.io image references"
echo "2. Updated GitHub Actions workflow to push to GitHub Container Registry"
echo "3. Updated documentation to reflect GHCR usage"
echo "4. Created .env.prod.example with GHCR configuration"
echo ""
echo "To complete the setup:"
echo "1. Update your GitHub repository settings to enable packages"
echo "2. Ensure your GitHub Actions have proper permissions for packages"
echo "3. Update any deployment scripts to use the new configuration"