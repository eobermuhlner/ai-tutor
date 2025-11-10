# Production Deployment Process for AI Tutor

## Overview
This document provides a step-by-step guide to deploy the AI Tutor application to a Linux server using Docker Compose with nginx as a reverse proxy and SSL termination.

## Prerequisites
- Linux server (Ubuntu 20.04 LTS or later recommended)
- Domain name pointing to the server IP address
- SSH access to the server
- Sudo privileges on the server

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
   sudo mkdir -p /opt/ai-tutor/{data,course-content,ssl,logs}
   sudo chown -R $USER:$USER /opt/ai-tutor
   cd /opt/ai-tutor
   ```

2. Create the production docker-compose file:
   ```bash
   # This file should have been created already as part of this deployment pipeline
   # If not, create docker-compose.prod.yml with the production configuration
   ```

3. Create the nginx configuration file:
   ```bash
   # This file should have been created already as part of this deployment pipeline
   # If not, create nginx.prod.conf with the SSL termination configuration
   ```

4. Create the environment configuration file:
   ```bash
   nano .env
   ```
   
   Add the following content, filling in your specific values:
   ```bash
   # GitHub Container Registry configuration
   GITHUB_USERNAME=your-github-username

   # AI Provider Configuration (Choose one or more)
   OPENAI_API_KEY=your-openai-api-key
   # AZURE_OPENAI_API_KEY=your-azure-openai-key
   # AZURE_OPENAI_ENDPOINT=your-azure-openai-endpoint
   # ANTHROPIC_API_KEY=your-anthropic-key

   # Security Configuration
   JWT_SECRET=your-32-character-jwt-secret-key
   ENCRYPTION_KEY=your-32-character-encryption-key

   # User Account Configuration
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=your-secure-admin-password
   DEMO_USERNAME=demo
   DEMO_PASSWORD=your-secure-demo-password

   # Application Configuration
   DOMAIN_NAME=yourdomain.com
   FRONTEND_URL=https://yourdomain.com
   BACKEND_URL=https://yourdomain.com/api/v1

   # CEFR and Summarization Configuration
   CEFR_LEVEL_MIN_TURNS=3
   PROGRESSIVE_SUMMARIZATION_ENABLED=true
   SUMMARIZATION_CHUNK_SIZE=10
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
   0 12 * * * /usr/bin/certbot renew --quiet
   ```

4. Copy the SSL certificates to the application directory:
   ```bash
   sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem /opt/ai-tutor/ssl/
   sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem /opt/ai-tutor/ssl/
   ```

### Step 4: Configure Nginx

1. Create the nginx configuration:
   ```bash
   sudo nano /etc/nginx/nginx.conf
   ```

2. Add the content from nginx.prod.conf, updating the server_name to your domain:
   ```
   events {
       worker_connections 1024;
   }

   http {
       upstream backend {
           server ai-tutor-backend:8080;
       }

       upstream frontend {
           server ai-tutor-frontend:80;
       }

       # SSL Configuration
       ssl_protocols TLSv1.2 TLSv1.3;
       ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
       ssl_prefer_server_ciphers off;
       ssl_session_cache shared:SSL:10m;
       ssl_session_timeout 10m;

       # Gzip Compression
       gzip on;
       gzip_vary on;
       gzip_min_length 1024;
       gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/javascript application/json;

       # Security Headers
       add_header X-Frame-Options "SAMEORIGIN" always;
       add_header X-Content-Type-Options "nosniff" always;
       add_header X-XSS-Protection "1; mode=block" always;
       add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

       server {
           listen 80;
           server_name yourdomain.com;
           return 301 https://$server_name$request_uri;
       }

       server {
           listen 443 ssl http2;
           server_name yourdomain.com;

           # SSL Certificate
           ssl_certificate /opt/ai-tutor/ssl/fullchain.pem;
           ssl_certificate_key /opt/ai-tutor/ssl/privkey.pem;

           # Frontend - Serve static files and handle SPA routing
           location / {
               proxy_pass http://ai-tutor-frontend:80;
               proxy_set_header Host $host;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
           }

           # API requests - Proxy to backend
           location /api/v1 {
               proxy_pass http://ai-tutor-backend:8080;
               proxy_set_header Host $host;
               proxy_set_header X-Real-IP $remote_addr;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header X-Forwarded-Proto $scheme;
               proxy_connect_timeout 60s;
               proxy_send_timeout 60s;
               proxy_read_timeout 60s;
           }

           # Health check endpoint
           location /health {
               access_log off;
               return 200 "healthy\n";
               add_header Content-Type text/plain;
           }

           # Security - Deny access to sensitive files
           location ~ /\. {
               deny all;
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

1. Pull the latest production images:
   ```bash
   # Make sure docker-compose.prod.yml points to the correct GitHub Container Registry image names
   
   docker compose -f docker-compose.prod.yml pull
   ```

2. Start the services:
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

3. Verify all services are running:
   ```bash
   docker compose -f docker-compose.prod.yml ps
   ```

4. Check the application logs:
   ```bash
   docker compose -f docker-compose.prod.yml logs -f
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

### Step 8: Monitoring Setup

1. Set up basic monitoring:
   ```bash
   # Check if services are running
   docker compose -f docker-compose.prod.yml ps
   
   # Monitor logs for issues
   docker compose -f docker-compose.prod.yml logs -f --tail=100
   ```

2. Create a simple monitoring script:
   ```bash
   cat > /opt/ai-tutor/monitor.sh << 'EOF'
   #!/bin/bash
   # Simple monitoring script for AI Tutor
   
   # Check if all containers are running
   RUNNING_CONTAINERS=$(docker compose -f /opt/ai-tutor/docker-compose.prod.yml ps -q | wc -l)
   TOTAL_CONTAINERS=$(docker compose -f /opt/ai-tutor/docker-compose.prod.yml config --services | wc -l)
   
   if [ "$RUNNING_CONTAINERS" -ne "$TOTAL_CONTAINERS" ]; then
       echo "$(date): Warning! Not all containers are running"
       docker compose -f /opt/ai-tutor/docker-compose.prod.yml ps
   fi
   
   # Check if the site is responding
   HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://yourdomain.com)
   if [ "$HTTP_CODE" -ne "200" ]; then
       echo "$(date): Warning! Site returned HTTP $HTTP_CODE"
   fi
   EOF
   
   chmod +x /opt/ai-tutor/monitor.sh
   ```

3. Add monitoring to crontab:
   ```bash
   crontab -e
   ```
   
   Add the following line to check every 5 minutes:
   ```bash
   */5 * * * * /opt/ai-tutor/monitor.sh >> /opt/ai-tutor/logs/monitor.log 2>&1
   ```

## Troubleshooting

### Common Issues

1. **SSL Certificate Issues**
   - Check if domain name is correctly pointing to server
   - Verify certificate files exist in `/opt/ai-tutor/ssl/`
   - Ensure nginx is configured to use correct certificate paths

2. **Database Connection Issues**
   - Check the DB container is running: `docker compose -f docker-compose.prod.yml ps`
   - Verify volume mounts are accessible: `ls -la /opt/ai-tutor/data/`
   - Check the H2 database logs

3. **API Connectivity Issues**
   - Verify CORS settings in the environment file
   - Check nginx proxy configuration
   - Ensure backend is accessible on port 8080

4. **Docker Container Failures**
   - Check container logs: `docker compose -f docker-compose.prod.yml logs <service-name>`
   - Verify environment variables are correctly set
   - Ensure sufficient disk space is available

### Diagnostic Commands

- Check all services: `docker compose -f docker-compose.prod.yml ps`
- View application logs: `docker compose -f docker-compose.prod.yml logs -f`
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

1. **Before Update**
   - Create a backup of data and configurations
   - Test update procedure in a staging environment
   - Schedule update during low-traffic periods

2. **Update Process**
   ```bash
   # Pull new images
   docker compose -f docker-compose.prod.yml pull
   
   # Stop current services
   docker compose -f docker-compose.prod.yml down
   
   # Start services with new images
   docker compose -f docker-compose.prod.yml up -d
   
   # Verify services are running correctly
   docker compose -f docker-compose.prod.yml ps
   ```

3. **Rollback Procedure**
   - If issues arise, identify the previous stable version
   - Update docker-compose.prod.yml to use previous image tags
   - Restart services: `docker compose -f docker-compose.prod.yml up -d`

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

## Conclusion

This deployment process creates a production-ready AI Tutor application with proper security, monitoring, and maintenance procedures. The setup includes SSL termination, a reverse proxy, persistent data storage, and automated certificate renewal.

Remember to regularly maintain the system, monitor its performance, and follow security best practices to ensure the application continues to run securely and efficiently.