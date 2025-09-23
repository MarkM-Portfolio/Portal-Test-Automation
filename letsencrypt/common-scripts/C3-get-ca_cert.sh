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
 
## Get parameters
SCRIPT_NAME=$(basename "${BASH_SOURCE[0]}")
JENKINS_MASTER=$1
ACCESS_KEY_ID=$2
SECRET_ACCESS_KEY=$3

if [ "$AWS_SECRET_ACCESS_KEY" == "" ]; then
   echo "ERROR: Parameters missing!"
   echo "Syntax: $SCRIPT_NAME SYSTEM_NAME AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY"
   echo "        SYSTEM_NAME - Name of system (instance name) to put new certificate on"
   echo "        AWS_ACCESS_KEY_ID - AWS access key ID"
   echo "        AWS_SECRET_ACCESS_KEY - AWS secret access key"
   exit 1
fi

## Get Let's Encrypt CA certificate
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
SERVER="letsencrypt"
CERT_OK="no"

if [ -e $HOME/.acme.sh/acme.sh ]; then
   $HOME/.acme.sh/acme.sh --issue --dns dns_aws --server $SERVER -d $JENKINS_MASTER
   CERTDIR="$HOME/.acme.sh/$JENKINS_MASTER"
   CERTFILE=$CERTDIR/fullchain.cer
   KEYFILE=$CERTDIR/$JENKINS_MASTER".key"
   RENEWALDIR="$HOME/.acme.sh/renewal"
   mkdir -p $RENEWALDIR
   CERT_OK="yes"
else
   res=$(certbot version 2>&1)
   if [[ "$res" != *"command not found"* ]]; then
      certbot certonly --dns-route53 -d $JENKINS_MASTER --register-unsafely-without-email --agree-tos --no-eff-email
      CERTDIR="/etc/letsencrypt/live/$JENKINS_MASTER"
      CERTFILE=$CERTDIR/fullchain.pem
      KEYFILE=$CERTDIR/privkey.pem
      RENEWALDIR="/etc/letsencrypt/renewal"
      CERT_OK="yes"
   fi
fi
if [ "$CERT_OK" == "no" ]; then
   echo "Certificate not installed due to missing installer."
   echo "Install either acmesh or certbot."
   exit 1
fi
