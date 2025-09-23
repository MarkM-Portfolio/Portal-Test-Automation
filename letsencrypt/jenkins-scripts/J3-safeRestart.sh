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
JUSER=$2
JPASSWD=$3

if [ "$JPASSWD" == "" ]; then
   echo "ERROR: Parameters missing!"
   echo "Syntax: $SCRIPT_NAME SYSTEM_NAME JENKINS_ADMIN JENKINS_PASSWORD"
   echo "        SYSTEM_NAME - Name of system (instance name) to put new certificate on"
   echo "        JENKINS_ADMIN - Jenkins admin user"
   echo "        JENKINS_PASSWORD - Password of admin user"
   exit 1
fi

JENKINS_SYSCONFIG="/etc/sysconfig/jenkins"

## Get JENKINS_HOME
JENKINS_HOME=$(cat $JENKINS_SYSCONFIG | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME%*\"}
JENKINS_HOME=${JENKINS_HOME##*\"}

cd $JENKINS_HOME

echo "Safe restart Jenkins"
echo "curl https://$JENKINS_MASTER:8443/jnlpJars/jenkins-cli.jar -o jenkins-cli.jar --silent --insecure"
curl https://$JENKINS_MASTER:8443/jnlpJars/jenkins-cli.jar -o jenkins-cli.jar --silent --insecure
java -jar jenkins-cli.jar -noCertificateCheck -auth $JUSER:$JPASSWD -s https://$JENKINS_MASTER:8443/ safe-restart
rm -f jenkins-cli.jar 

max_loop=6
while [ "$max_loop" != "0" ]; do
   echo "   Continuing in 10.0 Seconds...."
   sleep 10s
   jenkins_up=$(curl -s https://$JENKINS_MASTER:8443 --insecure)
   if [[ $jenkins_up == *"Welcome to Jenkins!"* ]]; then
      break;
   fi
   ((max_loop--))
done

## Fail pipeline if Jenkins didn't start
if [ "$max_loop" == "0" ]; then
   echo "ERROR: Jenkins did not start as expected."
   exit 1
fi

echo "Jenkins is up and running."
