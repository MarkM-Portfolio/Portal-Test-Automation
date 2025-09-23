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


ROUTE53_HOSTNAME=$1
PRIMARY_NODE_NAME=$2
sed -i "s|localhost|${ROUTE53_HOSTNAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85BaseResponse.xml
sed -i "s|localhost|${ROUTE53_HOSTNAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85CF18Response.xml
sed -i "s|localhost|${ROUTE53_HOSTNAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85CF19Response.xml
sed -i "s|localhost|${ROUTE53_HOSTNAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal95Response.xml
sed -i "s|localhost|${ROUTE53_HOSTNAME}|g;" /tmp/dx-onpremise/scripts/helpers/RemoteSearch85Response.xml


sed -i "s|dockerNode|${PRIMARY_NODE_NAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85BaseResponse.xml
sed -i "s|dockerNode|${PRIMARY_NODE_NAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85CF18Response.xml
sed -i "s|dockerNode|${PRIMARY_NODE_NAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal85CF19Response.xml
sed -i "s|dockerNode|${PRIMARY_NODE_NAME}|g;" /tmp/dx-onpremise/scripts/helpers/Portal95Response.xml
sed -i "s|dockerNode|${PRIMARY_NODE_NAME}|g;" /tmp/dx-onpremise/scripts/helpers/RemoteSearch85Response.xml


mkdir -p $INSTALLBASE

cd $INSTALLBASE

echo "Started unzipping iim setup package into $INSTALLBASE"

unzip /tmp/dx-onpremise/scripts/helpers/iim_setup.zip

echo "Displaying the content of $INSTALLBASE below after unzip operation"

ls -ltrha

echo "Started executing the installc command"

$INSTALLBASE/installc \
    -acceptLicense \
    -accessRights nonAdmin \
    -installationDirectory "/opt/HCL/InstallationManager"  \
    -dataLocation "/tmp/appdata" \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false

echo "Displaying the content of $INSTALLBASE below after getting remote search base"

ls -ltrha

echo "Started unzipping remote search setup package into $INSTALLBASE"

sudo unzip /tmp/dx-onpremise/scripts/helpers/RemoteSearch85FullInstall.zip

echo "Started executing the imcl command for RemoteSearch85Response.xml"

/opt/HCL/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /tmp/dx-onpremise/scripts/helpers/RemoteSearch85Response.xml \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log $INSTALLBASE/Install.RemoteSearch85Response.log

echo "Started executing the imcl command deleteSavedFiles"

/opt/HCL/InstallationManager/eclipse/tools/imcl deleteSavedFiles
