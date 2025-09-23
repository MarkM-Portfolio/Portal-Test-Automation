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
 
# This script will upload the certificate files to the trustore in Artifactory

CERT_HOME=$1 
CERT_DOMAIN=$2 
ARTIFACTORY_URL=$3
ARTIFACTORY_USER=$4
ARTIFACTORY_PASSWORD=$5

CERT_FILES=("ca.cer" "fullchain.cer" "${CERT_DOMAIN}.cer" "${CERT_DOMAIN}.conf" "${CERT_DOMAIN}.csr" "${CERT_DOMAIN}.csr.conf" "${CERT_DOMAIN}.key")

# create build output for Artifactory
for f in "${CERT_FILES[@]}"
do
    if [ -e $CERT_HOME/$CERT_DOMAIN/$f ]; then
        curl -s -u$ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD -T $CERT_HOME/$CERT_DOMAIN/$f $ARTIFACTORY_URL/$CERT_DOMAIN/$f
    fi
done

