#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

INSTALLBASE=/tmp/im
WAS_PASSWORD=wpsadmin
DX_PASSWORD=wpsadmin

echo "Started executing applyCF command"

/opt/HCL/wp_profile/PortalServer/bin/applyCF.sh \
    -DCwUserPwd=${WAS_PASSWORD} \
    -DWasPassword=${WAS_PASSWORD} \
    -DPortalAdminPwd=${DX_PASSWORD}

echo "Started executing ConfigEngine stop-portal-server command"

/opt/HCL/wp_profile/ConfigEngine/./ConfigEngine.sh stop-portal-server -DWasPassword=${WAS_PASSWORD}

echo "Started executing the installc command for WASFP9055JDK806.xml"

/opt/HCL/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/WASFP9055JDK806.xml \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/WASFP9055JDK806.log

echo "Started executing the imcl command for WASFP9055APARs.xml"

/opt/HCL/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/WASFP9055APARs.xml \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/WASFP9055APARs.log

echo "Started executing the imcl command for Portal95Response.xml"

/opt/HCL/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/Portal95Response.xml \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/Install.Portal95Response.log

echo "Started executing the imcl command deleteSavedFiles"

/opt/HCL/InstallationManager/eclipse/tools/imcl deleteSavedFiles
