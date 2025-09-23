#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2024. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

cd ~/whitesource

mkdir logs

# Docker image scans

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-runtime-controller-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dx-license-manager-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dx-core-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dx-core-remote-search-image.config &

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-openldap-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-logging-sidecar-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-prereqs-checker-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-haproxy-image.config &

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-ringapi-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-content-ui-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-image-processor-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-site-manager-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-search-middleware-image.config &

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-media-library-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-media-library-persistence-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-media-library-persistence-pgpool-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-media-library-persistence-repmgr-image.config &

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dam-plugin-kaltura-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dam-plugin-google-vision-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dam-plugin-kaltura-image.config &

java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-liberty-portlet-container-image.config &

wait
echo "Container image scans done"


# DX Client scans
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dxclient-image.config &
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dxclient-zip.config -d ~/whitesource/artifacts/dxclient &

wait
echo "DX client scans done"

# DX Extensions scan
java -d64 -Xms16g -Xmx32g -jar wss-unified-agent.jar -c ./config/wss-dx-extensions.config -d ~/whitesource/artifacts/dx-extensions &

wait
echo "DX Extensions scan done"

# DX Core Profile scans

DX_CORE_IMAGE_NAME=$(docker images --format "{{.Repository}}" |grep "/dx-build-output/core/dxen$")
DX_CORE_IMAGE_ID=$(docker images $DX_CORE_IMAGE_NAME --format "{{.ID}}")
DX_CORE_IMAGE_VERSION=$(docker images $DX_CORE_IMAGE_NAME --format "{{.Tag}}")

echo "Found DX Core image:"
echo "IMAGE_NAME = $DX_CORE_IMAGE_NAME"
echo "IMAGE_ID = $DX_CORE_IMAGE_ID"
echo "IMAGE_VERSION = $DX_CORE_IMAGE_VERSION"

docker run -d -p 80:10039 -p 443:10041 $DX_CORE_IMAGE_ID

DX_CORE_CONTAINER_ID=$(docker ps -l --format {{.ID}})

echo "CONTAINER_ID = $DX_CORE_CONTAINER_ID"

mkdir $DX_CORE_IMAGE_VERSION
cd $DX_CORE_IMAGE_VERSION
docker cp $DX_CORE_CONTAINER_ID:/opt/HCL/PortalServer .
docker cp $DX_CORE_CONTAINER_ID:/opt/HCL/AppServer/profiles/cw_profile .
docker cp $DX_CORE_CONTAINER_ID:/opt/HCL/wp_profile .
echo "Profile copies done"

docker stop $DX_CORE_CONTAINER_ID
docker rm $DX_CORE_CONTAINER_ID
echo "DX Core stopped"

sed -i "s/INSERT_PROJECT_NAME_HERE/DX-Core-$DX_CORE_IMAGE_VERSION/g" ../config/wss-dx-core-was-profiles.config

java -d64 -Xms16g -Xmx32g -jar ../wss-unified-agent.jar -c ../config/wss-dx-core-was-profiles.config &


cd ..

# DX Core Remote Search Profile scans

DX_CORE_RS_IMAGE_NAME=$(docker images --format "{{.Repository}}" |grep "/dx-build-output/core/dxrs$")
DX_CORE_RS_IMAGE_ID=$(docker images $DX_CORE_RS_IMAGE_NAME --format "{{.ID}}")
DX_CORE_RS_IMAGE_VERSION=$(docker images $DX_CORE_RS_IMAGE_NAME --format "{{.Tag}}")

echo "Found DX remote search image:"
echo "IMAGE_NAME = $DX_CORE_RS_IMAGE_NAME"
echo "IMAGE_ID = $DX_CORE_RS_IMAGE_ID"
echo "IMAGE_VERSION = $DX_CORE_RS_IMAGE_VERSION"

docker run -d $DX_CORE_RS_IMAGE_ID

DX_CORE_RS_CONTAINER_ID=$(docker ps -l --format {{.ID}})

echo "CONTAINER_ID = $DX_CORE_RS_CONTAINER_ID"

mkdir $DX_CORE_RS_IMAGE_VERSION
cd $DX_CORE_RS_IMAGE_VERSION

docker cp $DX_CORE_RS_CONTAINER_ID:/opt/HCL/PortalRemoteSearch .
docker cp $DX_CORE_RS_CONTAINER_ID:/opt/HCL/AppServer/profiles/prs_profile .
echo "Profile copies done"

docker stop $DX_CORE_RS_CONTAINER_ID
docker rm $DX_CORE_RS_CONTAINER_ID
echo "DX remote search stopped"

sed -i "s/INSERT_PROJECT_NAME_HERE/DX-Core-RS-$DX_CORE_RS_IMAGE_VERSION/g" ../config/wss-dx-core-rs-was-profiles.config

java -d64 -Xms16g -Xmx32g -jar ../wss-unified-agent.jar -c ../config/wss-dx-core-rs-was-profiles.config &
wait

echo "DX Core profile scans done"
