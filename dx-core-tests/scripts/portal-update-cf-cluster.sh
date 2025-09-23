#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2024. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #


# FTP Access to sources
if [ XXX$1 != "XXX" ]; then
    echo "If mount needed, will mount as "$1
    FTP_USER=$1
    FTP_PASSWORD=$2
    FTP_HOST=$3
    PORTAL_ADMIN_PWD=$4
    CF_VERSION=$5
    BUILD_LABEL=$6
fi

# Extract build context using a regex
# Transforms WP95_CF171_integration_20200127-210418_rohan_release_95_CF171 into WP95_CF171_integration
BUILD_CONTEXT=$(echo "$BUILD_LABEL" | sed 's/_[0-9]\{8\}-[0-9]\{6\}.*//g')
echo "Current build-context is: $BUILD_CONTEXT"
echo "Current build-label is: $BUILD_LABEL"

# Create directory for the source downloads
if [ ! -d "/opt/zips" ]; then
    mkdir /opt/zips/
fi
cd /opt/zips/

# create CF directory
mkdir $CF_VERSION
cd /opt/zips/$CF_VERSION
echo "Working in directory /opt/zips/$CF_VERSION"

# Download CF Server zip and unzip
SERVER_ZIP_URL="$FTP_HOST/msa/rtpmsa/projects/b/build.portal/builds/$BUILD_CONTEXT/$BUILD_LABEL/buildartifacts/iim.server/WP8500${CF_VERSION}_Server.zip"
echo "Downloading server from $SERVER_ZIP_URL"
curl $SERVER_ZIP_URL -u $FTP_USER:$FTP_PASSWORD -o WP8500${CF_VERSION}_Server.zip
yes | unzip WP8500${CF_VERSION}_Server.zip 
echo "Latest build unzipped"

#Run Installation Manager on primary node
echo "Starting to install CF on the primary node"
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /opt/zips/$CF_VERSION/repository.config -installationDirectory /opt/IBM/WebSphere/PortalServer/ -acceptLicense

#Start the deployment manager
echo "Starting the deployment manager"
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/startManager.sh

#Apply CF on primary node
echo "Starting to Apply CF on the primary node"
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

#Stop the deployment manager
echo "Stopping the deployment manager"
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/stopManager.sh -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD

#Run Installation Manager on secondary node
echo "Starting to install CF on the secondary node"
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /opt/zips/$CF_VERSION/repository.config -installationDirectory /opt/IBM/WebSphere/PortalServer_1/ -acceptLicense

#Start the deployment manager
echo "Starting the deployment manager"
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/startManager.sh

# Remove any ConfigEngine lock files if they exist
echo "Revoving ConfigEngine lock files"
rm -f /opt/IBM/WebSphere/wp_profileSecondNode/ConfigEngine/ConfigEngine.lck
rm -f /opt/IBM/WebSphere/wp_profileSecondNode/ConfigEngine/log/ConfigMessages.log.lck
rm -f /opt/IBM/WebSphere/AppServer_1/java/8.0/jre/.systemPrefs/.system.lock

#Apply CF on secondary node
echo "Starting to Apply CF on the secondary node"
cd /opt/IBM/WebSphere/wp_profileSecondNode/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

#Download test zips
echo "Copy the test-zips into this environment"
FTP_NAME=$(echo "$FTP_HOST" | sed 's/ftp:\/\///g')
wget -r --user=$FTP_USER --password=$FTP_PASSWORD $FTP_HOST/msa/rtpmsa/projects/b/build.portal/builds/$BUILD_CONTEXT/$BUILD_LABEL/buildartifacts/test.* -P /
yes | cp -rf $FTP_NAME/msa/rtpmsa/projects/b/build.portal/builds/$BUILD_CONTEXT/$BUILD_LABEL/buildartifacts/test.*/*  /shared/care/

#Update port in wtf.properties so tests run
sed -i 's/server1.WpsHostPort=/server1.WpsHostPort=80/g' /root/wtf.properties
sed -i 's/server1.WpsHostSecurePort=/server1.WpsHostSecurePort=80/g' /root/wtf.properties

if grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties && grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/PortalServer_1/wps.properties && grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/wp_profileSecondNode/PortalServer/wps.properties
then 
    echo "$CF_VERSION Installation Successful !"
else
    echo "$CF_VERSION Installation Failed !"
    exit 1
fi 

# Restart the cluster here before testing
echo "Restarting the Cluster"
/opt/IBM/WebSphere/wp_profile/bin/stopServer.sh WebSphere_Portal -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD
/opt/IBM/WebSphere/wp_profileSecondNode/bin/stopServer.sh WebSphere_Portal_SecondNode -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD
/opt/IBM/WebSphere/wp_profile/bin/stopNode.sh -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD
/opt/IBM/WebSphere/wp_profileSecondNode/bin/stopNode.sh -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD
/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/stopManager.sh -username $PORTAL_ADMIN_PWD -password $PORTAL_ADMIN_PWD

/opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/startManager.sh
/opt/IBM/WebSphere/wp_profile/bin/startNode.sh
/opt/IBM/WebSphere/wp_profileSecondNode/bin/startNode.sh
/opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal 
/opt/IBM/WebSphere/wp_profileSecondNode/bin/startServer.sh WebSphere_Portal_SecondNode

echo "AdminControl.invoke('WebSphere:name=WebServer,process=dmgr,platform=common,node=dmgrNode01,version=9.0.5.0,type=WebServer,mbeanIdentifier=WebServer,cell=dmgrCell01,spec=1.0', 'start', '[dmgrCell01 node1Node WebServer1]')" > /opt/zips/startWebServer.py
cd /opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/zips/startWebServer.py

# Synchronize the cluster nodes
echo "Synchronizing the cluster nodes"
echo "AdminControl.invoke('WebSphere:name=cellSync,process=dmgr,platform=common,node=dmgrNode01,version=9.0.5.0,type=CellSync,mbeanIdentifier=cellSync,cell=dmgrCell01,spec=1.0', 'syncNode', '[node1Node]')" > /opt/zips/syncNode1.py
cd /opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/zips/syncNode1.py

echo "AdminControl.invoke('WebSphere:name=cellSync,process=dmgr,platform=common,node=dmgrNode01,version=9.0.5.0,type=CellSync,mbeanIdentifier=cellSync,cell=dmgrCell01,spec=1.0', 'syncNode', '[node2Node]')" > /opt/zips/syncNode2.py
cd /opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin/
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/zips/syncNode2.py

echo "Cluster has been restarted and nodes have been synchronized"

# Install xvfb for WTF tests
echo "installing xvfb"
yes | yum install xorg-x11-server-Xvfb