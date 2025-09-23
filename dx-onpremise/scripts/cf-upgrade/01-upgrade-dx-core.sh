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

DX_BUILD_VERSION=$1
DX_USER=$2
DX_PASSWORD=$3

echo "Stopping webserver and dmgr for cf-upgrade"
/opt/HCL/HTTPServer/bin/apachectl stop
if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
/opt/HCL/AppServer/profiles/dmgr01/bin/stopServer.sh dmgr -username $DX_USER -password $DX_PASSWORD
else
/opt/HCL/wp_profile/bin/stopServer.sh WebSphere_Portal -username $DX_USER -password $DX_PASSWORD
fi
/opt/HCL/AppServer/profiles/cw_profile/bin/stopServer.sh server1 -user $DX_USER -password $DX_PASSWORD
/opt/HCL/wp_profile/bin/serverStatus.sh -all -username $DX_USER -password $DX_PASSWORD
/opt/HCL/AppServer/profiles/cw_profile/bin/serverStatus.sh -all -username $DX_USER -password $DX_PASSWORD

echo "Started executing the imcl command for cf-upgrade"
cd /opt/HCL/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /tmp/upgrade/rtpmsa/projects/b/build.portal/builds/DX_Core/$DX_BUILD_VERSION/buildartifacts/iim.server/WP85_Server_CF/repository.config -installationDirectory /opt/HCL/PortalServer/ -acceptLicense

echo "Executing addtional steps for cf-upgrade"
sudo /opt/HCL/wp_profile/bin/osgiCfgInit.sh

if [ -d "/opt/HCL/AppServer/profiles/dmgr01/" ] 
then
echo "Starting DMGR server"
sudo /opt/HCL/AppServer/profiles/dmgr01/bin/startServer.sh dmgr
else
echo "Starting WAS server"
/opt/HCL/wp_profile/bin/startServer.sh WebSphere_Portal
fi

echo "Started executing applyCF command for cf-upgrade"
sudo /opt/HCL/wp_profile/PortalServer/bin/applyCF.sh -DCwUserPwd=$DX_PASSWORD -DWasPassword=$DX_PASSWORD -DPortalAdminPwd=$DX_PASSWORD -Dskip.profile.template.update=true

echo "reinstall dx-connect on primary node"
/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh reinstall-dxconnect-application -DWasPassword=$DX_PASSWORD

echo "Start server1"
/opt/HCL/AppServer/profiles/cw_profile/bin/startServer.sh server1
