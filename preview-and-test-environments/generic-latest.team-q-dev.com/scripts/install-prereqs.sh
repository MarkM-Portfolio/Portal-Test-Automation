#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# This script installs the necessary pre-reqs for the ML-Latest environment
# NodeJS
# NVM
# Docker CE

# Install NodeJS
sudo yum install -y gcc-c++ make unzip
curl -sL https://rpm.nodesource.com/setup_12.x | sudo -E bash -
sudo yum install -y nodejs

# Echo version for verification
node -v
npm -v

# Install NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.35.1/install.sh | bash

## Install Docker CE
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Start Docker CE
sudo systemctl start docker

# Add centos to docker group
sudo usermod -aG docker centos

# Verify docker setup
sudo docker run hello-world

# Install PM2 for management of the native NodeJS Processes
sudo npm install pm2 -g

# Install docker compose
sudo curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
