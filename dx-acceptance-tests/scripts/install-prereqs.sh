#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2024. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# This script installs the necessary pre-reqs for the acceptance test instance
# NodeJS
# NVM
# ChromeDriver
# ChromeBrowser
# OpenJDK
# Docker CE
# Docker Compose
# RobotJS
# Xvfb

echo "Install NodeJS"
sudo yum install -y -q gcc-c++ make unzip
curl -sL https://rpm.nodesource.com/setup_20.x | sudo -E bash -

# currently latest nodejs-12.22.7-1nodesource ends up in a download error
# so we catch the output check for it and use hard coded nodejs-12.22.6-1nodesource instead as workaround
# as soon as the error disappears we take whatever we get
ret=$(sudo yum install -q -y nodejs 2>&1)
echo "$ret"
if [[ "$ret" == *"Error downloading packages:"* ]]; then
    echo " "
    echo "=========================================================================="
    echo " Download error detected install nodejs-12.22.6-1nodesource as workaround"
    echo "=========================================================================="
    sudo yum install -q -y nodejs-12.22.6-1nodesource
fi

echo "Echo version for verification"
node -v
npm -v

echo "Install NVM"
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.3/install.sh | bash

echo "Install PM2 for management of the native NodeJS Processes"
sudo npm install pm2 -g

# Install test prereqs
echo "Install chrome driver"
sudo mkdir -p /opt/chrome
sudo chown centos: /opt/chrome
cd /opt/chrome
CHROME_DRIVER_VERSION=`curl -sS https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_STABLE`
curl -O https://storage.googleapis.com/chrome-for-testing-public/$CHROME_DRIVER_VERSION/linux64/chromedriver-linux64.zip
sudo unzip -o chromedriver-linux64.zip
sudo mv chromedriver-linux64/chromedriver .

echo "Install chrome browser"
sudo yum -y -q install wget
# wget -q https://dl.google.com/linux/chrome/rpm/stable/x86_64/google-chrome-stable-88.0.4324.96-1.x86_64.rpm
wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
#sudo yum -y -q localinstall google-chrome-stable-88.0.4324.96-1.x86_64.rpm
sudo yum -y -q localinstall google-chrome-stable_current_*.rpm

echo "Install opendjk"
sudo yum -y -q install java-11-openjdk

echo "Install Docker CE"
sudo yum install -y -q yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y -q docker-ce docker-ce-cli containerd.io

echo "Start Docker CE"
sudo systemctl start docker

echo "Add centos to docker group"
sudo usermod -aG docker centos

echo "Install docker compose"
sudo curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

echo "Make scope - register enchanted repositories in npm"
npm config delete @enchanted:registry && npm config delete @enchanted-prod:registry
npm config set @enchanted:registry https://artifactory.cwp.pnp-hcl.com/artifactory/api/npm/quintana-npm/
npm config set @enchanted-prod:registry https://artifactory.cwp.pnp-hcl.com/artifactory/api/npm/quintana-npm-prod/

echo "Install prereqs for RobotJS"
sudo yum -y install gcc openssl-devel bzip2-devel python27
sudo yum install -y -q libpng-devel libXtst-devel 

echo "Install Xvfb"
sudo yum install -y -q Xvfb libXfont2 Xorg tigervnc-server
sudo yum install -y -q https://dl.fedoraproject.org/pub/epel/epel-release-latest-$(rpm -E '%{rhel}').noarch.rpm
sudo yum -y -q install x11vnc

echo "Start Xvfb"
nohup Xvfb :99 > /dev/null 2>&1 &
export DISPLAY=:99

# Install jq
echo "Installing jq"
sudo yum install epel-release -y
sudo yum install jq -y
jq -Version
