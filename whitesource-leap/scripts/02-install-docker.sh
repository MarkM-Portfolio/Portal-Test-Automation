#!/bin/sh
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

# Install Docker CE
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce-24.0.7-1.el7 docker-ce-cli-24.0.7-1.el7 containerd.io

# Start Docker CE
sudo systemctl enable --now docker

# Add centos to docker group
sudo usermod -aG docker centos

echo "Done with Leap Whitesource 02-install-docker"