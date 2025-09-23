#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Create new content entry in the search management index.
# The following parameter are needed in the correct order to create the new entry.

# Bearer token for search authorization
BEARER_TOKEN=$1

# Search host URL
SEARCH_HOST=$2

# Name (alias) of the new entry
CONTENT_NAME=$3
# Content type
CONTENT_TYPE=$4
# Optional ACL lookup host
ACL_HOST=$5

# Add crawler
INDEX_RESULT=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer ${BEARER_TOKEN}" -X POST "https://${SEARCH_HOST}/dx/api/search/v2/contentsources" -d "{ \"name\": \"${CONTENT_NAME}\", \"type\": \"${CONTENT_TYPE}\", \"aclLookupHost\": \"${ACL_HOST}\" }")
echo $INDEX_RESULT
