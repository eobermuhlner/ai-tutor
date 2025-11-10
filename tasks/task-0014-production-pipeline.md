# Task 0014: Production Deployment Pipeline

## Overview
This task focuses on creating a comprehensive production deployment pipeline for the AI Tutor application to enable deployment to Linux servers. Based on the existing CI/CD pipeline using GitHub Actions and Docker, we need to establish a complete production deployment process that includes configuration management, reverse proxy setup, SSL termination, and maintenance procedures.

## Current State Analysis
The application currently supports:
- GitHub Actions CI/CD with Docker image building
- Multi-stage Docker builds for both backend (Spring Boot) and frontend (React)
- Local docker-compose development environment
- Multi-provider AI integration (OpenAI, Azure OpenAI, Ollama, Anthropic)
- Bring Your Own Key (BYOK) feature for API keys

## Requirements

### 1. Production-Ready Docker Compose Configuration
- Create an optimized docker-compose.prod.yml for production deployment
- Include proper service dependencies and health checks
- Configure persistent storage for the H2 database
- Set up environment variable management for sensitive configuration
- Include resource limits and restart policies

### 2. Environment Configuration Management
- Document all required environment variables for production
- Create a template .env file for production deployment
- Define security best practices for sensitive configuration (API keys, JWT secrets)
- Document how to properly configure AI provider credentials in production

### 3. Reverse Proxy and SSL Configuration
- Set up nginx as a reverse proxy for both backend and frontend services
- Configure SSL certificate management with Let's Encrypt support
- Implement proper security headers and HTTP-to-HTTPS redirects
- Document how to set up custom domain names

### 4. Production Security Measures
- Implement non-root container execution
- Configure appropriate firewall rules
- Set up rate limiting to prevent abuse
- Ensure proper CORS configuration for production domain

### 5. Backup and Maintenance Procedures
- Document automated backup procedures for the H2 database
- Create maintenance scripts for routine tasks
- Set up monitoring and alerting capabilities
- Document update and rollback procedures

### 6. Deployment Process Documentation
- Step-by-step deployment guide for Linux servers
- Prerequisites and system requirements
- Configuration validation procedures
- Troubleshooting guide

## Implementation Plan

### Phase 1: Production Docker Configuration
#### 1.1 Create Production Docker Compose File
Create `docker-compose.prod.yml` with the following services:
- **Backend Service**: Spring Boot application configured for production profile
  - Use production images from Docker Hub
  - Configure environment variables for production
  - Set up health checks and restart policies
  - Mount persistent volumes for database and course content
  - Configure resource limits and logging

- **Frontend Service**: React application served by nginx
  - Use production images from Docker Hub
  - Configure API URL for production backend
  - Set up health checks and restart policies

- **Database Service**: H2 database with persistent storage
  - Mount volume for persistent data storage
  - Configure appropriate memory settings
  - Set up proper backup procedures

- **Nginx Service**: Reverse proxy with SSL termination
  - Configure SSL certificate support
  - Set up routing to backend and frontend
  - Implement security headers and performance optimizations

#### 1.2 Environment Variable Configuration
Define the following environment variables in a production `.env` file:

```bash
# GitHub Container Registry configuration
GITHUB_USERNAME=your-github-username

# AI Provider Configuration (Choose one or more)
OPENAI_API_KEY=your-openai-api-key
# AZURE_OPENAI_API_KEY=your-azure-openai-key
# AZURE_OPENAI_ENDPOINT=your-azure-openai-endpoint
# ANTHROPIC_API_KEY=your-anthropic-key

# Security Configuration
JWT_SECRET=your-32-character-jwt-secret-key # Generate with: openssl rand -base64 32
ENCRYPTION_KEY=your-32-character-encryption-key # For BYOK feature

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

### Phase 2: Reverse Proxy and SSL Setup
#### 2.1 Nginx Configuration
Create `nginx.conf` for production with:
- SSL certificate handling
- Static asset serving for frontend
- API request proxying to backend
- Security headers (HSTS, X-Frame-Options, etc.)
- Gzip compression
- Rate limiting
- Logging configuration

#### 2.2 SSL Certificate Management
- Document Let's Encrypt certificate acquisition using Certbot
- Set up automated certificate renewal
- Configure nginx for SSL termination
- Document custom certificate installation process

### Phase 3: Deployment Process Documentation
#### 3.1 Server Preparation
- Install Docker and Docker Compose on the Linux server
- Configure firewall (ufw/iptables) to allow necessary ports
- Set up domain name to point to server IP
- Verify system requirements and resources

#### 3.2 Deployment Steps
1. Clone or copy the production configuration files to the server
2. Create the required directory structure: `/opt/ai-tutor/{data,course-content,ssl,logs}`
3. Configure the `.env` file with production values
4. Pull the latest production Docker images
5. Start the services using `docker-compose -f docker-compose.prod.yml up -d`
6. Verify all services are running correctly
7. Test the application functionality
8. Set up monitoring and logging

#### 3.3 Post-Deployment Tasks
- Configure SSL certificate with Let's Encrypt if using
- Set up monitoring for service health
- Configure backup procedures
- Set up alerting for critical issues
- Document any post-deployment validation steps

### Phase 4: Backup and Maintenance
#### 4.1 Backup Procedures
- Create automated database backup scripts
- Schedule regular backup execution (e.g., via cron)
- Implement off-site backup storage
- Document backup verification procedures

#### 4.2 Maintenance Procedures
- Create scripts for routine maintenance tasks
- Document update and rollback procedures
- Set up monitoring and alerting
- Plan for regular security updates

## Configuration Files to Create

### docker-compose.prod.yml
```yaml
version: '3.8'

