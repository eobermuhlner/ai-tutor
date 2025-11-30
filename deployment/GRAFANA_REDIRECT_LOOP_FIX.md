# Fixing Grafana Infinite Redirect Loop

## Problem
Accessing `https://ai-tutor-test.obermuhlner.ch/grafana/` results in infinite redirect loop even though:
- All containers are running healthy
- docker-compose.yml has correct Grafana environment variables
- GitHub Actions workflow deployed successfully

## Root Cause Analysis

The redirect loop happens when Grafana is misconfigured for running behind a reverse proxy. The most common cause is:

**Nginx sends `X-Forwarded-Proto: https` header to Grafana**, which causes Grafana to redirect to HTTPS even though it's configured with `GF_SERVER_PROTOCOL=http`. When nginx receives the redirect, it forwards it to the browser with HTTPS, creating an infinite loop.

Other possible causes:
1. **Grafana container doesn't have updated environment variables** - Container was started with old docker-compose.yml
2. **Nginx configuration not updated** - Passwordless sudo not configured, so nginx config wasn't actually updated
3. **Browser cache** - Browser cached old redirect responses

## The Fix

Remove the `X-Forwarded-Proto` header from nginx's Grafana proxy configuration. This prevents Grafana from thinking it should redirect to HTTPS.

## Solution Steps

### Step 1: Run Diagnostic Script

On the server, run:
```bash
cd ~/test
bash diagnose-grafana-redirect.sh
```

This will check:
- Grafana container environment variables
- Nginx configuration differences
- Grafana direct port access
- Logs for errors

### Step 2: Verify Passwordless Sudo Configuration

The GitHub Actions workflow requires passwordless sudo for nginx updates. Check if configured:

```bash
sudo cat /etc/sudoers.d/aitutor-deploy
```

**Expected content:**
```
aitutor ALL=(ALL) NOPASSWD: /usr/sbin/nginx -t
aitutor ALL=(ALL) NOPASSWD: /usr/sbin/nginx -s reload
aitutor ALL=(ALL) NOPASSWD: /bin/cp /home/aitutor/*/nginx.conf /etc/nginx/nginx.conf
```

**If missing, configure it:**
```bash
sudo visudo -f /etc/sudoers.d/aitutor-deploy
# Add the above lines, save and exit
```

### Step 3: Manually Update Nginx (if needed)

If diagnostic shows nginx.conf differs:

```bash
cd ~/test
sudo cp ~/test/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t
sudo nginx -s reload
```

### Step 4: Force Recreate Grafana with Updated Configuration

Even if docker-compose.yml is updated, the running container may have old env vars:

```bash
cd ~/test
docker compose down grafana
docker compose up -d grafana
```

Wait 10 seconds, then check:
```bash
docker compose ps grafana
docker compose logs grafana | tail -20
```

### Step 5: Verify Environment Variables in Running Container

```bash
docker compose exec grafana env | grep GF_SERVER
```

**Expected output:**
```
GF_SERVER_ROOT_URL=https://ai-tutor-test.obermuhlner.ch/grafana
GF_SERVER_SERVE_FROM_SUB_PATH=true
GF_SERVER_ENFORCE_DOMAIN=false
GF_SERVER_PROTOCOL=http
```

If `GF_SERVER_ENFORCE_DOMAIN` or `GF_SERVER_PROTOCOL` are missing or wrong, the docker-compose.yml on the server is outdated.

### Step 6: Test with Curl (Bypass Browser Cache)

```bash
curl -L -v https://ai-tutor-test.obermuhlner.ch/grafana/ 2>&1 | grep -E "< (Location|HTTP)"
```

**Good response:**
```
< HTTP/2 200
```

**Bad response (redirect loop):**
```
< HTTP/2 302
< Location: /grafana/
< HTTP/2 302
< Location: /grafana/
...
```

### Step 7: Test Direct Port Access (Bypass Nginx)

```bash
curl -v http://localhost:3101/ 2>&1 | grep -E "< (Location|HTTP)"
```

If this also redirects, the issue is in Grafana configuration, not nginx.

### Step 8: Check Grafana Logs for Configuration Issues

```bash
docker compose logs grafana | grep -i -E "root_url|subpath|redirect|enforce"
```

Look for warnings about configuration or redirect behavior.

## Quick Fix Commands (All-in-One)

If you want to fix everything at once:

```bash
cd ~/test

# 1. Download latest docker-compose.yml (in case it's outdated)
curl -s -o docker-compose.yml https://raw.githubusercontent.com/eobermuhlner/ai-tutor/master/deployment/docker-compose.yml

# 2. Update nginx configuration
sudo cp ~/test/nginx.conf /etc/nginx/nginx.conf
sudo nginx -t && sudo nginx -s reload

# 3. Recreate Grafana with latest configuration
docker compose down grafana
docker compose up -d grafana

# 4. Wait for startup
sleep 10

# 5. Verify
docker compose exec grafana env | grep GF_SERVER
docker compose ps grafana
curl -L -v https://ai-tutor-test.obermuhlner.ch/grafana/ 2>&1 | head -20
```

## Verification Checklist

After fixes, verify:

- [ ] `docker compose exec grafana env | grep GF_SERVER_ENFORCE_DOMAIN` shows `false`
- [ ] `docker compose exec grafana env | grep GF_SERVER_PROTOCOL` shows `http`
- [ ] `docker compose exec grafana env | grep GF_SERVER_ROOT_URL` shows correct URL with `/grafana`
- [ ] `docker compose exec grafana env | grep GF_SERVER_SERVE_FROM_SUB_PATH` shows `true`
- [ ] `sudo nginx -t` succeeds
- [ ] `diff ~/test/nginx.conf /etc/nginx/nginx.conf` shows no differences
- [ ] `curl -L https://ai-tutor-test.obermuhlner.ch/grafana/` returns HTTP 200
- [ ] Browser access to `https://ai-tutor-test.obermuhlner.ch/grafana/` shows Grafana login page

## If Still Not Working

1. Check Grafana logs for startup errors:
   ```bash
   docker compose logs grafana | tail -50
   ```

2. Check nginx error logs:
   ```bash
   sudo tail -50 /var/log/nginx/error.log
   ```

3. Verify nginx is listening on correct upstream port:
   ```bash
   netstat -tlnp | grep 3101
   ```

4. Test if backend is blocking access:
   ```bash
   curl -v http://localhost:3101/api/health
   ```

5. Try clearing browser cache completely or use incognito mode

6. Check if there are any firewall rules blocking port 3101

## Prevention

To prevent this issue in future deployments:

1. Ensure passwordless sudo is configured for automated nginx updates
2. Always use `--force-recreate` when updating docker-compose.yml
3. Verify deployment with curl before testing in browser
4. Check GitHub Actions workflow logs for any errors in nginx update step
