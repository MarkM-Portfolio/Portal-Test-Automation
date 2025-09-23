#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

DX_USER=$1
DX_PASSWORD=$2

echo "update and restart remote search"
sudo kill -9 $(ps aux | grep java | grep -v 'grep' | awk '{print $2}')
/opt/HCL/AppServer/profiles/prs_profile/bin/stopServer.sh server1 -user $DX_USER -password $DX_PASSWORD
cd /opt/HCL/InstallationManager/eclipse/tools/
sudo ./imcl -acceptLicense input /tmp/dx-onpremise/scripts/helpers/UpdateRemoteSearch.xml -showProgress -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false
sudo kill -9 $(ps aux | grep java | grep -v 'grep' | awk '{print $2}')
