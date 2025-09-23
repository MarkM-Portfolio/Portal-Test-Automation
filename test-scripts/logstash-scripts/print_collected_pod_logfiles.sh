# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
#!/bin/bash

# Accept parameters
ENABLE_LOGSTASH_SETUP="$1"

# Define the directory where logs are stored
LOG_DIRECTORY="/home/centos/logs_output"

# Define the directory where you want to copy the log files
PODLOGS_DIRECTORY="/home/centos/pod_logs"

# Check if ENABLE_LOGSTASH_SETUP is true
if [[ "$ENABLE_LOGSTASH_SETUP" == "true" ]]; then
    # Function to display log directory size
    display_log_directory_size() {
        echo "=== Log Directory Size ==="
        du -sh "$LOG_DIRECTORY"
    }

    # Function to display all stored .log files
    display_stored_log_files() {
        echo "=== Stored .log Files ==="
        for log_file in "$LOG_DIRECTORY"/*.log; do
            echo "$(basename "$log_file")"
        done
    }

    # Function to copy all displayed log files to a different folder
    copy_log_files() {
        echo "=== Copying Log Files ==="
        mkdir -p "$PODLOGS_DIRECTORY"
        cp "$LOG_DIRECTORY"/*.log "$PODLOGS_DIRECTORY"
        echo "All log files copied to $PODLOGS_DIRECTORY"
    }

    # Example usage:
    display_log_directory_size
    display_stored_log_files
    copy_log_files
else
    echo "Logstash setup is not enabled. Skipping display log directory size and stored .log files."
fi