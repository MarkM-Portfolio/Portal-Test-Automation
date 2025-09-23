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

TARGET_SSL=$1
CERTHOME=$2
CERTS=$3
ACCESS_KEY=$4

# Check if mandatory parameter TARGET_SSL is set
if [ "$TARGET_SSL" == "" ]; then
   echo "ERROR: Missing target system"
   exit 1
fi


if [ "$CERTHOME" == "" ]; then
   CERTHOME=$(pwd)
fi

if [ "$CERTS" == "" ] || [ "$CERTS" == "all" ]; then
   ldir=$(pwd)
   cd $CERTHOME
   CERT_FILES=(*)
   cd $ldir
else
   IFS=',' eval 'CERT_FILES=($CERTS)'
fi

if [ "$ACCESS_KEY" == "" ]; then
   ACCESS_KEY="test-automation.pem"
fi

echo "Certs will be copied to centos@$TARGET_SSL :"
for cert in "${CERT_FILES[@]}"; do
   echo " - ${CERTHOME}/$cert"
   scp -o LogLevel=Error -i $ACCESS_KEY -o StrictHostKeyChecking=no ${CERTHOME}/$cert centos@$TARGET_SSL:$cert
done
