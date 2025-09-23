#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

INSTALLBASE=/tmp/im
WAS_ADMIN=wpsadmin
WAS_PASSWORD=wpsadmin

mkdir -p $INSTALLBASE

cd $INSTALLBASE

echo "Started unzipping iim setup package into $INSTALLBASE"

unzip /tmp/dx-onpremise/scripts/helpers/iim_setup.zip

echo "Displaying the content of $INSTALLBASE below after unzip operation"

ls -ltrha

echo "Started executing the installc command for Portal85"

$INSTALLBASE/installc \
    -acceptLicense \
    -accessRights nonAdmin \
    -installationDirectory "/opt/IBM/InstallationManager"  \
    -dataLocation "/tmp/appdata" \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false

echo "Started executing the imcl command for BinaryPortal85BaseResponse.xml"

/opt/IBM/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/BinaryPortal85BaseResponse.xml \
    -showVerboseProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/Install.Portal85Base.log

echo "Stopping the CW Profile Server after installc and imcl of Portal85"
/opt/IBM/WebSphere/AppServer/profiles/cw_profile/bin/./stopServer.sh server1 -user ${WAS_ADMIN} -password ${WAS_PASSWORD}

echo "Started executing the imcl command for BinaryPortal85CF19Response.xml"
/opt/IBM/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/BinaryPortal85CF19Response.xml \
    -showVerboseProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/Install.PortalCF19.log