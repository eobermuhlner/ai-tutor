# Grafana Troubleshooting Guide

## Issue: `/grafana` endpoint returns frontend app instead of Grafana

### Quick Checks

1. **Verify Grafana container is running on the server:**
```bash
ssh user@ai-tutor-test.obermuhlner.ch
cd ~/test  # or ~/prod
docker-compose ps | grep grafana
```

Expected output should show Grafana running on port 3101 (test) or 3001 (prod).

2. **Check if Grafana is accessible locally on the server:**
```bash
# On the deployment server
curl -I http://localhost:3101  # test environment
# or
curl -I http://localhost:3001  # prod environment
```

Should return Grafana headers, not frontend HTML.

3. **Verify nginx configuration is updated:**
```bash
# On the deployment server
sudo nginx -t  # Test nginx configuration
sudo nginx -s reload  # Reload nginx if test passes
```

4. **Check nginx is using the updated configuration:**
```bash
# On the deployment server
sudo cat /etc/nginx/nginx.conf | grep -A 20 "location /grafana"
```

Should show the Grafana proxy configuration.

### Common Issues

#### Issue 1: Grafana container not running (Missing provisioning files)
**Symptoms:** `docker-compose ps` shows Grafana as exited or not present

**Root Cause:** The Grafana provisioning files weren't copied to the server during deployment.

**Solution - Re-deploy with updated script:**
```bash
# From your local machine
cd deployment
./deploy.sh test  # or prod
```

The updated deploy.sh now copies:
- `prometheus.yml`
- `grafana/provisioning/dashboards/dashboard.yml`
- `grafana/provisioning/dashboards/ai-tutor-dashboard.json`
- `grafana/provisioning/datasources/prometheus.yml`

**Manual Fix (if you can't re-deploy):**
```bash
# On the deployment server
cd ~/test  # or ~/prod

# Create directory structure
mkdir -p grafana/provisioning/dashboards
mkdir -p grafana/provisioning/datasources

# You'll need to manually copy the files from your local machine:
# - grafana/provisioning/dashboards/dashboard.yml
# - grafana/provisioning/dashboards/ai-tutor-dashboard.json
# - grafana/provisioning/datasources/prometheus.yml
# - prometheus.yml

# Then start Grafana
docker-compose up -d grafana prometheus
docker-compose logs -f grafana
```

#### Issue 2: Wrong port in docker-compose
**Symptoms:** Grafana is running but on wrong port

**Solution:** Check `.env` file has correct `GRAFANA_PORT`:
```bash
cat .env | grep GRAFANA_PORT
# Should show:
# GRAFANA_PORT=3101  (for test)
# GRAFANA_PORT=3001  (for prod)
```

If wrong, fix and restart:
```bash
docker-compose down
docker-compose up -d
```

#### Issue 3: nginx.conf not updated on server
**Symptoms:** Grafana container runs but `/grafana` still serves frontend

**Solution:** The `nginx.conf` in `/etc/nginx/` needs to be updated with the new Grafana location blocks.

**Option A: Manual update (if you have sudo access)**
```bash
# Copy the updated nginx.conf from deployment directory
sudo cp ~/test/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t
sudo nginx -s reload
```

**Option B: Re-deploy**
```bash
# From your local machine
cd deployment
./deploy.sh test  # or prod
```

#### Issue 4: Grafana not configured for subpath
**Symptoms:** Grafana loads but CSS/JS are broken, 404 errors

**Solution:** Ensure docker-compose.yml has:
```yaml
environment:
  - GF_SERVER_ROOT_URL=https://ai-tutor-test.obermuhlner.ch/grafana
  - GF_SERVER_SERVE_FROM_SUB_PATH=true
```

Restart Grafana:
```bash
docker-compose restart grafana
```

### Verification Steps

After applying fixes:

1. **Test Grafana locally on server:**
```bash
curl -I http://localhost:3101/
```

2. **Test through nginx:**
```bash
curl -I https://ai-tutor-test.obermuhlner.ch/grafana/
```

3. **Access in browser:**
```
https://ai-tutor-test.obermuhlner.ch/grafana/
```

Login with: admin / (your GRAFANA_ADMIN_PASSWORD)

### Debug Commands

```bash
# Check what's listening on Grafana port
netstat -tlnp | grep 3101  # or 3001 for prod

# Check Grafana container logs
docker-compose logs -f grafana

# Check nginx error logs
sudo tail -f /var/log/nginx/error.log

# Test nginx upstream connectivity
curl -v http://127.0.0.1:3101/  # from server

# Check nginx access logs for /grafana requests
sudo tail -f /var/log/nginx/access.log | grep grafana
```

### Most Likely Solution

Based on the symptoms (frontend app being served instead of Grafana), the most likely issue is that **nginx.conf on the server hasn't been updated yet**.

The deployment script copies `nginx.conf` from the deployment directory to `~/test/nginx.conf` (or `~/prod/nginx.conf`), but the system nginx at `/etc/nginx/nginx.conf` needs to be manually updated or linked.

**To fix:**
1. SSH to the server
2. Copy the updated nginx.conf: `sudo cp ~/test/nginx.conf /etc/nginx/nginx.conf`
3. Test: `sudo nginx -t`
4. Reload: `sudo nginx -s reload`
5. Access: `https://ai-tutor-test.obermuhlner.ch/grafana/`
