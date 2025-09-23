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

# Check if ENABLE_LOGSTASH_SETUP is true
if [[ "$ENABLE_LOGSTASH_SETUP" == "true" ]]; then
    # Start Logstash with the created pipeline.conf in the background
    cd /home/centos/logstash-8.9.0
    echo "Start Logstash with the created pipeline.conf"
    ./bin/logstash -f config/pipeline.conf > /home/centos/logstash.log 2>&1 &
    # Store the PID of the Logstash process
    logstash_pid=$!
    # Save PID to a file for future reference
    echo "$logstash_pid" > /home/centos/logstash.pid

    echo "Logstash started with PID: $logstash_pid"
else
    echo "Logstash setup is not enabled. Skipping Logstash startup"
fi