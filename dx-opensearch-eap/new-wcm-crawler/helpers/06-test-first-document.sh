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

# Get the first document from a content source index.
# The following parameter are needed in the correct order to create the new entry.

# Bearer token for search authorization
BEARER_TOKEN=$1

# Search host URL
SEARCH_HOST=$2

# Crawler ID created in management index
CONTENTSOURCE_ID=$3

# Request first document
CONTENT_RESULT=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer ${BEARER_TOKEN}" -X POST "https://${SEARCH_HOST}/dx/api/search/v2/search" -d "{ \"query\": {}, \"page\": 0, \"pageSize\": 1, \"scope\": [ \"${CONTENTSOURCE_ID}\" ] }")
echo $CONTENT_RESULT
