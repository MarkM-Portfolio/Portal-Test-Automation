#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
## Get parameters
HOSTNAME=$1
AWS_ACCESS_KEY_ID=$2
AWS_SECRET_ACCESS_KEY=$3


## Get Let's Encrypt CA certificate
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
SERVER="letsencrypt"

$HOME/.acme.sh/acme.sh --issue --dns dns_aws --server $SERVER -d $HOSTNAME
CERTDIR="$HOME/.acme.sh/$HOSTNAME"
CERTFILE=$CERTDIR/fullchain.cer
KEYFILE=$CERTDIR/$HOSTNAME".key"
RENEWALDIR="$HOME/.acme.sh/renewal"
mkdir -p $RENEWALDIR

echo "Certificate installed successfully."

## ensure we can fetch it
yum -y install ca-certificates

cd $CERTDIR
#wget --no-check-certificate https://letsencrypt.org/certs/trustid-x3-root.pem.txt
curl -L http://x1.i.lencr.org/ -o trustid-x3-root.pem.der

echo "Succesfully fetched Lets Encrypt root certificate."
ls -ltrha
