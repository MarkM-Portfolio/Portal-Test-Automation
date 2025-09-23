#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

DX_PASSWORD=$1
DOMAIN_NAME=$2

# Configure SSO
/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh configure-single-signon -DWasPassword=${DX_PASSWORD} -Ddomain=${DOMAIN_NAME} -DattributePropagation=true
