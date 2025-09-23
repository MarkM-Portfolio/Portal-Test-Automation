# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

export http_proxy=$1
export https_proxy=$1
printenv | sort

# Add docker repo
sudo https_proxy=$1 http_proxy=$1 yum-config-manager -y --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo https_proxy=$1 http_proxy=$1 yum install -y docker-ce docker-ce-cli containerd.io


sudo mkdir -p /etc/systemd/system/docker.service.d

echo "
[Service]
Environment="HTTP_PROXY=$1"
Environment="HTTPS_PROXY=$1"
" | sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf

sudo systemctl daemon-reload
sudo systemctl enable --now docker

# Add user to docker group
sudo usermod -aG docker centos