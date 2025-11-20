# Deployment Process for AI Tutor

## Overview

This document describes how the AI Tutor application is deployed using **GitHub Actions CI/CD** for automated deployments, and provides manual deployment instructions for emergency situations or custom setups.

## Deployment Methods

1. **Automated CI/CD (Primary)**: GitHub Actions automatically builds and deploys on code changes
2. **Manual Deployment (Fallback)**: Step-by-step instructions for manual deployment when CI/CD is unavailable

---

# Automated Deployment (CI/CD)

The AI Tutor uses GitHub Actions for fully automated CI/CD. See [DEPLOYMENT_CONFIG.md](DEPLOYMENT_CONFIG.md) for complete configuration details.

## Quick Reference

### Test Environment
- **Trigger**: Automatic on push to `master` branch
- **Workflow**: `.github/workflows/deploy-test.yml`
- **URL**: https://ai-tutor-test.obermuhlner.ch
- **Deployment**: `~/test` directory on server
- **Ports**: Backend 5100, Frontend 5101

### Production Environment
- **Trigger**: Automatic on GitHub Release creation
- **Workflow**: `.github/workflows/deploy-prod.yml`
- **URL**: https://ai-tutor.obermuhlner.ch
- **Deployment**: `~/prod` directory on server
- **Ports**: Backend 5000, Frontend 5001

## How It Works

1. **Developer** pushes code to `master` or creates a release
2. **GitHub Actions** automatically:
   - Builds Docker images for backend and frontend
   - Pushes images to GitHub Container Registry
   - SSHs into deployment server
   - Pulls latest images
   - Recreates containers with new images
   - Verifies services are healthy
3. **Server** runs the application with new version

## Deployment Workflow Steps

### Test Deployment (Automatic)
```yaml
Trigger: git push origin master

1. Build backend Docker image
2. Build frontend Docker image (with test API URL)
3. Push images as :test and :test-{sha}
4. SSH to server
5. Update ~/test/.env with test configuration
6. docker compose pull
7. docker compose up -d --force-recreate
8. Verify health checks pass
```

### Production Deployment (Automatic)
```yaml
Trigger: GitHub Release (e.g., v1.0.0)

1. Build backend Docker image
2. Build frontend Docker image (with prod API URL)
3. Push images as :latest, :prod-{sha}, :{version}
4. SSH to server
5. Update ~/prod/.env with production configuration
6. docker compose pull
7. docker compose up -d --force-recreate
8. Verify health checks pass
```

## Creating a Production Release

1. **Ensure all tests pass** on master branch
2. **Create a new release** in GitHub:
   - Go to Releases → Draft a new release
   - Create a new tag (e.g., `v1.0.0`)
   - Add release title and description
   - Publish release
3. **GitHub Actions automatically deploys** to production
4. **Monitor deployment** in Actions tab
5. **Verify deployment** at production URL

## Monitoring Deployments

### View Deployment Status
- GitHub → Actions tab
- Check workflow run logs
- View environment deployment history

### Check Server Status
```bash
ssh user@server
cd ~/prod  # or ~/test
docker compose ps
docker compose logs -f
```

## Rollback a Failed Deployment

If a deployment fails or causes issues:

```bash
# SSH into server
ssh user@server
cd ~/prod

# Edit .env to use previous version
nano .env
# Change: IMAGE_TAG=latest
# To: IMAGE_TAG=v1.0.0  (previous working version)

# Restart with old version
docker compose pull
docker compose up -d --force-recreate
```

---

# Manual Deployment (Fallback)

Use these instructions when GitHub Actions CI/CD is unavailable or for custom deployment scenarios.

## Prerequisites
- Linux server (Ubuntu 20.04 LTS or later recommended)
- Domain name pointing to the server IP address
- SSH access to the server with sudo privileges

## System Requirements
- CPU: 2+ cores
- Memory: 4GB+ RAM (8GB recommended)
- Storage: 10GB+ available space
- OS: Linux with kernel 4.0+
- Docker: Version 20.10.0+
- Docker Compose: Version 2.0+

## Step-by-Step Deployment Process

### Step 1: Server Preparation
1. Update the system packages:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

2. Install Docker:
   ```bash
   # Install Docker prerequisites
   sudo apt install ca-certificates curl gnupg lsb-release -y
   
   # Add Docker's official GPG key
   sudo mkdir -p /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   
   # Set up the Docker repository
   echo \
     "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   
   # Update package index and install Docker
   sudo apt update
   sudo apt install docker-ce docker-ce-cli containerd.io docker-compose-plugin
   ```

3. Add your user to the docker group:
   ```bash
   sudo usermod -aG docker $USER
   # Log out and log back in for the changes to take effect
   ```

4. Install additional tools:
   ```bash
   sudo apt install nginx certbot python3-certbot-nginx ufw git -y
   ```

### Step 2: Directory Setup and Configuration Files

1. Create the application directory structure:
   ```bash
   mkdir -p ~/prod/{data,ssl}
   cd ~/prod
   ```

