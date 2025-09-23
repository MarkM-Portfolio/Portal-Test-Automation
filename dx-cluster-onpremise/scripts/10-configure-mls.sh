#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

DX_USERID=$1
DX_PASSWORD=$2

# Configure MLS
echo "Running Configure MLS config task"
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh register-wcm-mls -DPortalAdminId=${DX_USERID} -DPortalAdminPwd=${DX_PASSWORD} -DWasUserId=${DX_USERID} -DWasPassword=${DX_PASSWORD}
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh deploy-wcm-mls -DPortalAdminId=${DX_USERID} -DPortalAdminPwd=${DX_PASSWORD} -DWasUserId=${DX_USERID} -DWasPassword=${DX_PASSWORD}
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh run-wcm-admin-task-member-fixer -DPortalAdminId=${DX_USERID} -DPortalAdminPwd=${DX_PASSWORD} -DWasUserId=${DX_USERID} -DWasPassword=${DX_PASSWORD} -Dlibrary="ML Configuration" -Dfix=true -DinvalidDn=update -DmismatchedId=update -DaltDn=update
