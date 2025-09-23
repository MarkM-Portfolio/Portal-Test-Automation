#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install jq
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version

#sudo yum install git -y
# sudo yum install unzip -y

# Install NodeJS
sudo yum install -y gcc-c++ make unzip
curl -sL https://rpm.nodesource.com/setup_12.x | sudo -E bash -
sudo yum install -y nodejs

# Echo version for verification
node -v
npm -v

# Install NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.35.1/install.sh | bash

# Install PM2 for management of the native NodeJS Processes
sudo npm install pm2 -g

# install test prereqs
# install chrome driver
sudo mkdir -p /opt/chrome
sudo chown centos: /opt/chrome
cd /opt/chrome
curl -O https://chromedriver.storage.googleapis.com/88.0.4324.96/chromedriver_linux64.zip
sudo unzip -o chromedriver_linux64.zip

# install chrome browser. We set a specific version of chrome here to avoid it auto updating and not being compatible with chromedriver.
sudo yum -y install wget
# wget -q https://dl.google.com/linux/chrome/rpm/stable/x86_64/google-chrome-stable-88.0.4324.96-1.x86_64.rpm
wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
#sudo yum -y -q localinstall google-chrome-stable-88.0.4324.96-1.x86_64.rpm
sudo yum -y -q localinstall google-chrome-stable_current_*.rpm

# install opendjk
sudo yum -y install java-11-openjdk

# make scope - register enchanted repositories in npm
npm config delete @enchanted:registry && npm config delete @enchanted-prod:registry
npm config set @enchanted:registry https://artifactory.cwp.pnp-hcl.com/artifactory/api/npm/quintana-npm/
npm config set @enchanted-prod:registry https://artifactory.cwp.pnp-hcl.com/artifactory/api/npm/quintana-npm-prod/

if [[ -n "$1" && -n "$2" ]]
    then
        sudo sh -c -e "echo '$1  $2' >> /etc/hosts"
fi


cd /opt/
sudo mkdir -p validate-search
sudo chown -R centos:centos validate-search/
