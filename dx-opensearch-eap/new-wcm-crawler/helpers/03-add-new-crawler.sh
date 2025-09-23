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
 
# Create new crawler entry in the crawler management index.
# The following parameter are needed in the correct order to create the new entry.

# Bearer token for search authorization
BEARER_TOKEN=$1

# Search host URL
SEARCH_HOST=$2

# Content ID created in management index
CONTENT_ID=$3

# WCM host, user Id and password to access WCM data source
WCM_HOST=$4
WCM_USER=$5
WCM_USER_PASSWORD=$6

# Add crawler
INDEX_RESULT=$(curl -s -H 'Content-Type: application/json' -H "Authorization: Bearer ${BEARER_TOKEN}" -X POST "https://${SEARCH_HOST}/dx/api/search/v2/crawlers" -d "{ \"contentSource\": \"${CONTENT_ID}\", \"type\": \"wcm\", \"configuration\": { \"targetDataSource\": \"https://${WCM_HOST}/wps/seedlist/myserver?SeedlistId=&Source=com.ibm.workplace.wcm.plugins.seedlist.retriever.WCMRetrieverFactory&Action=GetDocuments\", \"httpProxy\": \"\", \"schedule\": \"\", \"security\": { \"type\": \"basic\", \"username\": \"${WCM_USER}\", \"password\": \"${WCM_USER_PASSWORD}\" }, \"maxCrawlTime\": 0, \"maxRequestTime\": 0 } }")
echo $INDEX_RESULT
