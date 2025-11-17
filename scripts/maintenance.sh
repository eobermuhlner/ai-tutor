#!/bin/bash
# AI Tutor Maintenance Script
# This script performs routine maintenance tasks for the AI Tutor application

set -e  # Exit on any error

# Configuration
APP_DIR="/opt/ai-tutor"
LOG_FILE="/opt/ai-tutor/logs/maintenance.log"

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a $LOG_FILE
}

# Function to check service health
check_service_health() {
    log_message "Checking service health..."
    
    # Change to the application directory
    cd $APP_DIR
    
    # Check if all services are running
    RUNNING_SERVICES=$(docker compose -f docker-compose.prod.yml ps --services --filter "status=running" | wc -l)
    TOTAL_SERVICES=$(docker compose -f docker-compose.prod.yml config --services | wc -l)
    
    log_message "Running services: $RUNNING_SERVICES / Total services: $TOTAL_SERVICES"
    
    if [ "$RUNNING_SERVICES" -ne "$TOTAL_SERVICES" ]; then
        log_message "WARNING: Not all services are running"
        docker compose -f docker-compose.prod.yml ps
    else
        log_message "All services are running"
    fi
    
    # Check container logs for errors
    SERVICES=$(docker compose -f docker-compose.prod.yml config --services)
    for service in $SERVICES; do
        # Check for error patterns in logs
        ERROR_COUNT=$(docker compose -f docker-compose.prod.yml logs --tail=50 "$service" 2>&1 | grep -i -E "(error|exception|fatal|segfault)" | wc -l)
        if [ "$ERROR_COUNT" -gt 0 ]; then
            log_message "WARNING: Found $ERROR_COUNT error patterns in $service logs"
        fi
    done
}

# Function to clean Docker system
clean_docker_system() {
    log_message "Cleaning Docker system..."
    
    # Remove unused containers, networks, images, and build cache
    docker system prune -f
    
    # Remove unused volumes (be careful with this)
    # Uncomment the next line if you want to remove unused volumes
    # docker volume prune -f
    
    log_message "Docker system cleaned"
}

# Function to rotate logs
rotate_logs() {
    log_message "Rotating logs..."
    
    # Find log files and compress older ones
    find $APP_DIR/logs -name "*.log" -type f -mtime +7 -exec gzip {} \; 2>/dev/null || true
    
    # Remove logs older than 30 days
    find $APP_DIR/logs -name "*.log.gz" -type f -mtime +30 -delete 2>/dev/null || true
    
    log_message "Log rotation completed"
}

# Function to restart unhealthy containers
restart_unhealthy_containers() {
    log_message "Checking for unhealthy containers..."
    
    cd $APP_DIR
    
    # Get list of containers with their health status
    UNHEALTHY=$(docker compose -f docker-compose.prod.yml ps --format "table {{.Name}}\t{{.Status}}" | grep -i "unhealthy\|exited\|dead" | wc -l)
    
    if [ "$UNHEALTHY" -gt 0 ]; then
        log_message "Found $UNHEALTHY unhealthy containers, restarting them..."
        docker compose -f docker-compose.prod.yml restart
        log_message "Unhealthy containers restarted"
    else
        log_message "All containers are healthy"
    fi
}

# Function to check disk usage
check_disk_usage() {
    log_message "Checking disk usage..."
    
    # Check disk usage for important directories
    for dir in "/" "/opt" "/opt/ai-tutor" "/var/lib/docker"; do
        if [ -d "$dir" ]; then
            USAGE=$(df "$dir" | awk 'NR==2 {print $5}' | sed 's/%//')
            if [ "$USAGE" -gt 80 ]; then
                log_message "WARNING: $dir disk usage is ${USAGE}%"
            else
                log_message "$dir disk usage is ${USAGE}%"
            fi
        fi
    done
}

# Function to check memory usage
check_memory_usage() {
    log_message "Checking memory usage..."
    
    # Get memory usage percentage
    MEM_USAGE=$(free | awk 'NR==2{printf "%.1f", $3*100/$2}')
    
    if (( $(echo "$MEM_USAGE > 80" | bc -l) )); then
        log_message "WARNING: Memory usage is ${MEM_USAGE}%"
    else
        log_message "Memory usage is ${MEM_USAGE}%"
    fi
}

# Function to update SSL certificates if needed
update_ssl_certificates() {
    log_message "Checking SSL certificate expiration..."
    
    # Check if certbot is available
    if command -v certbot &> /dev/null; then
        # Renew certificates if they are close to expiration
        sudo certbot renew --quiet
        
        # Check if renewal was successful by comparing file modification times
        if [ -f "/etc/letsencrypt/live/$(hostname)/fullchain.pem" ]; then
            CERT_AGE=$(find /etc/letsencrypt/live/$(hostname)/fullchain.pem -mtime -30 | wc -l)
            if [ "$CERT_AGE" -eq 0 ]; then
                log_message "WARNING: SSL certificate might be expiring soon"
            else
                log_message "SSL certificate is up to date"
            fi
        fi
    else
        log_message "Certbot not found, skipping SSL certificate check"
    fi
}

# Main execution
main() {
    log_message "=== Maintenance process started ==="
    
    check_service_health
    clean_docker_system
    rotate_logs
    restart_unhealthy_containers
    check_disk_usage
    check_memory_usage
    update_ssl_certificates
    
    log_message "=== Maintenance process completed ==="
}

# Run main function
main