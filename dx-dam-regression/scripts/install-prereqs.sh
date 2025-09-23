#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2021, 2022. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# This script installs the necessary pre-reqs for the dam performance test instance

# install opendjk
sudo yum -y install java-11-openjdk

# Install jq
echo "Installing jq"
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

# install bc for math in centos
sudo yum -y install bc

# jmeter binary version 
JMETER_BINARY_VERSION=$1

echo "JMETER_BINARY_FILE_NAME: $JMETER_BINARY_VERSION.tgz"
cd /home/centos/
curl -LJO "https://dlcdn.apache.org//jmeter/binaries/${JMETER_BINARY_VERSION}.tgz"

# untar the jmeter 
tar -zxvf "$JMETER_BINARY_VERSION.tgz"

# print the version in console
cd $JMETER_BINARY_VERSION/bin
./jmeter.sh -v

cd ..
cd lib
# download cmdrunner-2.0.jar 
sudo curl -O https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.3/cmdrunner-2.3.jar
cd ext
# download jmeter-plugins-manager jar
sudo curl -O https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.6/jmeter-plugins-manager-1.6.jar
# install PluginManager CMDInstaller
java -cp jmeter-plugins-manager-1.6.jar org.jmeterplugins.repository.PluginManagerCMDInstaller
cd ..
# install directory-listing plugin
java  -jar cmdrunner-2.3.jar --tool org.jmeterplugins.repository.PluginManagerCMD install jpgc-directory-listing