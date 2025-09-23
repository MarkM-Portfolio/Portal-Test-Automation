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
# The script determines the latest artifacts to use, and unzips them
#

regex=$1
CONFIG_FILE=$2
artifactoryPath=$3
artifactoryurl="https://artifactory.cwp.pnp-hcl.com/artifactory/$artifactoryPath"

mkdir  ~/whitesource/artifacts

while read artifact; do

  echo "----------------------------------------------------"
  echo "Pull the artifact: $artifact from $artifactoryurl"
  echo "----------------------------------------------------"
  echo ""

  latest=$(IMAGE_PATH=$artifact REGEX_PATTERN=$regex ARTIFACTORY_URL=$artifactoryurl sh ~/utils/get_latest_image.sh)

  echo "Going to download the following artifact $artifact in version: $latest"

  # download and unzip desired artifacts
  curl -o ~/whitesource/$artifact.zip $artifactoryurl/$artifact/$latest
  unzip -q ~/whitesource/$artifact.zip -d  ~/whitesource/artifacts/$artifact

  # update project name in respective config
  version=$(basename $latest .zip)
  echo "Latest version : $version"
  if [[ $version == *"wef"* ]];
  then
   sed -i "s/INSERT_PROJECT_NAME_HERE/$version/g"  ~/whitesource/config/wss-wef-zip.config
  elif [[ $version == *"dxclient"* ]]; 
  then
   sed -i "s/INSERT_PROJECT_NAME_HERE/$version/g"  ~/whitesource/config/wss-dxclient-zip.config
  elif [[ $version == *"extensions"* ]];
  then
    sed -i "s/INSERT_PROJECT_NAME_HERE/$version/g"  ~/whitesource/config/wss-dx-extensions.config
  fi

done < $CONFIG_FILE

echo "done downloading artifacts"