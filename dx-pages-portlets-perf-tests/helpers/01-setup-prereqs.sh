#!/bin/sh
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

# Install Docker CE
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Start Docker CE
sudo systemctl enable --now docker

# Add centos to docker group
sudo usermod -aG docker centos

sudo yum -y install unzip
sudo yum -y install zip

# install opendjk
sudo yum -y install java-11-openjdk

# install bc for math in centos
sudo yum -y install bc

# Install jq
echo "Installing jq"
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

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