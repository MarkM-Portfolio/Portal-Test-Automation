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

echo "Copy LTPAKeyExported"
sudo cp  /tmp/dx-onpremise/LTPAKeyExported /root/LTPAKeyExported


echo "Started executing ConfigEngine export-ssl-key-for-remote-search command"

sudo /opt/HCL/WebSphere/PortalRemoteSearch/ConfigEngine/./ConfigEngine.sh configure-remote-search-server-for-remote-search -DWasPassword=${WAS_PASSWORD} -DPortalAdminPwd=${DX_PASSWORD} -Dremote.search.host.name=${REMOTE_SEARCH_INSTANCE_IP} -Dportal.host.name=${INSTANCE_IP}


