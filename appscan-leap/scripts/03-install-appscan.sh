#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install AppScan

appscanDockerImageVersion=$1

docker pull quintana-docker.artifactory.cwp.pnp-hcl.com/appscan/source/cli:$appscanDockerImageVersion

cd ~/appscan-leap
mkdir logs