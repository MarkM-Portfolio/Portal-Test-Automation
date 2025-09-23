#!/bin/sh
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

drawioImage='quintana-docker.artifactory.cwp.pnp-hcl.com/utility/drawio:16.5.2'
nginxImage='quintana-docker.artifactory.cwp.pnp-hcl.com/utility/nginx:1.21.3'
domain='team-q-dev.com'

# Pull docker images
docker pull $drawioImage
docker pull $nginxImage

# Stop services if already running
docker stop drawio || true
docker rm drawio || true
docker stop nginx || true
docker rm nginx || true

# delete network if existing
docker network rm drawio || true

# remove pulled certificates
rm -rf ./certs || true
mkdir -p ./certs

# Create new docker network
docker network create drawio

# Retrieve wildcard certificate
cd ./certs
curl -JL https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore/$domain/fullchain.cer -o tls_cert.cer
curl -JL https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-truststore/$domain/$domain.key -o tls_cert.key
cd ..

# Create docker containers
docker run --restart unless-stopped --name drawio --network drawio -d $drawioImage
docker run --restart unless-stopped --name nginx -v $(pwd)/certs:/etc/nginx/certs:ro -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro --network drawio -p 80:80 -p 443:443 -d $nginxImage