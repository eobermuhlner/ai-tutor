#!/bin/bash
# AI Tutor Backup and Maintenance Scripts

# backup.sh - Daily backup script for AI Tutor application
# This script creates backups of the database and other important data

set -e  # Exit on any error

# Configuration
BACKUP_DIR="/opt/ai-tutor/backups"
DATA_DIR="/opt/ai-tutor/data"
LOG_FILE="/opt/ai-tutor/logs/backup.log"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="ai-tutor-backup-$DATE"
RETENTION_DAYS=30  # Keep backups for 30 days

# Function to log messages
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a $LOG_FILE
}

# Function to perform backup
perform_backup() {
    log_message "Starting backup process for $BACKUP_NAME"
    
    # Create backup directory if it doesn't exist
    mkdir -p $BACKUP_DIR
    
    # Create temporary directory for this backup
    TEMP_DIR=$(mktemp -d)
    
    # Copy database files to temporary directory
    log_message "Copying database files..."
    if [ -d "$DATA_DIR" ]; then
        cp -r $DATA_DIR $TEMP_DIR/db_backup
        log_message "Database files copied successfully"
    else
        log_message "WARNING: Database directory does not exist: $DATA_DIR"
    fi
    
    # Copy course content
    log_message "Copying course content..."
    if [ -d "/opt/ai-tutor/course-content" ]; then
        cp -r /opt/ai-tutor/course-content $TEMP_DIR/course-content
        log_message "Course content copied successfully"
    else
        log_message "WARNING: Course content directory does not exist"
    fi
    
    # Create backup archive
    log_message "Creating backup archive..."
    tar -czf "$BACKUP_DIR/$BACKUP_NAME.tar.gz" -C $TEMP_DIR .
    
    # Clean up temporary directory
    rm -rf $TEMP_DIR
    
    log_message "Backup created successfully: $BACKUP_DIR/$BACKUP_NAME.tar.gz"
}

# Function to clean old backups
clean_old_backups() {
    log_message "Cleaning backups older than $RETENTION_DAYS days..."
    
    find $BACKUP_DIR -name "ai-tutor-backup-*.tar.gz" -type f -mtime +$RETENTION_DAYS -delete
    
    log_message "Old backups cleaned up"
}

# Function to check disk space
check_disk_space() {
    log_message "Checking disk space..."
    
    USAGE=$(df $BACKUP_DIR | awk 'NR==2 {print $5}' | sed 's/%//')
    
    if [ $USAGE -gt 80 ]; then
        log_message "WARNING: Disk usage is ${USAGE}%, which is above 80% threshold"
    else
        log_message "Disk usage is at ${USAGE}%, which is acceptable"
    fi
}

# Main execution
main() {
    log_message "=== Backup process started ==="
    
    perform_backup
    clean_old_backups
    check_disk_space
    
    log_message "=== Backup process completed ==="
}

# Run main function
main