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

# Check if the log collection PID file exists
if [ -f "/home/centos/log_collection.pid" ]; then
    # Read the log collection PID from the file
    log_collection_pid=$(cat /home/centos/log_collection.pid)
    
    # Check if the log collection process is running
    if ps -p $log_collection_pid > /dev/null; then
        echo "Stopping log collection script with PID: $log_collection_pid"
        # Send SIGTERM signal to stop the log collection process
        kill $log_collection_pid
        echo "Log collection script stopped successfully."
        # Remove the PID file
        rm /home/centos/log_collection.pid
    else
        echo "Log collection script with PID $log_collection_pid is not running."
        # Remove the PID file if the log collection process is not running
        rm /home/centos/log_collection.pid
    fi
else
    echo "Log collection PID file not found. Log collection script may not be running."
fi