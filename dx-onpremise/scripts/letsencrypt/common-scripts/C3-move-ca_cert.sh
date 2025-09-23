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
HOSTNAME=$1
SOURCEDIR=$2
user=$(whoami)

if [ "$SOURCEDIR" == "" ]; then
   SOURCEDIR="/home/centos"
fi
CERTDIR="$HOME/.acme.sh/$HOSTNAME"
RENEWALDIR="$HOME/.acme.sh/renewal"
mkdir -p $RENEWALDIR
mkdir -p $CERTDIR

ls -l /home/centos
mv $SOURCEDIR/${HOSTNAME}.* $CERTDIR
mv $SOURCEDIR/ca.cer $CERTDIR
mv $SOURCEDIR/fullchain.cer $CERTDIR
mv $SOURCEDIR/trustid-x3-root.pem.der $CERTDIR

chown $user:$user $CERTDIR/${HOSTNAME}* 
chown $user:$user $CERTDIR/ca.cer
chown $user:$user $CERTDIR/fullchain.cer
chown $user:$user $CERTDIR/trustid-x3-root.pem.der
chmod 644 $CERTDIR/${HOSTNAME}* 
chmod 644 $CERTDIR/ca.cer
chmod 644 $CERTDIR/fullchain.cer
chmod 644 $CERTDIR/trustid-x3-root.pem.der

## ensure we can fetch it
yum -y install ca-certificates

echo "Succesfully moved Lets Encrypt certificate."
ls -l $CERTDIR
