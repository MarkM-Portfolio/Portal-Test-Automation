#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

REMOTE_SEARCH_INSTANCE_IP=$1

echo "Download remote search package"
curl -O https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/RemoteSearch85FullInstall.zip
curl -O https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/was.repo.90505.nd.zip
curl -O https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-prereqs/image_prereqs/ibm-java-sdk-8.0-6.15-linux-x64-installmgr.zip
scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no RemoteSearch85FullInstall.zip centos@${REMOTE_SEARCH_INSTANCE_IP}:/tmp/dx-onpremise/scripts/helpers
scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no was.repo.90505.nd.zip centos@${REMOTE_SEARCH_INSTANCE_IP}:/tmp/msa/rtpmsa/projects/b/build.portal/builds/image_prereqs
scp -r -i test-automation-deployments.pem -o StrictHostKeyChecking=no ibm-java-sdk-8.0-6.15-linux-x64-installmgr.zip centos@${REMOTE_SEARCH_INSTANCE_IP}:/tmp/msa/rtpmsa/projects/b/build.portal/builds/image_prereqs
