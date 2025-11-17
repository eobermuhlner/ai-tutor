#!/bin/bash
# AI Tutor SSL Certificate Check Script
# This script checks SSL certificate expiration and logs the status

set -e

LOG_FILE="/opt/ai-tutor/logs/ssl-check.log"

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a $LOG_FILE
}

# Function to check SSL certificate expiration
check_ssl_cert() {
    log_message "Checking SSL certificate expiration..."
    
    if command -v certbot &> /dev/null; then
        # Check if certificate files exist
        if [ -f "/opt/ai-tutor/ssl/fullchain.pem" ]; then
            # Get expiration date from certificate
            EXPIRATION_DATE=$(openssl x509 -in /opt/ai-tutor/ssl/fullchain.pem -noout -enddate | cut -d= -f2)
            EXPIRATION_TIMESTAMP=$(date -d "$EXPIRATION_DATE" +%s)
            CURRENT_TIMESTAMP=$(date +%s)
            
            # Calculate days until expiration
            SECONDS_PER_DAY=86400
            DAYS_UNTIL_EXPIRATION=$(( (EXPIRATION_TIMESTAMP - CURRENT_TIMESTAMP) / SECONDS_PER_DAY ))
            
            log_message "SSL Certificate expires on: $EXPIRATION_DATE"
            log_message "Days until expiration: $DAYS_UNTIL_EXPIRATION"
            
            if [ $DAYS_UNTIL_EXPIRATION -lt 10 ]; then
                log_message "CRITICAL: SSL Certificate expires in less than 10 days!"
            elif [ $DAYS_UNTIL_EXPIRATION -lt 30 ]; then
                log_message "WARNING: SSL Certificate expires in $DAYS_UNTIL_EXPIRATION days"
            else
                log_message "SSL Certificate is valid for $DAYS_UNTIL_EXPIRATION more days"
            fi
        else
            log_message "WARNING: SSL certificate file not found at /opt/ai-tutor/ssl/fullchain.pem"
        fi
    else
        log_message "ERROR: Certbot not found, cannot check SSL certificates"
    fi
}

# Execute the check
log_message "=== SSL certificate check started ==="
check_ssl_cert
log_message "=== SSL certificate check completed ==="