#!/bin/bash
# AI Tutor Disk Space Check Script
# This script checks disk space and sends warnings if usage is too high

set -e

LOG_FILE="/opt/ai-tutor/logs/disk-check.log"

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a $LOG_FILE
}

# Function to check disk space and send alerts
check_disk_space() {
    log_message "Checking disk space..."
    
    # Check main important directories
    for dir in "/" "/opt" "/opt/ai-tutor" "/var/lib/docker"; do
        if [ -d "$dir" ]; then
            USAGE=$(df "$dir" | awk 'NR==2 {print $5}' | sed 's/%//')
            if [ "$USAGE" -gt 85 ]; then
                log_message "CRITICAL: $dir disk usage is ${USAGE}% - Please take immediate action"
            elif [ "$USAGE" -gt 80 ]; then
                log_message "WARNING: $dir disk usage is ${USAGE}%"
            else
                log_message "$dir disk usage is ${USAGE}% - OK"
            fi
        fi
    done
}

# Execute the check
log_message "=== Disk space check started ==="
check_disk_space
log_message "=== Disk space check completed ==="