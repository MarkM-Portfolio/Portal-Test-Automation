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

# Check if mandatory parameters DOMAIN, AWS_ACCESS_KEY_ID, and AWS_SECRET_ACCESS_KEY are available
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

# Set download directory
CERT_DOWNLOAD="$CERTHOME/$DOMAIN"

# Remove leading . in domain name
if [[ "$DOMAIN" == "."* ]]; then
   DOMAIN="${DOMAIN:1}"
fi

# Test if we have a cert for the given domain
ret=$(curl -s -o /dev/null -w "%{http_code}" $ARTIFACTORY_URL/$DOMAIN/${DOMAIN}.conf)
if [[ "$ret" != "200" ]]; then
   echo "Looks like we don't have a wildcard certificate for domain $DOMAIN in our truststore"
   echo "Truststore: $ARTIFACTORY_URL"
   echo "$ret"
   exit 1
fi

CERT_FILES=("ca.cer" "fullchain.cer" "${DOMAIN}.cer" "${DOMAIN}.conf" "${DOMAIN}.csr" "${DOMAIN}.csr.conf" "${DOMAIN}.key")

# Download certificate files from Artifactory
mkdir -p $CERT_DOWNLOAD
for f in "${CERT_FILES[@]}"
do
   curl -s $ARTIFACTORY_URL/$DOMAIN/$f -o $CERT_DOWNLOAD/$f
done

## ensure we can fetch it
yum -y install ca-certificates

#curl --insecure https://letsencrypt.org/certs/trustid-x3-root.pem.txt -o $CERT_DOWNLOAD/trustid-x3-root.pem.txt
curl -L http://x1.i.lencr.org/ -o $CERT_DOWNLOAD/trustid-x3-root.pem.der

echo "Certificate downloaded:"
echo "  The cert is in $CERT_DOWNLOAD/$DOMAIN.cer"
echo "  The cert key is in $CERT_DOWNLOAD/$DOMAIN.key"
echo "  The CA cert is in $CERT_DOWNLOAD/ca.cer"
echo "  The full chain certs is in $CERT_DOWNLOAD/fullchain.cer"
