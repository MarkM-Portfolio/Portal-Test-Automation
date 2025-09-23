#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install docker

## Install Docker CE
yum install -y yum-utils device-mapper-persistent-data lvm2
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io

# Install docker-compose
curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Set experimental features on
mkdir -p /etc/docker

# Added GC to prevent '/overlay2' filsystem getting full and out of space issue.
echo '{
  "experimental": true,
  "builder": {
    "gc": {
      "enabled": true,
      "defaultKeepStorage": "10GB",
      "policy": [
          {"keepStorage": "10GB", "filter": ["unused-for=2200h"]},
          {"keepStorage": "50GB", "filter": ["unused-for=3300h"]},
          {"keepStorage": "75GB", "all": true}
      ]
    }
  },
  "default-ulimits": {
    "nofile": {
      "Hard": 64000,
      "Name": "nofile",
      "Soft": 8000
    }
  }
}' > /etc/docker/daemon.json

# Start Docker CE
systemctl enable docker
systemctl start docker

# Add wpbuild to docker group
usermod -aG docker wpbuild
