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

# Check if arguments are provided
if [ $# -ne 5 ]; then
    echo "Usage: $0 <protocol> <host> <index> <username> <password>"
fi

protocol="$1"
host="$2"
index="$3"
username="$4"

# Construct the delete logs OpenSearch URL
deleteIndexUrl="${protocol}://${host}/${index}/_delete_by_query"
echo "deleteIndexUrl:- ${deleteIndexUrl}"

# Construct the URL for index refresh
refreshIndexUrl="${protocol}://${host}/${index}/_refresh"
echo "refreshIndexUrl:- ${refreshIndexUrl}"

# Construct the OpenSearch URLs
opensearchIndexUrl="${protocol}://${host}/${index}"
echo "opensearchIndexUrl:- ${opensearchIndexUrl}"

# Construct the OpenSearch URLs for check index data 
checkIndexUrl="${protocol}://${host}/${index}/_search"
echo "checkIndexUrl:- ${checkIndexUrl}"

# Construct the request body
request_body='{
  "query": {
    "range": {
      "@timestamp": {
        "lt": "now-'$TIME_STAMP'"
      }
    }
  }
}'

# opensearchrequest_body json
echo "${request_body}"


# # Send the request
curl -X POST -u "${username}:${OPENSEARCH_PASSWORD}" -H "Content-Type: application/json" -d "${request_body}" "${deleteIndexUrl}" -k

# # output request
echo "logs deleted successfully !!"

# Send the request to refresh the index
curl -X POST -u "${username}:${OPENSEARCH_PASSWORD}" "${refreshIndexUrl}" -k

# Output message after refreshing index
echo "Index refreshed successfully !!"

# Function to check if index exists
check_index_exists() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" -X GET -u "${username}:${OPENSEARCH_PASSWORD}" "${checkIndexUrl}" -k)
    if [ $response -eq 200 ]; then
        return 0
    else
        return 1
    fi
}

# Function to check if index is empty
check_index_empty() {
    local response=$(curl -s -X POST -u "${username}:${OPENSEARCH_PASSWORD}" "${checkIndexUrl}" -k)
    local total_hits=$(echo "$response" | grep -o '"total":{"value":[0-9]*' | grep -o '[0-9]*')
    if [ $total_hits -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

# Function to delete index
delete_index() {
    local delete_response=$(curl -s -X DELETE -u "${username}:${OPENSEARCH_PASSWORD}" "${opensearchIndexUrl}" -k)
    local acknowledged=$(echo "$delete_response" | grep -o '"acknowledged":true')
    if [ "$acknowledged" ]; then
        return 0
    else
        return 1
    fi
}

# Check if the index exists
if check_index_exists; then
    echo "Index exists."

    # Check if the index is empty
    if check_index_empty; then
        echo "Index is empty. Deleting index..."

        # Check if any snapshots are in progress
        snapshot_in_progress=$(curl -s -u "${username}:${OPENSEARCH_PASSWORD}" "${opensearchIndexUrl}/_stats/snapshot" -k)
        if ! echo "$snapshot_in_progress" | grep -q 'snapshots'; then
            # No snapshot in progress, proceed with index deletion
            if delete_index; then
                echo "Index deleted successfully."
            else
                echo "Failed to delete index."
            fi
        else
            echo "Snapshot in progress. Waiting for snapshot to finish..."
            # Wait for a while before rechecking
            sleep 30
            # Recheck if there are any snapshots in progress
            snapshot_in_progress=$(curl -s -u "${username}:${OPENSEARCH_PASSWORD}" "${opensearchIndexUrl}/_stats/snapshot" -k)
            if ! echo "$snapshot_in_progress" | grep -q 'snapshots'; then
                # No snapshot in progress, proceed with index deletion
                if delete_index; then
                    echo "Index deleted successfully."
                else
                    echo "Failed to delete index."
                fi
            else
                echo "Snapshot is still in progress. Cannot delete index."
            fi
        fi
    else
        echo "Index is not empty. Exiting."
    fi
else
    echo "Index does not exist. Exiting."
fi