#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2024. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install OS dependencies
yum -y update libgcc* libstdc++* glibc*
yum -y install java-1.8.0-openjdk
yum -y install git
yum -y install gcc gcc-c++
yum -y install libgcc*i686 libstdc++*i686 glibc*i686
yum -y install unzip
yum -y install zip

# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install
/usr/local/bin/aws --version
# Create AWS credentials
su - wpbuild -c "mkdir -p /home/wpbuild/.aws"
su - wpbuild -c "echo -e '[default]\naws_access_key_id = $2\naws_secret_access_key = $3' > /home/wpbuild/.aws/credentials"

# Install AZURE CLI
rpm --import https://packages.microsoft.com/keys/microsoft.asc
echo -e "[azure-cli]
name=Azure CLI
baseurl=https://packages.microsoft.com/yumrepos/azure-cli
enabled=1
gpgcheck=1
gpgkey=https://packages.microsoft.com/keys/microsoft.asc" | tee /etc/yum.repos.d/azure-cli.repo
yes Y | yum install azure-cli
az --version

# Install GCLOUD CLI and gcloud authentication plugin
yes y | sudo tee -a /etc/yum.repos.d/google-cloud-sdk.repo << EOM
[google-cloud-sdk]
name=Google Cloud SDK
baseurl=https://packages.cloud.google.com/yum/repos/cloud-sdk-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=0
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg
       https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOM
sudo yum install google-cloud-sdk -y 
gcloud -v
sudo yum install google-cloud-sdk-gke-gcloud-auth-plugin -y
gke-gcloud-auth-plugin --version

# Install xvfb and its dependencies
yum -y install Xvfb xorg-x11-server-Xvfb

# Install a11y toolkit and its dependencies
yum -y install libatk-bridge-2.0.so.0 atk at-spi2-atk gtk3 libXt libdrm mesa-libgbm

# Install java-atk-wrapper by itself as it has had install issues in the past
yum -y install java-atk-wrapper

# Install and Configure Ansible
yum -y install epel-release
yum -y install ansible

mkdir -p /etc/ansible
NEWLINE=$'\n'
echo "[webservers]${NEWLINE}$1 ansible_user=wpbuild" > /etc/ansible/hosts
sed -i "s'\[defaults\]'[defaults]\nhost_key_checking = False'g" /etc/ansible/ansible.cfg

#Install perl modules
yum -y install perl-File-Copy
yum -y install perl-File-Compare

# Install operator build prerequisites
GOVERSION=1.15.2
curl -LO https://dl.google.com/go/go${GOVERSION}.linux-amd64.tar.gz
tar -C /usr/local -xvf go${GOVERSION}.linux-amd64.tar.gz
export GOROOT=/usr/local/go
export GOPATH=/home/$USER/projects/
export PATH=$GOPATH/bin:$GOROOT/bin:$PATH
mkdir -p /home/$USER/projects/bin
curl https://raw.githubusercontent.com/golang/dep/master/install.sh | sh
cp /home/$USER/projects/bin/dep $GOROOT/bin/dep
export RELEASE_VERSION=v0.13.0
curl -OJL https://github.com/operator-framework/operator-sdk/releases/download/${RELEASE_VERSION}/operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu
chmod +x operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu && sudo mkdir -p /usr/local/bin/ && sudo cp operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu /usr/local/bin/operator-sdk && rm -f operator-sdk-${RELEASE_VERSION}-x86_64-linux-gnu
su - wpbuild -c "curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash"
su - wpbuild -c "nvm install 14"
