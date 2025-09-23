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

# Check if Logstash PID file exists
if [ -f "/home/centos/logstash.pid" ]; then
    # Read the Logstash PID from the file
    logstash_pid=$(cat /home/centos/logstash.pid)
    
    # Check if the Logstash process is running
    if ps -p $logstash_pid > /dev/null; then
        echo "Stopping Logstash with PID: $logstash_pid"
        # Send SIGTERM signal to stop Logstash
        kill $logstash_pid
        echo "Logstash stop signal sent."
        # Remove PID file
        rm /home/centos/logstash.pid
        echo "Logstash PID file removed."
    else
        echo "Logstash with PID $logstash_pid is not running."
        # Remove PID file if Logstash is not running
        rm /home/centos/logstash.pid
        echo "Logstash PID file removed."
    fi
else
    echo "Logstash PID file not found. Logstash may not be running."
fi
