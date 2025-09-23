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
 
# This script authenticates a user to the search middleware.
# On successful authentication the script returns an authorization bearer token.
# The following parameter are needed in the correct order to create the new entry.

# Search host URL
SEARCH_HOST=$1
# Search admin user
SEARCH_ADMIN=$2
# Password for search admin
SEARCH_ADMIN_PASSWORD=$3

# Authenticate
AUTH_CODE=$(curl -s -H 'Content-Type: application/json' -X POST "https://${SEARCH_HOST}/dx/api/search/v2/admin/authenticate" -d "{ \"username\": \"${SEARCH_ADMIN}\", \"password\": \"${SEARCH_ADMIN_PASSWORD}\" }")
echo $AUTH_CODE
