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

DOWNLOAD_FILE=$1
DOWNLOAD_PATH=$2
GENERIC_URL=$3

GENERIC_URL_DEFAULT="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic"

if [ "$DOWNLOAD_FILE" == "" ]; then
   echo "No file passed to download from Artifactory."
   echo "Syntax: downloadArtifactory repo_filename [local_path]"
   exit 1
fi

if [ "$DOWNLOAD_PATH" != "" ]; then
   mkdir -p $DOWNLOAD_PATH
   if [ "${UPLOAD_PATH:(-1)}" != "/" ]; then
      DOWNLOAD_PATH="$DOWNLOAD_PATH/"
   fi
fi

if [ "$GENERIC_URL" == "" ]; then
   GENERIC_URL="$GENERIC_URL_DEFAULT"
fi

LOCAL_FILE=$(basename $DOWNLOAD_FILE)

echo "Download from Artifactory"
echo "  local file:  $DOWNLOAD_PATH$LOCAL_FILE"
echo "  Artifactory: quintana-generic/$DOWNLOAD_FILE"

curl -s "$GENERIC_URL/$DOWNLOAD_FILE" -o $DOWNLOAD_PATH$LOCAL_FILE

