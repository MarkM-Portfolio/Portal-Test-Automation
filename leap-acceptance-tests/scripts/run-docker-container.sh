#!/bin/sh
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

# Running dxubi docker image as dummy container
docker run -p 80:10039 -p 443:10041 --name DXUBI quintana-docker.artifactory.cwp.pnp-hcl.com/dx-build-output/common/dxubi:v1.0.0_8.7-1031 true

if [ "$?" != "0" ]; then
  echo "Docker test run failed"
  exit 1
else
  echo "Docker test run succesful"
  exit 0
fi