#!/bin/bash
# Diagnostic script to check monitoring stack on deployment server

echo "=== Checking Monitoring Stack ==="
echo ""

echo "1. Current directory and files:"
pwd
ls -la | grep -E "docker-compose|prometheus|grafana|\.env"
echo ""

echo "2. Docker Compose services defined:"
docker-compose config --services
echo ""

echo "3. Running containers:"
docker-compose ps
echo ""

echo "4. Check .env file for Grafana/Prometheus vars:"
cat .env | grep -E "GRAFANA|PROMETHEUS"
echo ""

echo "5. Check if Grafana/Prometheus services are in docker-compose.yml:"
grep -E "^\s+(grafana|prometheus):" docker-compose.yml
echo ""

echo "6. Try to start Grafana and Prometheus explicitly:"
docker-compose up -d grafana prometheus
echo ""

echo "7. Wait 5 seconds and check status:"
sleep 5
docker-compose ps grafana prometheus
echo ""

echo "8. Check logs:"
echo "--- Grafana logs (last 20 lines) ---"
docker-compose logs --tail=20 grafana
echo ""
echo "--- Prometheus logs (last 20 lines) ---"
docker-compose logs --tail=20 prometheus
echo ""

echo "9. Check if provisioning files exist:"
ls -la grafana/provisioning/dashboards/ 2>/dev/null || echo "Grafana dashboards directory not found"
ls -la grafana/provisioning/datasources/ 2>/dev/null || echo "Grafana datasources directory not found"
ls -la prometheus.yml 2>/dev/null || echo "prometheus.yml not found"
echo ""

echo "10. Check what's listening on monitoring ports:"
netstat -tlnp 2>/dev/null | grep -E "3101|9101" || echo "Nothing listening on 3101 or 9101"
