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

# this script accepts a parameter for authentication of opensearch user 

SEARCH_HOST=$1
USER_NAME=$2
PASSWORD=$3

curlResponse=$(curl -kL -H 'Content-Type: application/json' -d '{ "username": "'$USER_NAME'", "password": "'$PASSWORD'"}' -X POST ${SEARCH_HOST}/dx/api/search/v2/admin/authenticate)

echo $curlResponse
