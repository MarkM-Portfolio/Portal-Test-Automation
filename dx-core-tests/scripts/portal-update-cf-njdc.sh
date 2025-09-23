#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd.2022. All Rights Reserved.        #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #


# FTP Access to sources
if [ XXX$1 != "XXX" ]; then
    echo "If mount needed, will mount as "$1 $2 $3 $4 $5 $6
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
curl -o $SERVER_ZIP_URL
yes | unzip -o WP8500${CF_VERSION}_Server.zip 
# tar -xvf inst_repo.tar
echo "Latest build unzipped"

echo "Stopping portal server"
cd /opt/HCL/wp_profile/bin
./stopServer.sh WebSphere_Portal -username $4 -password $4

echo "Set CF version to 207 as a workaround for daily update"
cd /opt/HCL/PortalServer
sed -i "s/fixlevel=.*/fixlevel=CF207/g" wps.properties
cd /opt/HCL/wp_profile/PortalServer/
sed -i "s/fixlevel=.*/fixlevel=CF207/g" wps.properties

#Run Installation Manager
echo "Starting to Install Portal"
cd /opt/HCL/InstallationManager/eclipse/tools/
./imcl install com.ibm.websphere.PORTAL.SERVER.v85 -repositories /opt/zips/$CF_VERSION/repository.config -installationDirectory /opt/HCL/PortalServer/ -acceptLicense


#Apply CF
echo "Starting to Apply CF"
cd /opt/HCL/wp_profile/PortalServer/bin
./applyCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

echo "Starting portal server"
cd /opt/HCL/wp_profile/bin
./startServer.sh WebSphere_Portal

if grep -q fixlevel=$CF_VERSION /opt/HCL/PortalServer/wps.properties && grep -q fixlevel=$CF_VERSION /opt/HCL/wp_profile/PortalServer/wps.properties
then 
    echo "$CF_VERSION Installation Successful !"
else
    echo "$CF_VERSION Installation Failed !"
    exit 1
fi 
