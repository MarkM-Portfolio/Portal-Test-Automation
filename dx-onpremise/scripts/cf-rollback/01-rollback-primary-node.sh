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

#Stop Running Servers
echo "Stopping webserver and dmgr for cf-rollback"
/opt/HCL/HTTPServer/bin/apachectl stop
if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
sudo /opt/HCL/AppServer/profiles/dmgr01/bin/stopServer.sh dmgr -username $DX_USER -password $DX_PASSWORD
else
sudo /opt/HCL/wp_profile/bin/stopServer.sh WebSphere_Portal -username $DX_USER -password $DX_PASSWORD
fi
sudo /opt/HCL/AppServer/profiles/cw_profile/bin/stopServer.sh server1 -user $DX_USER -password $DX_PASSWORD
sudo /opt/HCL/wp_profile/bin/serverStatus.sh -all -username $DX_USER -password $DX_PASSWORD
sudo /opt/HCL/AppServer/profiles/cw_profile/bin/serverStatus.sh -all -username $DX_USER -password $DX_PASSWORD

#Rollback binaries
echo "Started executing the imcl command for rolling back cf binaries"
 cd /opt/HCL/InstallationManager/eclipse/tools/
sudo ./imcl rollback com.ibm.websphere.PORTAL.SERVER.v85 -installationDirectory /opt/HCL/PortalServer/


if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
echo "Starting DMGR server"
sudo /opt/HCL/AppServer/profiles/dmgr01/bin/startServer.sh dmgr
else
echo "Starting WebSphere_Portal server"
sudo /opt/HCL/wp_profile/bin/startServer.sh WebSphere_Portal
fi

echo "Delete rollbackCFprogress folder if already exists"
sudo rm -rf /opt/HCL/wp_profile/ConfigEngine/log/rollbackCFprogress/

echo "Started executing rollbackCF command for cf-rollback"
sudo /opt/HCL/wp_profile/PortalServer/bin/rollbackCF.sh -DCwUserPwd=$DX_PASSWORD -DWasPassword=$DX_PASSWORD -DPortalAdminPwd=$DX_PASSWORD 

echo "Start server1"
sudo /opt/HCL/AppServer/profiles/cw_profile/bin/startServer.sh server1



