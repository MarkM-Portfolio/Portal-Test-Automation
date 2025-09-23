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

host=$1
protocol=$2
prometheus_port=$3
# Set the time range (in minutes)
time_range=$4  # Change this to your desired time range
logLocation_cpu_results="/home/centos/prometheus_cpu_results.json"
logLocation_memory_results="/home/centos/prometheus_memory_results.json"

# Set execution permission for the script
chmod +x "$0"

url="${protocol}://${host}:${prometheus_port}/api/v1/query"

# Set the time range (in seconds)
time_range=60  # Change this to your desired time range


cpu_query="sum by (pod) (rate(container_cpu_usage_seconds_total{job=\"kubernetes-nodes-cadvisor\", container!=\"\", namespace=\"dxns\"}[${time_range}m]))"
memory_query="avg_over_time(container_memory_usage_bytes{namespace=\"dxns\"}[${time_range}m]) / 1024 / 1024"


# Read the allowed containers from the configuration file
allowed_containers=($(< ./container_config.txt))


execute_query() {
    local query="$1"
    curl -s -G "${url}" --data-urlencode "query=${query}" | jq -c '.data.result[] | {pod: .metric.pod, container: .metric.container, value: .value[1]}'
}


cpu_result=$(execute_query "${cpu_query}")
memory_result=$(execute_query "${memory_query}")

# Function to check if a string contains both numbers and letters
contains_numbers_and_letters() {
    [[ $1 =~ [[:digit:]] && $1 =~ [[:alpha:]] ]]
}

transform_pod_name() {
local your_string="$1"
# Check if the string contains alphanumeric characters
if [[ $your_string =~ [[:alnum:]] ]]; then
    # Split the string based on '-'
    IFS='-' read -ra string_array <<< "$your_string"
    
    # Initialize a new array
    new_array=()
    
    # Loop through each index in the array
    for ((i=0; i<${#string_array[@]}; i++)); do
        value="${string_array[$i]}"

        # If the value is "db2", add it to the new array
        if [[ $value == "db2" ]]; then
            new_array+=("$value")
            continue
        fi
        
        # Check if the value contains both numbers and letters (more than one character)
        if contains_numbers_and_letters "$value"; then
            # Skip the current value and the next one
            ((i++))
            continue
        fi
        
        # Add the value to the new array
        new_array+=("$value")
    done
    
    # Join the array elements with '-'
    new_string=$(IFS='-' ; echo "${new_array[*]}")
    
    echo "$new_string"
else
    echo "String does not contain alphanumeric characters."
fi
return 
}

# Format CPU result into the desired structure
cpu_json="{"
while IFS= read -r line; do
    pod=$(echo "$line" | jq -r '.pod')
    value=$(echo "$line" | jq -r '.value')
    pod=$(transform_pod_name "${pod}")
    cpu_json+="\"${pod}\": \"${value}\", "
done <<< "$cpu_result"
cpu_json="${cpu_json%, *} }"

# Format memory result into the desired structure
memory_json="{"
while IFS= read -r line; do
    pod=$(echo "$line" | jq -r '.pod')
    pod=$(transform_pod_name "${pod}")
    container=$(echo "$line" | jq -r '.container')
    value=$(echo "$line" | jq -r '.value')

    # Check if the container is allowed
    if [[ " ${allowed_containers[@]} " =~ " ${container} " ]]; then
        memory_json+="\"${pod}\": {\"${container}\": \"${value}\"}, "
    fi
done <<< "$memory_result"
memory_json="${memory_json%, *} }"

# Remove double quotes only from values in the entire CPU JSON string
cpu_json=$(echo "${cpu_json}" | sed -E 's/"([0-9]+\.[0-9]+)"/\1/g')

# Remove double quotes only from values in the entire Memory JSON string
memory_json=$(echo "${memory_json}" | sed -E 's/"([0-9]+\.[0-9]+)"/\1/g')

# Print the final JSON structure for CPU
echo "${cpu_json}" >> $logLocation_cpu_results

# Print the final JSON structure for Memory
echo "${memory_json}" >> $logLocation_memory_results