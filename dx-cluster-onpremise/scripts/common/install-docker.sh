#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
#Remove docker if already installed
DOCKER_INSTALLED=$(docker info | grep Containers)
if [[ $? -eq 0 && $DOCKER_INSTALLED != "" ]];then
    echo "Docker is already installed. Remove docker in order to upgrade to latest version."
    yum remove -y docker*
else
   echo "Docker is not installed previously."
fi

# Install Docker CE
yum install -y yum-utils device-mapper-persistent-data lvm2
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io

# Set experimental features on
# Here, storage derive was required in order to run docker on the EC2 machine.
mkdir -p /etc/docker
echo '{
    "experimental": true,
    "storage-driver": "overlay2"
}' > /etc/docker/daemon.json

# Start Docker CE
systemctl enable docker
systemctl start docker

# Add user to docker group
usermod -aG docker root
