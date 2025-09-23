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

# this script accepts two parameters to create contentsources for opensearch 

SEARCH_HOST=$1
TOKEN=$2
CONTENT_SOURCE_HOST=$3

curlResponse=$(curl \
                    -X POST -H 'Content-Type: application/json' \
                    -H 'Accept: application/json, text/plain, */*' \
                    -H 'Authorization: Bearer '${TOKEN} \
                    -d '{ "name": "WCMConfiguration", "type": "wcm", "aclLookupHost": "'${CONTENT_SOURCE_HOST}'"}' \
                    ${SEARCH_HOST}/dx/api/search/v2/contentsources)

echo $curlResponse
