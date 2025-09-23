#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023, 2024. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

appscanDockerImageVersion=$1

chmod -R 777 ~/appscan/logs
chmod -R 777 ~/appscan/repos/reports
docker run --env-file ~/appscan/config/env.list --volume ~/appscan/repos:/Apps --volume ~/appscan/logs:/appscansource/logs quintana-docker.artifactory.cwp.pnp-hcl.com/appscan/source/cli:$appscanDockerImageVersion script /Apps/cli_script.txt

echo "AppScan scans done"
