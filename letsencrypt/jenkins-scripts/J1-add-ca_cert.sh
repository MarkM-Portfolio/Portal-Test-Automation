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
SCRIPT_PATH="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
SCRIPT_NAME=$(basename "${BASH_SOURCE[0]}")
JENKINS_MASTER=$1
AWS_ACCESS_KEY_ID=$2
AWS_SECRET_ACCESS_KEY=$3
JKSpasswd=$4
JUSER=$5
JPASSWD=$6

if [ "$JPASSWD" == "" ]; then
   echo "ERROR: Parameters missing!"
   echo "Syntax: $SCRIPT_NAME SYSTEM_NAME AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY JKS_PASSWORD JENKINS_ADMIN JENKINS_PASSWORD"
   echo "        SYSTEM_NAME - Name of system (instance name) to put new certificate on"
   echo "        AWS_ACCESS_KEY_ID - AWS access key ID"
   echo "        AWS_SECRET_ACCESS_KEY - AWS secret access key"
   echo "        JKS_PASSWORD - Passwort to use for Jenkins keystore protection"
   echo "        JENKINS_ADMIN - Jenkins admin user"
   echo "        JENKINS_PASSWORD - Password of admin user"
   exit 1
fi

JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"

## Get JENKINS_HOME
JENKINS_HOME=$(cat $JENKINS_SYSCONFIG | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME%*\"}
JENKINS_HOME=${JENKINS_HOME##*\"}

## Get Let's Encrypt CA certificate
. $SCRIPT_PATH/../common-scripts/C3-get-ca_cert.sh "$JENKINS_MASTER" "$AWS_ACCESS_KEY_ID" "$AWS_SECRET_ACCESS_KEY"

## Navigate to location of JENKINS_HOME and generate required keystore
echo "Create new JKS"
cd $JENKINS_HOME
rm -f jenkins.p12
rm -f jenkins.jks
openssl pkcs12 -export -out jenkins.p12 -passout "pass:$JKSpasswd" -inkey $KEYFILE -in $CERTFILE -name $JENKINS_MASTER
keytool -importkeystore -srckeystore jenkins.p12 -srcstorepass "$JKSpasswd" -srcstoretype PKCS12 -srcalias $JENKINS_MASTER -deststoretype JKS -destkeystore jenkins.jks -deststorepass "$JKSpasswd" -destalias $JENKINS_MASTER
chown jenkins:jenkins jenkins.jks
chmod 644 jenkins.jks

## Copy renewal script and config
cp $SCRIPT_PATH/J2-renew-ca_cert.sh $RENEWALDIR/J2-renew-ca_cert.sh
chmod 744 $RENEWALDIR/J2-renew-ca_cert.sh
echo "JENKINS_MASTER=$JENKINS_MASTER" > $RENEWALDIR/J2-renew-ca_cert.sh.config
echo "JUSER=$JUSER" >> $RENEWALDIR/J2-renew-ca_cert.sh.config
echo "JPASSWD=$JPASSWD" >> $RENEWALDIR/J2-renew-ca_cert.sh.config
chmod 600 $RENEWALDIR/J2-renew-ca_cert.sh.config

echo "Restart $JENKINS_MASTER"
$SCRIPT_PATH/J3-safeRestart.sh $JENKINS_MASTER $JUSER $JPASSWD

## Install cronjob
$SCRIPT_PATH/../common-scripts/C4-add-cronjob.sh "$RENEWALDIR/J2-renew-ca_cert.sh"