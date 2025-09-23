#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. #
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
#BUILD_CONTEXT=$(echo "$BUILD_LABEL" | sed 's/_[0-9]\{8\}-[0-9]\{6\}.*//g')
#echo "Current build-context is: $BUILD_CONTEXT"
#echo "Current build-label is: $BUILD_LABEL"

# Create directory for the source downloads
if [ ! -d "/opt/zips" ]; then
    mkdir /opt/zips/
fi
cd /opt/zips/

# create CF directory
mkdir $CF_VERSION
cd /opt/zips/$CF_VERSION
echo "Working in directory /opt/zips/$CF_VERSION"

SERVER_ZIP_ARTIFACTORY_URL="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/portal/packaging/production/${CF_VERSION}/HCL-DX-${CF_VERSION}_Server_Update.zip"
echo "Downloading server from $SERVER_ZIP_ARTIFACTORY_URL"
curl $SERVER_ZIP_ARTIFACTORY_URL -o HCL-DX_Server_Update.zip
yes | unzip HCL-DX_Server_Update.zip
yes | unzip "WP8500${CF_VERSION}_Server.zip"
echo "Latest build unzipped"

#Run Installation Manager
echo "Starting to Install Portal"
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories "/opt/zips/${CF_VERSION}/repository.config" -installationDirectory /opt/IBM/WebSphere/PortalServer/ -acceptLicense

#Apply CF
echo "Starting to Apply CF"
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

if grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q fixlevel=$CF_VERSION /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties
then 
    echo "$CF_VERSION Installation Successful !"
else
    echo "$CF_VERSION Installation Failed !"
    exit 1
fi 