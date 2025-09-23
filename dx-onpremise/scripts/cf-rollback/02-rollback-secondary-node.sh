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

DX_USER=$1
DX_PASSWORD=$2

#Stop running servers
echo "Stop WebSphere_Portal"
sudo /opt/HCL/wp_profile/bin/stopServer.sh WebSphere_Portal -username $DX_USER -password $DX_PASSWORD

echo "Stop server1"
sudo /opt/HCL/AppServer/profiles/cw_profile/bin/stopServer.sh server1 -user $DX_USER -password $DX_PASSWORD

# Rollback binaries
echo "Started executing the imcl command for rolling back cf binaries"
cd /opt/HCL/InstallationManager/eclipse/tools/
sudo ./imcl rollback com.ibm.websphere.PORTAL.SERVER.v85 -installationDirectory /opt/HCL/PortalServer/

echo "Delete rollbackCFprogress folder if already exists"
sudo rm -rf /opt/HCL/wp_profile/ConfigEngine/log/rollbackCFprogress/

echo "Started executing rollbackCF command for cf-rollback"
sudo /opt/HCL/wp_profile/PortalServer/bin/rollbackCF.sh -DCwUserPwd=$DX_PASSWORD -DWasPassword=$DX_PASSWORD -DPortalAdminPwd=$DX_PASSWORD 

echo "Start server1"
sudo /opt/HCL/AppServer/profiles/cw_profile/bin/startServer.sh server1
