#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #


# FTP Access to sources
if [ XXX$1 != "XXX" ]; then
    echo "If mount needed, will mount as "$1
    PORTAL_ADMIN_PWD=$1
    CF_VERSION=$2
    BUILD_LABEL=$3

    echo "CF Version : "$CF_VERSION
    echo "BUILD LABEL : "$BUILD_LABEL
fi
CURRENT_CF_VERSION=$CF_VERSION
# Base version is pointing to WP8.5 GM and thus hardcording the PREVIOUS_CF_VERSION value to 0 as there was no CF installed
# PREVIOUS_CF_VERSION="CF$((${CURRENT_CF_VERSION: -2} - 1))" 
PREVIOUS_CF_VERSION="0"

#Rollback binaries
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl rollback com.ibm.websphere.PORTAL.SERVER.v85 -installationDirectory /opt/test/WebSphere9/PortalServer9/

#Rollback CF
cd /opt/test/WebSphere9/test_profile/PortalServer/bin
./rollbackCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

# Check for fixlevel=0, which is what we should see if we successfully rolled back to WP8.5 GM
if grep -q fixlevel=${PREVIOUS_CF_VERSION} /opt/test/WebSphere9/PortalServer9/wps.properties && grep -q fixlevel=${PREVIOUS_CF_VERSION} /opt/test/WebSphere9/test_profile/PortalServer/wps.properties
then 
echo "Rollback from $CF_VERSION to CF$PREVIOUS_CF_VERSION is successful ! "
else
echo "Rollback  from $CF_VERSION to CF$PREVIOUS_CF_VERSION failed  ! "
exit 1
fi 