2. Download the docker-compose configuration:
   ```bash
   curl -O https://raw.githubusercontent.com/your-username/ai-tutor/master/deployment/docker-compose.yml
   ```

3. Download the nginx configuration (for reference):
   ```bash
   curl -O https://raw.githubusercontent.com/your-username/ai-tutor/master/deployment/nginx.conf
   ```
   **Note**: Nginx is configured system-wide in `/etc/nginx/nginx.conf`, not per-deployment.

4. Create the environment configuration file:
   ```bash
   nano .env
   ```

   Add the following content, filling in your specific values:
   ```bash
   # Container Registry
   GITHUB_USERNAME=your-github-username
   IMAGE_TAG=latest

   # User/Group IDs (for file permissions)
   UID=1000
   GID=1000

   # Port Configuration
   BACKEND_PORT=5000
   FRONTEND_PORT=5001

   # API URLs
   BACKEND_URL=http://localhost:5000/api/v1
   FRONTEND_URL=https://yourdomain.com

   # Security Configuration
   JWT_SECRET=your-32-character-jwt-secret-key

   # AI Provider Configuration (Choose one or more)
   OPENAI_API_KEY=your-openai-api-key
   # AZURE_OPENAI_API_KEY=your-azure-openai-key
   # AZURE_OPENAI_ENDPOINT=your-azure-openai-endpoint
   # ANTHROPIC_API_KEY=your-anthropic-key

   # User Account Configuration
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=your-secure-admin-password
   DEMO_USERNAME=demo
   DEMO_PASSWORD=your-secure-demo-password
   ```

5. Set appropriate permissions for the environment file:
   ```bash
   chmod 600 .env
   ```

### Step 3: SSL Certificate Setup with Let's Encrypt

1. If using a domain name, stop nginx temporarily:
   ```bash
   sudo systemctl stop nginx
   ```

2. Obtain SSL certificate:
   ```bash
   sudo certbot certonly --standalone -d yourdomain.com
   ```
   
   Replace `yourdomain.com` with your actual domain name.

3. Set up automatic renewal:
   ```bash
   sudo crontab -e
   ```

   Add the following line to renew the certificate automatically:
   ```bash
   0 12 * * * /usr/bin/certbot renew --quiet --post-hook "systemctl reload nginx"
   ```

4. Verify certificates exist:
   ```bash
   sudo ls -la /etc/letsencrypt/live/yourdomain.com/
   ```

### Step 4: Configure Nginx

1. Edit the nginx configuration:
   ```bash
   sudo nano /etc/nginx/nginx.conf
   ```

2. Add a server block for your domain (see `deployment/nginx.conf` for reference):
   ```nginx
   http {
       upstream aitutor_prod_backend {
           server 127.0.0.1:5000;
       }

       upstream aitutor_prod_frontend {
           server 127.0.0.1:5001;
       }

       # HTTP → HTTPS redirect
       server {
           listen 80;
           server_name yourdomain.com;
           return 301 https://$server_name$request_uri;
       }

       # HTTPS server
       server {
           listen 443 ssl http2;
           server_name yourdomain.com;

           # SSL Certificate
           ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
           ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

           # API requests
           location /api/v1 {
               proxy_pass http://aitutor_prod_backend;
               proxy_set_header Host $host;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
           }

           # Frontend
           location / {
               proxy_pass http://aitutor_prod_frontend;
               proxy_set_header Host $host;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
           }
       }
   }
   ```

   Replace `yourdomain.com` with your actual domain name.

3. Test the nginx configuration:
   ```bash
   sudo nginx -t
   ```

4. Restart nginx:
   ```bash
   sudo systemctl restart nginx
   ```

### Step 5: Deploy the Application

1. Ensure you're in the deployment directory:
   ```bash
   cd ~/prod
   ```

2. Pull the latest production images:
   ```bash
   docker compose pull
   ```

3. Start the services:
   ```bash
   docker compose up -d
   ```

4. Verify all services are running:
   ```bash
   docker compose ps
   ```

5. Check the application logs:
   ```bash
   docker compose logs -f
   ```

### Step 6: Firewall Configuration

1. Configure UFW firewall:
   ```bash
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow ssh
   sudo ufw allow 'Nginx Full'
   sudo ufw enable
   ```

### Step 7: Post-Deployment Validation

1. Verify the application is accessible:
   - Visit `https://yourdomain.com` in a web browser
   - Check that the frontend loads without errors
   - Verify API endpoints are accessible

2. Test core functionality:
   - User registration and login
   - Course browsing
   - Conversation functionality
   - Error correction features

3. Verify SSL certificate:
   - Check that the connection is secure
   - Verify SSL certificate details in the browser

4. Test health endpoints:
   ```bash
   curl -k https://yourdomain.com/health
   curl -k https://yourdomain.com/api/v1/actuator/health
   ```

### Step 8: Monitoring Setup (Optional)

1. Set up basic monitoring:
   ```bash
   # Check if services are running
   docker compose ps

   # Monitor logs for issues
   docker compose logs -f --tail=100
   ```

