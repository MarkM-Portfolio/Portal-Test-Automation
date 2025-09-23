#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2020, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Script to install Jenkins Plugins, will accept a comma separated list as parameter $1
JENKINS_PLUGINS=$1

# Subroutine to install single Jenkins plugin
# Plugins can either be defined only as name, installing the latest, or have a ":" delimiter for a version number, e.g. credentials:6.2.1
installPlugin() {
  
  local loc_plugin=$1
  local loc_version="latest"
  local loc_dwnl_url="https://updates.jenkins-ci.org"
  if [[ "$1" == *":"* ]]; then
    loc_version=${loc_plugin##*:}
    loc_plugin=${loc_plugin%:*}
    loc_dwnl_url="https://updates.jenkins-ci.org/download/plugins/${loc_plugin}"
  fi
  if [ -f ${plugin_dir}/${loc_plugin}.hpi -o -f ${plugin_dir}/${loc_plugin}.jpi ]; then
    if [ "$2" == "1" ]; then
      return 1
    fi
    echo "Skipped: $1 (already installed)"
    return 0
  else
    echo "Installing: $loc_plugin:$loc_version"
    curl -L --silent --output ${plugin_dir}/${loc_plugin}.hpi  $loc_dwnl_url/${loc_version}/${loc_plugin}.hpi
    return 0
  fi
}

## Get script directory
SCRIPT_DIR=$(dirname "$0")
echo "SCRIPT_DIR = $SCRIPT_DIR"

## Get JENKINS_HOME
JENKINS_HOME=$(cat /lib/systemd/system/jenkins.service | grep "JENKINS_HOME=")
JENKINS_HOME=${JENKINS_HOME//Environment=/}
JENKINS_HOME=${JENKINS_HOME//\"/}
JENKINS_HOME=${JENKINS_HOME//JENKINS_HOME=/}
echo "JENKINS_HOME = $JENKINS_HOME"

## Set variables for script
plugin_dir=$JENKINS_HOME/plugins
file_owner=jenkins.jenkins
mkdir -p $plugin_dir

## Stop Jenkins to install plugins offline
echo "Stop Jenkins for plugins install"
systemctl stop jenkins
echo "   Continuing in 5.0 Seconds...."
sleep 5s

## Install Plugins
for plugin in in ${JENKINS_PLUGINS//,/ }; do
  installPlugin "$plugin"
done

## Install Plugins dependencies
changed=1
max_loop=100
incl_optional_dependencies=0
while [ "$changed" == "1" ]; do
  echo "Check for missing dependecies ..."
  if  [ $max_loop -lt 1 ] ; then
    echo "ERROR: Max loop count reached - probably a bug in this script: $0"
    exit 1
  fi
  ((max_loop--))
  changed=0
  for f in ${plugin_dir}/*.hpi ; do
    if [ "$incl_optional_dependencies" = "0" ]; then
      # install without optionals
      deps=$( unzip -p ${f} META-INF/MANIFEST.MF | tr -d '\r' | sed -e ':a;N;$!ba;s/\n //g' | grep -e "^Plugin-Dependencies: " | awk '{ print $2 }' | tr ',' '\n' | grep -v "resolution:=optional" | awk -F ':' '{ print $1 }' | tr '\n' ' ' )
    else
      # install with optionals
      deps=$( unzip -p ${f} META-INF/MANIFEST.MF | tr -d '\r' | sed -e ':a;N;$!ba;s/\n //g' | grep -e "^Plugin-Dependencies: " | awk '{ print $2 }' | tr ',' '\n' | awk -F ':' '{ print $1 }' | tr '\n' ' ' )
    fi
    for plugin in $deps; do
      installPlugin "$plugin" 1 && changed=1
    done
  done
done

echo "Fixing permissions"
chown ${file_owner} ${plugin_dir} -R
echo "Plugins install done"

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
