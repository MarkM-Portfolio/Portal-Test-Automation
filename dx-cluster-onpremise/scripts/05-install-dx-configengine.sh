#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

WAS_PASSWORD=wpsadmin
DX_PASSWORD=wpsadmin

echo "Started executing ConfigEngine update-jsfportletbridge9x command"

/opt/IBM/WebSphere/wp_profile/ConfigEngine/./ConfigEngine.sh update-jsfportletbridge9x -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

echo "Started executing ConfigEngine enable-v95-UI-features command"

/opt/IBM/WebSphere/wp_profile/ConfigEngine/./ConfigEngine.sh enable-v95-UI-features -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

echo "Started executing ConfigEngine enable-portal-light-startup-performance command"

/opt/IBM/WebSphere/wp_profile/ConfigEngine/./ConfigEngine.sh enable-portal-light-startup-performance -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

echo "Started executing ConfigEngine enable-digitalasset-support command"

/opt/IBM/WebSphere/wp_profile/ConfigEngine/./ConfigEngine.sh enable-digitalasset-support -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

echo "Started executing ConfigEngine action-deploy-tiny-editors command"

/opt/IBM/WebSphere/wp_profile/ConfigEngine/./ConfigEngine.sh action-deploy-tiny-editors -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD}

cd /opt/IBM/WebSphere/wp_profile/ConfigEngine/log

echo "Finding the ConfigTrace.log"

find . ! -name ConfigTrace.log -delete
