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
sudo yum -y -q install java-11-openjdk
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Install jq
echo "Installing jq"
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

# Start Docker CE
sudo systemctl enable --now docker

# Add centos to docker group
sudo usermod -aG docker centos

sudo yum -y install unzip
sudo yum -y install zip