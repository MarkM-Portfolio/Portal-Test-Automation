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

regex=$1
CONFIG_FILE=$2
artifactoryPath=$3
artifactoryurl="https://artifactory.cwp.pnp-hcl.com/artifactory/list/$artifactoryPath"

while read image; do

  echo "----------------------------------------------------"
  echo "Pull and run the image: $artifactoryPath/$image"
  echo "----------------------------------------------------"
  echo ""

  latest=$(IMAGE_PATH=$image REGEX_PATTERN=$regex ARTIFACTORY_URL=$artifactoryurl sh ~/utils/get_latest_image.sh)
  
  echo "Going to pull the following image version: $latest"

  # Pull desired image from docker repo depending on selected BUILD_TYPE and passed in as artifactoryPath
  docker pull ${artifactoryPath}.artifactory.cwp.pnp-hcl.com/$image:$latest

done < $CONFIG_FILE

echo "done downloading container images"