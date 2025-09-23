#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
#
# The script determines the list of artifactory paths , reads and triggers pull of listed artifacts
#

IMAGE_FILTER=$1
ARTIFACT_PATH_CONFIG_FILE=$2

while read artifactoryPath; do
  case $artifactoryPath in
    quintana-docker*)
        sh ~/whitesource/scripts/04-01-pull-necessary-images.sh $IMAGE_FILTER ~/whitesource/config/image-pull.config $artifactoryPath
      ;;
    quintana-generic*)
        sh ~/whitesource/scripts/04-02-pull-necessary-artifacts.sh $IMAGE_FILTER ~/whitesource/config/artifact-list.config $artifactoryPath
      ;;
  esac
done < $ARTIFACT_PATH_CONFIG_FILE

