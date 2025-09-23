#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
#
# The script determines the latest docker images to use, and pulls them
#


leapimagelevel=$1

docker pull quintana-docker.artifactory.cwp.pnp-hcl.com/dx-build-output/leap:$leapimagelevel

echo "Done with Leap Whitesource 04-01-pull-necessary-images"
