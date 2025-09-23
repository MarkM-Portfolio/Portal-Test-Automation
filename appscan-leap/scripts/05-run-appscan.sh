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

appscanDockerImageVersion=$1

docker run --env-file ~/appscan-leap/config/env.list --volume ~/appscan-leap/repos:/Apps --volume ~/appscan-leap/logs:/appscansource/logs quintana-docker.artifactory.cwp.pnp-hcl.com/appscan/source/cli:$appscanDockerImageVersion script /Apps/cli_script.txt

echo "AppScan scans done"