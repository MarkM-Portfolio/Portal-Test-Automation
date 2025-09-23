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
CONFIG_FILE=$SCRIPT_PATH/$SCRIPT_NAME".config"
LOG_FILE=$SCRIPT_PATH/$SCRIPT_NAME".log"
if [ -e "$CONFIG_FILE" ]; then
   while IFS= read -r line
   do
      if [[ "$line" == "JENKINS_MASTER="* ]]; then
         JENKINS_MASTER=${line:15}
      elif [[ "$line" == "JUSER="* ]]; then
         JUSER=${line:6}
      elif [[ "$line" == "JPASSWD="* ]]; then
         JPASSWD=${line:8}
      fi
   done < "$CONFIG_FILE"
else
   JENKINS_MASTER=$1
   JUSER=$2
   JPASSWD=$3
   FORCE=$4
fi

JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"

## Get JENKINS_HOME
JENKINS_HOME=$(cat $JENKINS_SYSCONFIG | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME%*\"}
JENKINS_HOME=${JENKINS_HOME##*\"}

## Get KEYSTORE_PASSWORD
JKSpasswd=$(cat $JENKINS_SYSCONFIG | grep "JENKINS_HTTPS_KEYSTORE_PASSWORD=")
JKSpasswd=${JKSpasswd%*\"}
JKSpasswd=${JKSpasswd##*\"}

## Get jenkins-cli.jar
cd $JENKINS_HOME
curl https://$JENKINS_MASTER:8443/jnlpJars/jenkins-cli.jar -o jenkins-cli.jar --silent --insecure

## Renew Let's Encrypt CA certificate if necessary
echo "Try certificate renewal" > $LOG_FILE
if [ -e $HOME/.acme.sh/acme.sh ]; then
   CERTDIR="$HOME/.acme.sh/$JENKINS_MASTER"
   CERTFILE=$CERTDIR/fullchain.cer
   KEYFILE=$CERTDIR/$JENKINS_MASTER".key"
   NOT_DUE_CHECK="Skip, Next renewal time is"
   NOT_DUE=$($HOME/.acme.sh/acme.sh --renew -d $JENKINS_MASTER $FORCE 2>&1 |grep "$NOT_DUE_CHECK")
else
   CERTDIR="/etc/letsencrypt/live/$JENKINS_MASTER"
   CERTFILE=$CERTDIR/fullchain.pem
   KEYFILE=$CERTDIR/privkey.pem
   NOT_DUE_CHECK="Cert not yet due for renewal"
   NOT_DUE=$(certbot renew --cert-name $JENKINS_MASTER $FORCE 2>&1 |grep "$NOT_DUE_CHECK")
fi

echo "CERTFILE = $CERTFILE" >> $LOG_FILE
echo "KEYFILE = $KEYFILE" >> $LOG_FILE

if [[ "$NOT_DUE" == *"$NOT_DUE_CHECK"* ]]; then
   echo $NOT_DUE >> $LOG_FILE
else
   echo "Got new CA certificate for $JENKINS_MASTER" >> $LOG_FILE
   echo $NOT_DUE >> $LOG_FILE
   echo "Create new Jenkins keystore" >> $LOG_FILE
   cd $JENKINS_HOME
   rm -f jenkins.p12
   rm -f jenkins.jks
   openssl pkcs12 -export -out jenkins.p12 -passout "pass:$JKSpasswd" -inkey $KEYFILE -in $CERTFILE -name $JENKINS_MASTER >> $LOG_FILE
   keytool -importkeystore -srckeystore jenkins.p12 -srcstorepass "$JKSpasswd" -srcstoretype PKCS12 -srcalias $JENKINS_MASTER -deststoretype JKS -destkeystore jenkins.jks -deststorepass "$JKSpasswd" -destalias $JENKINS_MASTER >> $LOG_FILE
   chown jenkins:jenkins jenkins.jks
   chmod 644 jenkins.jks
   echo "Safe restart Jenkins" >> $LOG_FILE
   java -jar jenkins-cli.jar -noCertificateCheck -auth $JUSER:$JPASSWD -s https://$JENKINS_MASTER:8443/ safe-restart >> $LOG_FILE
fi

## Remove jenkins-cli.jar
rm -f jenkins-cli.jar