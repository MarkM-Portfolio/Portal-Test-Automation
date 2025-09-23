#!/bin/bash
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Function to check the readiness of nodes
check_nodes_readiness() {
    nodes=$(kubectl get nodes -o=name)

    for node in $nodes; do
        readiness=$(kubectl get $node --template="{{range .status.conditions}}{{if eq .type \"Ready\"}}{{.status}}{{end}}{{end}}")

        # If any node is not ready, return false
        [[ "$readiness" != "True" ]] && return 1
    done

    # All nodes are ready
    return 0
}

# Set the duration for checking (in seconds)
duration=300  # 5 minutes

# Get the start time
start_time=$(date +%s)

# Loop until either all nodes are up or the duration is reached
while [ $(( $(date +%s) - start_time )) -lt $duration ]; do
    if check_nodes_readiness; then
        echo "All nodes are up and ready!"
        exit 0
    fi

    echo "Not all nodes are up. Waiting..."
    sleep 15
done

# Display the current status of each node
echo "Timeout reached. Current status of nodes:"
kubectl get nodes
exit 1
