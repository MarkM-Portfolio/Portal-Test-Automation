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

# this script accepts three parameters to create crawlers for opensearch 

SEARCH_HOST=$1
TOKEN=$2
CONTENT_SOURCE_ID=$3
CONTENT_SOURCE_HOST=$4

curlResponse=$(curl \
                    -X POST -H 'Content-Type: application/json' \
                    -H 'Accept: application/json, text/plain, */*' \
                    -H 'Authorization: Bearer '${TOKEN} \
                    -d '{"contentSource": "'${CONTENT_SOURCE_ID}'", "type": "wcm", "configuration": {"targetDataSource": "'${CONTENT_SOURCE_HOST}'/wps/seedlist/myserver?SeedlistId=&Source=com.ibm.workplace.wcm.plugins.seedlist.retriever.WCMRetrieverFactory&Action=GetDocuments", "schedule": "*/15 * * * *", "security": {"type": "basic", "username": "wpsadmin", "password": "wpsadmin"}, "maxCrawlTime": 3600, "maxRequestTime": 60}}' \
                    ${SEARCH_HOST}/dx/api/search/v2/crawlers)
                    
echo  "$curlResponse"
