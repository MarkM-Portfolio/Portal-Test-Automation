#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install OS dependencies for the upcoming setup

domain='team-q-dev.com'
# NGINX setup comes from the EPEL-Repository
sudo yum -y install epel-release
# Install NGINX via yum
sudo yum -y install nginx


# remove pulled certificates
rm -rf ./certs || true
mkdir -p ./certs


# Retrieve wildcard certificate
cd ./certs
curl -JL https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore/$domain/fullchain.cer -o tls_cert.cer
curl -JL https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore/$domain/$domain.key -o tls_cert.key
sudo mkdir -p /etc/nginx/certs
sudo cp /home/centos/certs/tls_cert.cer /etc/nginx/certs/tls_cert.cer
sudo cp /home/centos/certs/tls_cert.key /etc/nginx/certs/tls_cert.key
sudo chmod -R 755 /etc/nginx/certs
cd ..

# Enable NGINX service
sudo systemctl enable --now nginx