2. Create a simple monitoring script:
   ```bash
   cat > ~/prod/monitor.sh << 'EOF'
   #!/bin/bash
   # Simple monitoring script for AI Tutor

   cd ~/prod

   # Check if all containers are running
   RUNNING_CONTAINERS=$(docker compose ps -q | wc -l)
   TOTAL_CONTAINERS=$(docker compose config --services | wc -l)

   if [ "$RUNNING_CONTAINERS" -ne "$TOTAL_CONTAINERS" ]; then
       echo "$(date): Warning! Not all containers are running"
       docker compose ps
   fi

   # Check if the site is responding
   HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://yourdomain.com)
   if [ "$HTTP_CODE" -ne "200" ]; then
       echo "$(date): Warning! Site returned HTTP $HTTP_CODE"
   fi
   EOF

   chmod +x ~/prod/monitor.sh
   ```

3. Add monitoring to crontab:
   ```bash
   crontab -e
   ```

   Add the following line to check every 5 minutes:
   ```bash
   */5 * * * * ~/prod/monitor.sh >> ~/prod/monitor.log 2>&1
   ```

## Troubleshooting

### Common Issues

1. **SSL Certificate Issues**
   - Check if domain name is correctly pointing to server
   - Verify certificate files exist in `/opt/ai-tutor/ssl/`
   - Ensure nginx is configured to use correct certificate paths

2. **Database Connection Issues**
   - Check data directory: `ls -la ~/prod/data/`
   - Check backend logs for database errors

3. **API Connectivity Issues**
   - Verify CORS settings in the environment file
   - Check nginx proxy configuration
   - Ensure backend is accessible on configured port (5000 prod, 5100 test)

4. **Docker Container Failures**
   - Check container logs: `docker compose logs backend` or `docker compose logs frontend`
   - Verify environment variables are correctly set in `.env`
   - Ensure sufficient disk space is available: `df -h`

### Diagnostic Commands

- Check all services: `docker compose ps`
- View application logs: `docker compose logs -f`
- Check backend health: `curl http://localhost:5000/actuator/health`
- Check frontend health: `curl http://localhost:5001/`
- Check nginx logs: `sudo tail -f /var/log/nginx/error.log`
- Check system resources: `df -h && free -h && top`

## Maintenance Procedures

### Regular Maintenance Tasks

1. **Log Rotation**
   - Set up log rotation for Docker containers
   - Monitor log sizes to prevent disk space issues

2. **System Updates**
   - Regularly update the underlying OS
   - Update Docker and Docker Compose to latest versions
   - Test updates in a staging environment first

3. **Backup Verification**
   - Regularly test backup restoration procedures
   - Monitor backup storage usage
   - Verify backup integrity

### Updating Application

**Note**: With automated CI/CD, updates are typically deployed automatically. Use manual update only if needed.

1. **Before Update**
   - Create a backup of data and configurations
   - Note the current version for rollback if needed

2. **Update Process**
   ```bash
   cd ~/prod

   # Pull new images
   docker compose pull

   # Stop current services
   docker compose down

   # Start services with new images
   docker compose up -d

   # Verify services are running correctly
   docker compose ps
   docker compose logs -f
   ```

3. **Rollback Procedure**
   ```bash
   cd ~/prod

   # Edit .env to use previous version tag
   nano .env
   # Change: IMAGE_TAG=latest
   # To: IMAGE_TAG=v1.0.0  (previous version)

   # Restart with old version
   docker compose pull
   docker compose up -d --force-recreate
   ```

## Security Best Practices

1. **Regular Security Updates**
   - Keep the OS updated with latest security patches
   - Update Docker and Docker Compose regularly
   - Monitor security advisories for the application dependencies

2. **Access Control**
   - Use strong passwords for admin accounts
   - Regularly rotate API keys
   - Implement proper firewall rules

3. **Monitoring**
   - Monitor application logs for suspicious activity
   - Set up alerts for service failures
   - Regularly review access logs

## Summary

### Automated Deployment (Recommended)

The AI Tutor project uses GitHub Actions for automated CI/CD:

- **Test deployments**: Automatic on push to `master`
- **Production deployments**: Automatic on GitHub Release creation
- **No manual intervention** required for typical deployments

See [DEPLOYMENT_CONFIG.md](DEPLOYMENT_CONFIG.md) for complete CI/CD documentation.

### Manual Deployment (Fallback)

Manual deployment instructions above are provided for:
- Initial server setup and configuration
- Emergency situations when CI/CD is unavailable
- Custom deployment scenarios
- Understanding the deployment process

### Key Points

- Docker images are built and pushed to GitHub Container Registry
- Deployment uses Docker Compose with environment-specific `.env` files
- Nginx handles SSL termination and reverse proxying
- Health checks verify successful deployment
- Rollback is simple: change `IMAGE_TAG` in `.env` and restart

For ongoing operations, the automated CI/CD pipeline handles all deployments. Manual procedures are kept for reference and emergency use.