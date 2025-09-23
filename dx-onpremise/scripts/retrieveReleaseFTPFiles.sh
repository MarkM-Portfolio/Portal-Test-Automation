#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

IIM_VERSION="agent.installer.linux.gtk.x86_64_1.9.0.20190715_0328.zip"
mkdir -p ftp
cd ftp
CFDIR="DX_Core/${DX_IMAGE}"
IMAGE_PATH="CF${CF_VERSION}/HCL-DX-CF${CF_VERSION}"
if [[ "${CF_VERSION}"<="19" ]]; then
    IMAGE_PATH="CF${CF_VERSION}/${DX_IMAGE}/HCL-Portal-CF${CF_VERSION}"
fi
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/$IIM_VERSION -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/$IIM_VERSION --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/ifixes/8.5.0.0-WP-Server-IFPI59896.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/IFPI59896/8.5.0.0-WP-Server-IFPI59896.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/JDK8.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/JDK8.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WASND90.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/WASND90.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WP9.0_integration/portal9.packaging/WP85_Server.zip -o msa/rtpmsa/projects/b/build.portal/builds/WP9.0_integration/WP9.0_integration_20170630-0946/buildartifacts/portal9.packaging/WP90_Enable/SETUP/products/WP85_Server.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WP9.0_integration/portal9.packaging/WP85_Enable.zip -o msa/rtpmsa/projects/b/build.portal/builds/WP9.0_integration/WP9.0_integration_20170630-0946/buildartifacts/portal9.packaging/WP90_Enable/SETUP/products/WP85_Enable.zip --create-dirs

curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.5.2-ws-wasprod-ifph27509.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.5.2-ws-wasprod-ifph27509.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.5.0-ws-wasprod-ifph26220.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.5.0-ws-wasprod-ifph26220.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.0.9-ws-wasprod-ifph27157.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.0.9-ws-wasprod-ifph27157.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/was.repo.90505.nd.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/was.repo.90505.nd.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/ibm-java-sdk-8.0-6.15-linux-x64-installmgr.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/ibm-java-sdk-8.0-6.15-linux-x64-installmgr.zip --create-dirs


mkdir -p msa/rtpmsa/projects/b/build.portal/builds/$CFDIR/buildartifacts
cd msa/rtpmsa/projects/b/build.portal/builds/$CFDIR/buildartifacts

CF_RELEASE_ARCHIVE="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/portal/packaging/production/${IMAGE_PATH}_Server_Update.zip"
mkdir -p iim.server/WP85_Server_CF
cd iim.server/WP85_Server_CF
CF_FILE="./release.zip"
echo "Downloading build from: $CF_RELEASE_ARCHIVE"
curl $CF_RELEASE_ARCHIVE -o $CF_FILE

unzip $CF_FILE
unzip "WP8500CF${CF_VERSION}_Server.zip"

rm $CF_FILE
rm WP8500CF${CF_VERSION}_Server.zip


CF_REMOTE_SEARCH_ARCHIVE="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/portal/packaging/production/${IMAGE_PATH}_RemoteSearch_Update.zip"
cd ../..
mkdir -p iim.remotesearch/WP85_RemoteSearch_CF
cd iim.remotesearch/WP85_RemoteSearch_CF
RS_FILE="./remoteSearch.zip"
curl $CF_REMOTE_SEARCH_ARCHIVE -o $RS_FILE
unzip $RS_FILE
unzip "WP8500CF${CF_VERSION}_Remote.zip"
rm $RS_FILE
rm "WP8500CF${CF_VERSION}_Remote.zip"

V95_ENABLE_ARCHIVE="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic-prod/portal/packaging/production/V95/HCL-Portal-95_Enable_REPOS.zip"
V95_SERVER_ARCHIVE="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic-prod/portal/packaging/production/V95/HCL-Portal-95_Server_REPOS.zip"
cd ../..
mkdir -p iim.portal95
cd iim.portal95
V95_SERVER="./WP95_Server.zip"
V95_ENABLE="./WP95_Enable.zip"

curl $V95_SERVER_ARCHIVE -o $V95_SERVER
unzip $V95_SERVER
curl $V95_ENABLE_ARCHIVE -o $V95_ENABLE
unzip $V95_ENABLE

rm $V95_ENABLE
rm $V95_SERVER
