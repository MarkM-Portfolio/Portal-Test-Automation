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
 
# Deletes an element in the search content or crawler management index.
# Deleting a content element include the contentsource index as well.
# The following parameter are needed in the correct order to create the new entry.

# Bearer token for search authorization
BEARER_TOKEN=$1

# Search host URL
SEARCH_HOST=$2

# Name of the management index
MANAGEMENT_INDEX=$3

# Document id of index element
DOC_ID=$4

# Add crawler
INDEX_RESULT=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer ${BEARER_TOKEN}" -X DELETE "https://${SEARCH_HOST}/dx/api/search/v2/${MANAGEMENT_INDEX}/${DOC_ID}")
echo $INDEX_RESULT
