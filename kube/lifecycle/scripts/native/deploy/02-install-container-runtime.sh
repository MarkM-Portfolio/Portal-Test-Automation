#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2020, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install Docker CE (docker is used to run DB2)
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Set cgroup driver required for Kubernetes 1.22+
sudo mkdir -p /etc/docker
cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2",
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 1048576,
      "Soft": 1048576
    }
  }
}
EOF

# Start Docker CE
sudo systemctl enable --now docker

# Add centos to docker group
sudo usermod -aG docker centos

# Configure containerd as CRI and with increased ulimit
sudo sed -i.bck "s'disabled_plugins'#disabled_plugins'g" /etc/containerd/config.toml
sudo mkdir /etc/systemd/system/containerd.service.d
cat <<EOF | sudo tee /etc/systemd/system/containerd.service.d/override.conf
[Service]
LimitNOFILE=1048576
EOF
sudo systemctl daemon-reload
sudo systemctl restart containerd

# Install nerdctl (CLI interface for easy interaction with containerd)
sudo curl -OJ https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/nerdctl/nerdctl-1.3.1-linux-amd64.tar.gz
sudo tar Cxzvvf /usr/bin nerdctl-1.3.1-linux-amd64.tar.gz
sudo rm -f nerdctl-1.3.1-linux-amd64.tar.gz

# Install buildkit (used by nerdctl)
sudo curl -OJ https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/buildkit/buildkit-v0.11.6.linux-amd64.tar.gz
sudo tar Cxzvvf /usr buildkit-v0.11.6.linux-amd64.tar.gz
sudo rm -f buildkit-v0.11.6.linux-amd64.tar.gz
sudo cp /home/centos/native-kube/buildkit.* /etc/systemd/system
sudo systemctl daemon-reload
sudo systemctl start buildkit.socket
sudo systemctl start buildkit.service
sudo systemctl enable buildkit.socket buildkit.service