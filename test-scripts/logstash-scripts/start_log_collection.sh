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

# Namespace and log directory configuration
NAMESPACE="dxns"
LOG_DIRECTORY="/home/centos/logs_output"

# Accept parameters
ENABLE_LOGSTASH_SETUP="$1"

# Check if ENABLE_LOGSTASH_SETUP is true
if [[ "$ENABLE_LOGSTASH_SETUP" == "true" ]]; then
    # Create the directory if it doesn't exist
    mkdir -p "$LOG_DIRECTORY"
    chmod 755 "$LOG_DIRECTORY"

    # Function to fetch logs for a given pod name
    fetch_logs() {
        local pod_name="$1"
        local current_time=$(date +"%Y%m%d%H%M%S")  # Generate timestamp in the desired format
        local log_file="${LOG_DIRECTORY}/${pod_name}_${current_time}.log"
        local logs=$(kubectl -n "$NAMESPACE" logs "$pod_name" 2>&1)  # Redirect stderr to stdout to capture error messages
        if [[ $logs =~ "Error from server (NotFound): pods \"$pod_name\" not found" ]]; then
            echo "Pod '$pod_name' not found in namespace '$NAMESPACE'."
        else
            echo "$logs" >> "$log_file"  # Append logs to the log file
            # Calculate and display the size of the log file
            local log_size=$(du -sh "$log_file" | awk '{print $1}')
            echo "Size of $pod_name log file: $log_size"
        fi
    }

    # Continuous loop to fetch logs
    while true; do
        PODS=$(kubectl -n "$NAMESPACE" get pods -o jsonpath='{.items[*].metadata.name}')  # Get all pod names in the namespace
        for pod_name in $PODS; do
            fetch_logs "$pod_name"
        done
        echo "Logs retrieved at $(date +"%Y-%m-%d %H:%M:%S")..."
        sleep 30  # Wait for 30 seconds before fetching logs again
    done >> log_collection.log 2>&1 &

    # Store the PID of the log collection process
    log_collection_pid=$!

    # Save PID to a file for future reference
    echo "$log_collection_pid" > /home/centos/log_collection.pid

    echo "Log collection started with PID: $log_collection_pid"
else
    echo "Logstash setup is not enabled. Skipping log collection."
fi