services:
  backend:
    image: ghcr.io/${GITHUB_USERNAME:-your-username}/ai-tutor-backend:production
    container_name: ai-tutor-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      # Spring Profile - set to production for production deployment
      - SPRING_PROFILES_ACTIVE=prod
      
      # Required: OpenAI Configuration (or other AI provider configuration)
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      # Alternative: Azure OpenAI (uncomment to use)
      # - SPRING_PROFILES_ACTIVE=prod-azure-openai
      # - AZURE_OPENAI_API_KEY=${AZURE_OPENAI_API_KEY}
      # - AZURE_OPENAI_ENDPOINT=${AZURE_OPENAI_ENDPOINT}
      
      # JWT Configuration - CRITICAL: Change this in production!
      - JWT_SECRET=${JWT_SECRET}
      
      # Admin User Configuration
      - ADMIN_USERNAME=${ADMIN_USERNAME:-admin}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
      
      # Demo User Configuration
      - DEMO_USERNAME=${DEMO_USERNAME:-demo}
      - DEMO_PASSWORD=${DEMO_PASSWORD}
      
      # Database Configuration (H2 file-based for production)
      - SPRING.DATASOURCE.URL=jdbc:h2:file:/data/db/aitutor;DB_CLOSE_ON_EXIT=FALSE
      
      # CORS Configuration for production
      - CORS_ALLOWED-ORIGINS[0]=${FRONTEND_URL:-https://yourdomain.com}
      
      # CEFR Level Configuration
      - CEFR_LEVEL_MIN_TURNS=${CEFR_LEVEL_MIN_TURNS:-3}
      
      # Progressive Summarization Configuration
      - AI-TUTOR_CONTEXT_SUMMARIZATION_ENABLED=${PROGRESSIVE_SUMMARIZATION_ENABLED:-true}
      - AI-TUTOR_CONTEXT_SUMMARIZATION_PROGRESSIVE_ENABLED=${PROGRESSIVE_SUMMARIZATION_ENABLED:-true}
      - AI-TUTOR_CONTEXT_SUMMARIZATION_CHUNK_SIZE=${SUMMARIZATION_CHUNK_SIZE:-10}
    volumes:
      # Persistent data storage
      - ./data:/data
      # Course content for updates without rebuild
      - ./course-content:/app/course-content
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 3s
      start_period: 60s
      retries: 3
    networks:
      - ai-tutor-network
    depends_on:
      - db

  frontend:
    image: ghcr.io/${GITHUB_USERNAME:-your-username}/ai-tutor-frontend:production
    container_name: ai-tutor-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    environment:
      - VITE_API_URL=${BACKEND_URL:-http://localhost:8080/api/v1}
    depends_on:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost/ >/dev/null"]
      interval: 30s
      timeout: 3s
      start_period: 5s
      retries: 3
    networks:
      - ai-tutor-network

  db:
    image: h2database/h2:2.2.224
    container_name: ai-tutor-db
    restart: unless-stopped
    environment:
      - H2_OPTS=-Xmx1g
    volumes:
      - ./data:/data
    command: ["-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-baseDir", "/data"]
    networks:
      - ai-tutor-network

networks:
  ai-tutor-network:
    driver: bridge
```

### nginx.conf (production template)
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
        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

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

## Security Considerations

### 1. Data Protection
- Never commit secrets to version control
- Use environment variables for sensitive configuration
- Implement proper encryption for stored API keys using the BYOK feature
- Ensure all traffic is encrypted with SSL/TLS

### 2. Container Security
- Run containers as non-root users where possible
- Implement resource limits to prevent denial of service
- Regularly update base images and dependencies
- Implement proper network segmentation

### 3. Network Security
- Configure firewall to restrict access to necessary ports
- Implement rate limiting to prevent abuse
- Use secure SSL protocols and ciphers
- Implement proper CORS policies for the frontend

## Monitoring and Maintenance

### 1. System Monitoring
- Monitor container health and resource usage
- Set up alerts for service failures
- Monitor application logs for errors
- Track performance metrics

### 2. Backup Strategy
- Daily automated database backups
- Off-site backup storage
- Regular backup verification
- Documented restore procedures

### 3. Update Process
- Test updates in staging environment
- Implement blue-green deployment for minimal downtime
- Document rollback procedures
- Schedule updates during low-usage periods

## Deployment Validation

### 1. Pre-Deployment Checks
- Verify server meets system requirements
- Check available disk space and memory
- Validate domain name configuration
- Ensure SSL certificate availability

### 2. Post-Deployment Validation
- Verify all services are running
- Test frontend and backend functionality
- Confirm SSL certificate is working
- Validate API endpoints
- Check user registration and login

## Troubleshooting Guide

### 1. Common Issues
- SSL certificate errors: Check certificate files and paths
- Database connection issues: Verify volume mounts and permissions
- API connectivity: Check CORS configuration and proxy settings
- Container startup failures: Review logs and resource limits

### 2. Diagnostic Commands
- Check container status: `docker-compose -f docker-compose.prod.yml ps`
- View application logs: `docker-compose -f docker-compose.prod.yml logs -f`
- Check health endpoints: `curl https://yourdomain.com/health`
- Verify database connectivity: Check H2 logs

## Conclusion
This production deployment pipeline enables the AI Tutor application to be deployed to any Linux server with Docker support. The configuration uses the existing Docker images built by the GitHub Actions pipeline and provides a secure, scalable, and maintainable production environment.