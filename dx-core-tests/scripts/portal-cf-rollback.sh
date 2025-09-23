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
    PREVIOUS_CF_VERSION=$4

    echo "CF Version : "$CF_VERSION
    echo "BUILD LABEL : "$BUILD_LABEL
fi
CURRENT_CF_VERSION=$CF_VERSION
# Base version is pointing to CF16 and thus hardcording the PREVIOUS_CF_VERSION value to CF16
# PREVIOUS_CF_VERSION="CF$((${CURRENT_CF_VERSION: -2} - 1))" 
# PREVIOUS_CF_VERSION="CF18"

#Rollback binaries
cd /opt/IBM/InstallationManager/eclipse/tools/
./imcl rollback com.ibm.websphere.PORTAL.SERVER.v85 -installationDirectory /opt/IBM/WebSphere/PortalServer/

#Rollback CF
cd /opt/IBM/WebSphere/wp_profile/PortalServer/bin
./rollbackCF.sh -DPortalAdminPwd=$PORTAL_ADMIN_PWD -DWasPassword=$PORTAL_ADMIN_PWD

if grep -q fixlevel=${PREVIOUS_CF_VERSION} /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q fixlevel=${PREVIOUS_CF_VERSION} /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties
then 
echo "Rollback from $CF_VERSION to $PREVIOUS_CF_VERSION is successful ! "
else
echo "Rollback  from $CF_VERSION to $PREVIOUS_CF_VERSION failed  ! "
exit 1
fi 

