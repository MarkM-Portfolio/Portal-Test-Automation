#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Setup Jenkins Security

## Get parameters
GROOVY_SCRIPT=in-script-approval.groovy
DATA_FILE=in-script.txt

## Get script directory
SCRIPT_DIR=$(dirname "$0")
echo "SCRIPT_DIR = $SCRIPT_DIR"

## Get JENKINS_HOME
JENKINS_HOME=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME//Environment=/}
JENKINS_HOME=${JENKINS_HOME//\"/}
JENKINS_HOME=${JENKINS_HOME//JENKINS_HOME=/}
echo "JENKINS_HOME = $JENKINS_HOME"

## Move migrate creds to /tmp
mkdir -p /tmp/install_jenkins
mv $SCRIPT_DIR/helpers/$DATA_FILE /tmp/install_jenkins/migrate-$DATA_FILE
chown -R jenkins:jenkins /tmp/install_jenkins

## Stop Jenkins to install plugins offline
echo "Stop Jenkins for plugins install"
systemctl stop jenkins
echo "   Continuing in 5.0 Seconds...."
sleep 5s

## Add new boot script
mkdir $JENKINS_HOME/init.groovy.d
cp $SCRIPT_DIR/helpers/$GROOVY_SCRIPT $JENKINS_HOME/init.groovy.d/$GROOVY_SCRIPT
chown -R jenkins:jenkins $JENKINS_HOME/init.groovy.d

## Start and connect to Jenkins, retry max 6 times
echo "Restart Jenkins"
systemctl start jenkins
max_loop=6
while [ "$max_loop" != "0" ]; do
   echo "   Continuing in 10.0 Seconds...."
   sleep 10s
   jenkins_up=$(curl -s http://127.0.0.1:8080)
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

## Remove boot script directory
INSTALL_BACKUP=jenkins_install_backup
rm -fR $JENKINS_HOME/init.groovy.d