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

curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/JDK8.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/JDK8.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/dx-build-prereqs/image_prereqs/was.repo.90515.nd.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/was.repo.90515.nd.zip --create-dirs
curl https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic-prod/dx-build-prereqs/image_prereqs/ibm-java-sdk-8.0-8.0-linux-x64-installmgr.zip -o msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/ibm-java-sdk-8.0-8.0-linux-x64-installmgr.zip --create-dirs
