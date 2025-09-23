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
 
# Test a crawler connection.
# The following parameter are needed in the correct order to create the new entry.

# Bearer token for search authorization
BEARER_TOKEN=$1

# Search host URL
SEARCH_HOST=$2

# Crawler ID created in management index
CRAWLER_ID=$3

# Test crawler connection
CRAWLER_RESULT=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer ${BEARER_TOKEN}" -X POST "https://${SEARCH_HOST}/dx/api/search/v2/crawlers/${CRAWLER_ID}/test")
echo $CRAWLER_RESULT
