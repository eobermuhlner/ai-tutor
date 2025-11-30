#!/bin/bash
# Diagnostic script to identify Grafana redirect loop issue

echo "=== Grafana Redirect Loop Diagnosis ==="
echo ""

echo "1. Check Grafana container environment variables:"
docker compose exec grafana env | grep -E "GF_SERVER_|GF_SECURITY_" | sort
echo ""

echo "2. Check docker-compose.yml Grafana configuration:"
grep -A 20 "grafana:" docker-compose.yml | grep -E "GF_SERVER_|environment:" -A 1
echo ""

echo "3. Compare nginx.conf on system vs deployment directory:"
if diff -q ~/test/nginx.conf /etc/nginx/nginx.conf > /dev/null; then
    echo "✓ nginx.conf files match"
else
    echo "✗ nginx.conf files DIFFER"
    echo "Differences:"
    diff ~/test/nginx.conf /etc/nginx/nginx.conf | head -20
fi
echo ""

echo "4. Check nginx Grafana location block configuration:"
grep -A 15 "location /grafana/" /etc/nginx/nginx.conf
echo ""

echo "5. Test Grafana endpoint with curl (following redirects):"
curl -L -v https://ai-tutor-test.obermuhlner.ch/grafana/ 2>&1 | grep -E "< (Location|HTTP)" | head -10
echo ""

echo "6. Test Grafana direct port (bypass nginx):"
curl -v http://localhost:3101/ 2>&1 | grep -E "< (Location|HTTP)" | head -10
echo ""

echo "7. Check nginx error log (last 10 lines):"
sudo tail -10 /var/log/nginx/error.log
echo ""

echo "8. Check Grafana logs for redirect or configuration issues:"
docker compose logs --tail=30 grafana | grep -i -E "redirect|root_url|enforce|protocol"
echo ""

echo "9. Verify .env file has correct Grafana settings:"
grep -E "GRAFANA_" .env
echo ""

echo "=== Diagnosis Complete ==="
echo ""
echo "Next steps based on findings:"
echo "- If GF_SERVER_ variables are missing: docker-compose.yml needs update, then 'docker compose up -d --force-recreate grafana'"
echo "- If nginx.conf differs: Run 'sudo cp ~/test/nginx.conf /etc/nginx/nginx.conf && sudo nginx -t && sudo nginx -s reload'"
echo "- If direct port access redirects: Check Grafana container logs for startup errors"
