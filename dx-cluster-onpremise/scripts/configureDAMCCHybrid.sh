#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

HYBRID_KUBE_HOST=$1
DX_PASSWORD=$2

# Configure CC
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh enable-headless-content -Dstatic.ui.url=https://${HYBRID_KUBE_HOST}/dx/ui/content/static -DWasPassword=${DX_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

# Configure DAM
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh enable-media-library -Dstatic.ui.url=https://${HYBRID_KUBE_HOST}/dx/ui/dam/static -DWasPassword=${DX_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

# Configure DX Picker
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh enable-dx-picker -Ddxpicker.static.ui.url=https://${HYBRID_KUBE_HOST}/dx/ui/picker/static -DWasPassword=${DX_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}
