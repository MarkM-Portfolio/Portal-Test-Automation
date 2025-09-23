#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

IIM_VERSION="agent.installer.linux.gtk.x86_64_1.9.0.20190715_0328.zip"

mkdir -p ftp
cd ftp
CFDIR="DX_Core/${DX_IMAGE}"
echo "SELECTED DX_IMAGE: $DX_IMAGE"
echo "CFDIR: $CFDIR"
ARTIFACTORY_ARCHIVE="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-output/builds/${DX_CORE_BUILD_VERSION}.tar"                                                                                                            

curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/$IIM_VERSION -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/$IIM_VERSION --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/ifixes/8.5.0.0-WP-Server-IFPI59896.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/IFPI59896/8.5.0.0-WP-Server-IFPI59896.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/JDK8.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/JDK8.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WASND90.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/WASND90.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WP9.0_integration/portal9.packaging/WP85_Server.zip -o msa/rtpmsa/projects/b/build.portal/builds/WP9.0_integration/WP9.0_integration_20170630-0946/buildartifacts/portal9.packaging/WP90_Enable/SETUP/products/WP85_Server.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/WP9.0_integration/portal9.packaging/WP85_Enable.zip -o msa/rtpmsa/projects/b/build.portal/builds/WP9.0_integration/WP9.0_integration_20170630-0946/buildartifacts/portal9.packaging/WP90_Enable/SETUP/products/WP85_Enable.zip --create-dirs

curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.5.2-ws-wasprod-ifph27509.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.5.2-ws-wasprod-ifph27509.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.5.0-ws-wasprod-ifph26220.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.5.0-ws-wasprod-ifph26220.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/9.0.0.9-ws-wasprod-ifph27157.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/9.0.0.9-ws-wasprod-ifph27157.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/dx-build-prereqs/image_prereqs/was.repo.90515.nd.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/was.repo.90515.nd.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/dx-build-prereqs/image_prereqs/ibm-java-sdk-8.0-8.0-linux-x64-installmgr.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/ibm-java-sdk-8.0-8.0-linux-x64-installmgr.zip --create-dirs

mkdir -p msa/rtpmsa/projects/b/build.portal/builds/$CFDIR
cd msa/rtpmsa/projects/b/build.portal/builds/$CFDIR
ARC_FILE="./DX_Core.tar"

echo "Downloading build from: $ARTIFACTORY_ARCHIVE"
echo "buildartifacts/iim.remotesearch"
curl $ARTIFACTORY_ARCHIVE -o $ARC_FILE
tar -xf $ARC_FILE buildartifacts/iim.portal95
tar -xf $ARC_FILE buildartifacts/iim.server
tar -xf $ARC_FILE buildartifacts/iim.remotesearch

rm -f buildartifacts/iim.server/WP8500CF19_Server.zip
rm -f buildartifacts/iim.remotesearch/WP8500CF19_Remote.zip
rm -f buildartifacts/iim.server/8.5-9.0-9.5-WP-WCM-Combined-CFDXQ10041-Server-CF19.zip
rm $ARC_FILE
