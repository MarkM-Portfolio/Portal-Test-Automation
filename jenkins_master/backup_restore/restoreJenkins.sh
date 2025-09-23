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

SCRIPT_PATH=$(dirname $0)
LOCAL_FILE=$1
ROOT_JOB=$2

workdir=$(pwd)

cd /

# Save current Jenkins if installed
if [ -e /etc/sysconfig/jenkins ]; then
   echo "Save current Jenkins" 
   systemctl stop jenkins
   JENKINS_HOME_ORG=$(cat /etc/sysconfig/jenkins |grep "JENKINS_HOME=" |awk '{split($0,a,"\""); print a[2]}')
   mv /etc/sysconfig/jenkins /etc/sysconfig/jenkins.org
   mv /usr/lib/jenkins/jenkins.war /usr/lib/jenkins/jenkins.war.org
   mv $JENKINS_HOME_ORG ${JENKINS_HOME_ORG}.org
fi

# Restore backup 
echo "Restore backup" 
tar -xzf $workdir/$LOCAL_FILE
JENKINS_HOME=$(cat /etc/sysconfig/jenkins |grep "JENKINS_HOME=" |awk '{split($0,a,"\""); print a[2]}')
JENKINS_USER=$(cat /etc/sysconfig/jenkins |grep "JENKINS_USER=" |awk '{split($0,a,"\""); print a[2]}')
chown -R $JENKINS_USER:$JENKINS_USER $JENKINS_HOME

## Start and connect to Jenkins, retry max 5 times
max_loop=5
while [ "$max_loop" != "0" ]; do
   echo "Start Jenkins"
   systemctl start jenkins
   echo "   Continuing in 10.0 Seconds...."
   sleep 10s
   jenkins_up=$(curl -s http://127.0.0.1:8080)
   if [[ $jenkins_up == *"Welcome to Jenkins!"* ]]; then
      break;
   fi
   echo "   Jenkins not started."
   echo "Stop Jenkins"
   systemctl stop jenkins
   echo "   Continuing in 5.0 Seconds...."
   sleep 5s
   (( max_loop -= 1 ))
done

## Fail pipeline if Jenkins didn't start
if [ "$max_loop" == "0" ]; then
   echo "ERROR: Jenkins did not start as expected."
   echo "       Restoring original status."
   mv /etc/sysconfig/jenkins.org /etc/sysconfig/jenkins
   mv /usr/lib/jenkins/jenkins.war.org /usr/lib/jenkins/jenkins.war
   mv ${JENKINS_HOME_ORG}.org ${JENKINS_HOME_ORG}
   exit 1
fi

# Install root job check
if [ "$ROOT_JOB" != "" ]; then
   sh $workdir/$SCRIPT_PATH/addCronJob.sh $workdir/$ROOT_JOB
fi

# Cleanup
rm -f /etc/sysconfig/jenkins.org
rm -f /usr/lib/jenkins/jenkins.war.org
rm -fR ${JENKINS_HOME_ORG}.org

echo "Jenkins is up and running."
