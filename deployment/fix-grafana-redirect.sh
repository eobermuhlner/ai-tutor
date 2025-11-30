#!/bin/bash
# Quick fix script for Grafana redirect loop issue
# Run this on the deployment server in the ~/test or ~/prod directory

set -e  # Exit on error

ENV_DIR=$(pwd)
echo "=== Fixing Grafana Redirect Loop in $ENV_DIR ==="
echo ""

# Determine if this is test or prod based on directory
if [[ "$ENV_DIR" == *"/test"* ]]; then
    ENV="test"
    GRAFANA_URL="https://ai-tutor-test.obermuhlner.ch/grafana/"
elif [[ "$ENV_DIR" == *"/prod"* ]]; then
    ENV="prod"
    GRAFANA_URL="https://ai-tutor.obermuhlner.ch/grafana/"
else
    echo "Error: Must be run from ~/test or ~/prod directory"
    exit 1
fi

echo "Environment: $ENV"
echo "Grafana URL: $GRAFANA_URL"
echo ""

# Step 1: Download latest docker-compose.yml
echo "Step 1: Downloading latest docker-compose.yml..."
curl -s -o docker-compose.yml https://raw.githubusercontent.com/eobermuhlner/ai-tutor/master/deployment/docker-compose.yml
echo "✓ docker-compose.yml updated"
echo ""

# Step 2: Check if nginx.conf exists and update it
echo "Step 2: Updating nginx configuration..."
if [ -f nginx.conf ]; then
    sudo cp nginx.conf /etc/nginx/nginx.conf
    if sudo nginx -t; then
        sudo nginx -s reload
        echo "✓ Nginx configuration updated and reloaded"
    else
        echo "✗ Nginx configuration test failed!"
        exit 1
    fi
else
    echo "⚠ nginx.conf not found in $ENV_DIR, skipping nginx update"
fi
echo ""

# Step 3: Stop Grafana
echo "Step 3: Stopping Grafana container..."
docker compose down grafana
echo "✓ Grafana stopped"
echo ""

# Step 4: Start Grafana with new configuration
echo "Step 4: Starting Grafana with updated configuration..."
docker compose up -d grafana
echo "✓ Grafana started"
echo ""

# Step 5: Wait for Grafana to be ready
echo "Step 5: Waiting for Grafana to start (10 seconds)..."
sleep 10
echo ""

# Step 6: Verify environment variables
echo "Step 6: Verifying Grafana environment variables..."
echo "--- GF_SERVER variables ---"
docker compose exec grafana env | grep GF_SERVER || echo "⚠ No GF_SERVER variables found!"
echo ""

# Step 7: Check container status
echo "Step 7: Checking Grafana container status..."
docker compose ps grafana
echo ""

# Step 8: Test with curl
echo "Step 8: Testing Grafana endpoint with curl..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -L "$GRAFANA_URL" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Grafana is responding correctly (HTTP $HTTP_CODE)"
else
    echo "⚠ Grafana returned HTTP $HTTP_CODE (expected 200)"
    echo "Response headers:"
    curl -L -v "$GRAFANA_URL" 2>&1 | grep -E "< (Location|HTTP)" | head -10
fi
echo ""

# Step 9: Show recent logs
echo "Step 9: Recent Grafana logs (last 15 lines)..."
docker compose logs --tail=15 grafana
echo ""

echo "=== Fix Complete ==="
echo ""
echo "Next steps:"
echo "1. Clear your browser cache or use incognito mode"
echo "2. Access: $GRAFANA_URL"
echo "3. Login with admin / {GRAFANA_ADMIN_PASSWORD}"
echo ""
echo "If still not working, check:"
echo "- Run 'bash diagnose-grafana-redirect.sh' for detailed diagnostics"
echo "- Check nginx logs: sudo tail -20 /var/log/nginx/error.log"
echo "- Verify passwordless sudo is configured (see GRAFANA_REDIRECT_LOOP_FIX.md)"
