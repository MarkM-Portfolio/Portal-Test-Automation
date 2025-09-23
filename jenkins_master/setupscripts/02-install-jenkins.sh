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
 
# Install Jenkins

## Get parameters
JENKINS_VERSION=$1
ADMIN_USER=$2
JAVA_ARGUMENTS=$3
echo "JENKINS_VERSION = $JENKINS_VERSION"
JENKINS_WAR_DIR=/usr/lib/jenkins
GROOVY_SCRIPT=basic-security.groovy
JENKINS_BUILD_ALERT_SCRIPT=jenkins-build-alert.py
ROOT_CRONJOB=runRootJob.sh

## Get script directory
SCRIPT_DIR=$(dirname "$0")
echo "SCRIPT_DIR = $SCRIPT_DIR"

## Enable Jenkins repository
curl --silent --location http://pkg.jenkins-ci.org/redhat-stable/jenkins.repo -o /etc/yum.repos.d/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat/jenkins.io.key

## Install Jenkins
yum install -y jenkins

## Get JENKINS_HOME
JENKINS_HOME=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME//Environment=/}
JENKINS_HOME=${JENKINS_HOME//\"/}
JENKINS_HOME=${JENKINS_HOME//JENKINS_HOME=/}
echo "JENKINS_HOME = $JENKINS_HOME"

## Disable setup wizard
#JENKINS_ARGS=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_ARGS=")
#JENKINS_ARGS=${JENKINS_ARGS//Environment=/}
#JENKINS_ARGS=${JENKINS_ARGS//\"/}
#JENKINS_ARGS=${JENKINS_ARGS//JENKINS_ARGS=/}

mkdir -p /etc/systemd/system/jenkins.service.d/
touch /etc/systemd/system/jenkins.service.d/override.conf

## Configure Java settings
cat <<EOF >>/etc/systemd/system/jenkins.service.d/override.conf
[Unit]
Description=DX Jenkins

[Service]
Environment="JAVA_OPTS=$3"
EOF
systemctl daemon-reload


## Create temp directory for installation
mkdir -p /tmp/install_jenkins
chown -R jenkins:jenkins /tmp/install_jenkins

## Add initial boot script
mkdir $JENKINS_HOME/init.groovy.d
chown -R jenkins:jenkins $JENKINS_HOME/init.groovy.d
cp $SCRIPT_DIR/helpers/$GROOVY_SCRIPT $JENKINS_HOME/init.groovy.d/$GROOVY_SCRIPT
sed -i "s/a-d-m-i-n/$ADMIN_USER/g" $JENKINS_HOME/init.groovy.d/$GROOVY_SCRIPT
chown jenkins:jenkins $JENKINS_HOME/init.groovy.d/$GROOVY_SCRIPT

## Add jenkins build alert boot script
cp $SCRIPT_DIR/helpers/$JENKINS_BUILD_ALERT_SCRIPT $JENKINS_HOME/$JENKINS_BUILD_ALERT_SCRIPT
chown jenkins:jenkins $JENKINS_HOME/$JENKINS_BUILD_ALERT_SCRIPT
chmod 700 $JENKINS_HOME/$JENKINS_BUILD_ALERT_SCRIPT
if crontab -l | grep "$JENKINS_BUILD_ALERT_SCRIPT"; then
  echo "Cronjob already installed"
else
  echo "Installing cron job"
  (crontab -l 2>/dev/null; echo "# Check Jenkins build alert every hour") | crontab -
  (crontab -l 2>/dev/null; echo "0 * * * * /usr/src/Python-3.8.1/python \"$JENKINS_HOME/$JENKINS_BUILD_ALERT_SCRIPT\"") | crontab -
fi

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
   exit 1
fi

echo "Jenkins is up and running."

## Reset saved JENKINS_ARGS
# obsolete, not used anymore
sed -i "/JENKINS_ARGS=/c\JENKINS_ARGS=\"$JENKINS_ARGS\"" /etc/sysconfig/jenkins

## Save install config
INSTALL_BACKUP=jenkins_install_backup
mkdir -p $JENKINS_HOME/$INSTALL_BACKUP
cp $JENKINS_HOME/config.xml $JENKINS_HOME/$INSTALL_BACKUP/config.xml

## Deactivate initial boot script directory
rm -fR $JENKINS_HOME/init.groovy.d

## Update Jenkins if required
JENKINS_WAR_URL="https://get.jenkins.io"
JENKINS_WAR_URL_ALTERNATE="http://mirror.xmission.com/jenkins"
if [ "$JENKINS_VERSION" != "centos" ]; then
   installed_version=$(java -jar /usr/lib/jenkins/jenkins.war --version)
   echo "Jenkins update requested."
   echo "   Installed version: $installed_version"
   echo "   Requested version: $JENKINS_VERSION"
   if [[ "$installed_version" != "$JENKINS_VERSION"* ]]; then
      echo "   Copy current installed warfile as backup to $JENKINS_HOME/$INSTALL_BACKUP"
      echo "   Stop Jenkins"
      systemctl stop jenkins
      echo "      Continuing in 5.0 Seconds...."
      sleep 5s
      cp $JENKINS_WAR_DIR/jenkins.war $JENKINS_HOME/$INSTALL_BACKUP/jenkins.war
      chown -R jenkins:jenkins $JENKINS_HOME/$INSTALL_BACKUP
      curl --silent --location $JENKINS_WAR_URL/war/$JENKINS_VERSION/jenkins.war -o $JENKINS_WAR_DIR/jenkins.war
      installed_version=$(java -jar /usr/lib/jenkins/jenkins.war --version)
      if [ "$JENKINS_VERSION" != "$installed_version" ]; then
         echo "WARNING: Download of new Jenkins warfile failed from $JENKINS_WAR_URL."
         echo "         Trying alternate download from $JENKINS_WAR_URL_ALTERNATE."
         curl --silent --location $JENKINS_WAR_URL_ALTERNATE/war/$JENKINS_VERSION/jenkins.war -o $JENKINS_WAR_DIR/jenkins.war
         installed_version=$(java -jar /usr/lib/jenkins/jenkins.war --version)
         if [ "$JENKINS_VERSION" != "$installed_version" ]; then
            echo "ERROR: Download of new Jenkins warfile failed."
            exit 1
         fi
      fi
      echo "   Restart Jenkins"
      systemctl start jenkins
      max_loop=6
      while [ "$max_loop" != "0" ]; do
         echo "      Continuing in 10.0 Seconds...."
         sleep 10s
         jenkins_up=$(curl -s http://127.0.0.1:8080)
         if [[ $jenkins_up == *"Welcome to Jenkins!"* ]]; then
            break;
         fi
         (( max_loop -= 1 ))
      done
      if [ "$max_loop" == "0" ]; then
         echo "ERROR: Jenkins did not start as expected."
         exit 1
      fi
      if [[ "$jenkins_up" != *">Jenkins $JENKINS_VERSION</"* ]]; then
         echo "ERROR: Jenkins did not update as expected."
         exit 1
      fi 
      echo "Jenkins is up and running."
   else
      echo "Installed Jenkins version meets request."
   fi
fi

## Add root cronjob for Jenkins full backup
cp $SCRIPT_DIR/helpers/$ROOT_CRONJOB /root/$ROOT_CRONJOB
chown root:root /root/$ROOT_CRONJOB
chmod 700 /root/$ROOT_CRONJOB
if crontab -l | grep "$$ROOT_CRONJOB"; then
  echo "Cronjob already installed"
else
  echo "Installing cron job"
  (crontab -l 2>/dev/null; echo "# Check Jenkins every 5 minutes for root job") | crontab -
  (crontab -l 2>/dev/null; echo "*/5 * * * * \"/root/$ROOT_CRONJOB\"") | crontab -
fi
