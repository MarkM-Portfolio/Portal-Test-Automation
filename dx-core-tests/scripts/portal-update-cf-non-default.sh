#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. #
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

echo "iptables flush start"
iptables -F
echo "iptables flush complete"

#Renew the cert that expires each year
echo "Renew the WAS cert"
cd /opt/test/WebSphere9/test_profile/bin
./startServer.sh WebSphere_Portal
echo "AdminTask.renewCertificate('[-keyStoreName NodeDefaultKeyStore -keyStoreScope (cell):ip-10-134-210-93Cell:(node):ip-10-134-210-93Node -certificateAlias default ]')
AdminConfig.save()" > renewCert.py
./wsadmin.sh -user portaladmin -password $PORTAL_ADMIN_PWD -lang jython -f renewCert.py
./stopServer.sh WebSphere_Portal -username portaladmin -password $PORTAL_ADMIN_PWD

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

#Run Installation Manager
echo "Starting to Install Portal"
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /opt/zips/$CF_VERSION/repository.config -installationDirectory /opt/test/WebSphere9/PortalServer9/ -acceptLicense

#Apply CF
echo "Starting to Apply CF"
cd /opt/test/WebSphere9/test_profile/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

if grep -q fixlevel=$CF_VERSION /opt/test/WebSphere9/PortalServer9/wps.properties && grep -q fixlevel=$CF_VERSION /opt/test/WebSphere9/test_profile/PortalServer/wps.properties
then 
    echo "$CF_VERSION Installation Successful !"
else
    echo "$CF_VERSION Installation Failed !"
    exit 1
fi 