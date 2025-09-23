#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

WAS_PASSWORD=wpsadmin
DX_PASSWORD=wpsadmin

if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
    echo 'Restarting the dmgr ...'
    cd /opt/HCL/AppServer/profiles/dmgr01/bin
    sudo ./stopServer.sh dmgr -username ${WAS_PASSWORD} -password ${DX_PASSWORD}
    sudo ./startServer.sh dmgr
else
    echo "No dmgr found"
fi

echo "Started executing ConfigEngine export-ssl-key-for-remote-search command"

sudo /opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh configure-portal-for-remote-search -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD} -Dremote.search.host.name=${REMOTE_SEARCH_INSTANCE_IP} -Dremote.search.iiop.url=iiop://${REMOTE_SEARCH_INSTANCE_IP}:2809

cd /opt/HCL/wp_profile/ConfigEngine/log

echo "Finding the ConfigTrace.log"

sudo find . ! -name ConfigTrace.log -delete
