#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
echo "currentBuild is $1"
echo "CF number is $2"

echo "Downloading Remote Search CF package"
mkdir /opt/zips/remotesearch
cd /opt/zips/remotesearch
aws s3 cp s3://aws-hcl-dx-delivery/msa/rtpmsa/projects/b/build.portal/builds/DX_Core/$1/buildartifacts/iim.remotesearch/WP8500$2_Remote.zip .
unzip WP8500$2_Remote.zip

echo "Extracting Remote Search build number and updating response file"
cd /opt/zips/remotesearch/8500$2/repository
rs_build_number=$( cat update_com.ibm.websphere.PORTAL.REMOTESEARCH.v85_8.5.0.*.xml | grep -o -P "version='8.5.0.{0,15}" )
echo "Remote Search build number is $rs_build_number"
echo "Replacing Remote Search build number in response file"
sed -i "s/version='8.5.0.20220707_1420'/${rs_build_number}/g" /opt/zips/install_rs_cf.xml


echo "Installing Remote Search"
pkill -9 -f InstallationManager
cd /opt/zips
/opt/IBM/InstallationManager/eclipse/tools/imcl \
    -acceptLicense \
    input /opt/zips/install_rs_cf.xml \
    -showProgress \
    -preferences com.ibm.cic.common.core.preferences.preserveDownloadedArtifacts=false \
    -log /opt/zips/Install.RemoteSearch85Install.log

find Install.RemoteSearch85Install.log | xargs -I {} bash -c 'if grep -Fq "Portal Remote Search has been updated" {} ; then echo Remote Search has been updated {}; else echo "Remote Search failed to update.  See /opt/zips/Install.RemoteSearch85Install.log for more information"; exit 1 ; fi'