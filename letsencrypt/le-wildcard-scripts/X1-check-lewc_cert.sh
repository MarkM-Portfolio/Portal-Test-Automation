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

ARTIFACTORY_URL=$1
DOMAIN=$2
CERTHOME=$3
quiet=$4
CERT_DOWNLOAD="$CERTHOME/$DOMAIN"
CERT_FILES=("ca.cer" "fullchain.cer" "${DOMAIN}.cer" "${DOMAIN}.conf" "${DOMAIN}.key")

# Check if mandatory parameters ARTIFACTORY_URL, DOMAIN, and CERTHOME are available
if [ "$ARTIFACTORY_URL" == "" ]; then
   echo "ERROR: Artifactory URL missing!"
   exit 1
fi
if [ "$DOMAIN" == "" ]; then
   echo "ERROR: Domain missing!"
   exit 1
fi
if [ "$CERTHOME" == "" ]; then
   echo "ERROR: Download directory missing!"
   exit 1
fi
# Check if cert directory and files are available
if [ ! -e "${CERTHOME}" ]; then
   echo "ERROR: Local certificate files."
   echo "       Missing directory ${CERTHOME}"
   exit 1
fi
if [ ! -e "${CERT_DOWNLOAD}" ]; then
   echo "ERROR: Local certificate files."
   echo "       Missing directory ${CERT_DOWNLOAD}"
   exit 1
fi
if [ ! -e "${CERT_DOWNLOAD}/${DOMAIN}.conf" ]; then
   echo "ERROR: Local certificate files."
   echo "       Missing file ${CERT_DOWNLOAD}/${DOMAIN}.conf"
   exit 1
fi

# Get cert cration date from truststore
certCreate=$(curl -s ${ARTIFACTORY_URL}/${DOMAIN}/${DOMAIN}.conf |grep "Le_CertCreateTime=")

# Get local cert cration date from truststore
localCertCreate=$(cat ${CERT_DOWNLOAD}/${DOMAIN}.conf |grep "Le_CertCreateTime=")

if [ "$certCreate" == "$localCertCreate" ]; then
   if [ "$quiet" == "" ]; then
      echo "No new certificate files found in Artifactory"
   fi
else
   if [ "$quiet" == "" ]; then
      echo "Download new certificate files from Artifactory"
   fi
   for f in "${CERT_FILES[@]}"
   do
      curl -s $ARTIFACTORY_URL/$DOMAIN/$f -o $CERT_DOWNLOAD/$f
   done
fi
