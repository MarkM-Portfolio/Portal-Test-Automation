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

# this script accepts three parameters to produce search results for opensearch 

SEARCH_HOST=$1
TOKEN=$2
CONTENT_SOURCE_ID=$3

curlResponse=$(curl \
                    -X POST -H 'Content-Type: application/json' \
                    -H 'Accept: application/json, text/plain, */*' \
                    -H 'Authorization: Bearer '${TOKEN} \
                    -d '{ "query": {}, "scope": [ "'${CONTENT_SOURCE_ID}'" ]}' \
                    ${SEARCH_HOST}/dx/api/search/v2/search)

echo $curlResponse
