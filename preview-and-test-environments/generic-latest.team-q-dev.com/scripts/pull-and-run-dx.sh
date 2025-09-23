#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# The script determines the latest dx image to use, pull it and runs it
#

do_wait_for_container(){
#wait for the container using curl, netstat is not accurate
n=1
while [[ "$(curl -L -s -o /dev/null -w ''%{http_code}'' $DX_CORE_URL)" != "200" ]]
do
        echo "Container not running yet, waiting another 30s"
        sleep 30
        n=$(( n+1 ))
        if [ $n -eq 20 ]; then
          echo "Container failed to run within alotted time"
          exit 1
        fi
done
echo "Container is running"
}

# stop and remove existing dx container running
docker stop $DX_CORE_CONTAINER_NAME
docker rm $DX_CORE_CONTAINER_NAME

echo "ARTIFACTORY_HOST: $ARTIFACTORY_HOST"

# retrieve the dxen index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/dxen/ -o dxen.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' dxen.list > dxen-clean.list

# remove all non v95 content
echo "DX_CORE_IMAGE_FILTER: $DX_CORE_IMAGE_FILTER"
grep "$DX_CORE_IMAGE_FILTER" dxen-clean.list > dxen-v95.list

# create a csv out of it
cat dxen-v95.list | tr -s '[:blank:]' ',' > dxen-v95.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var3=$(echo "$line" | cut -d ',' -f 3)
        var2=$(date --date="$var2 $var3" +"%s")
        echo "$var1,$var2" >> dxen-transformed.csv
done < dxen-v95.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr dxen-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm dxen.list dxen-clean.list dxen-v95.list dxen-v95.csv dxen-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/dxen:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/dxen -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/dxen:$latest
fi

# run the dx image
execCmd="docker run -d $DX_CORE_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $DX_CORE_CONTAINER_NAME $DX_CORE_ENV_CONFIGURATION $ARTIFACTORY_HOST/dxen:$latest"
eval "$execCmd"

#add media library integration
## need to wait for DX to start before running this
do_wait_for_container

#EPHOX
docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh update-textbox-io-service-config-jvm-settings-explicit -DDxHost=http://$ENV_HOSTNAME:10039 -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin"

#CC and DAM addition
if [ "$MEDIA_LIBRARY_IMAGE_FILTER" != "SKIP" ]; then
  docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-media-library -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dstatic.ui.url=http://$ENV_HOSTNAME:3000/dx/ui/dam/static"
  DAM_PATH="dx/ui/dam/static"
  #For older releases, we need to use the old context-route
  if [[ "$MEDIA_LIBRARY_IMAGE_FILTER" == *"CF18" ]]; then
    DAM_PATH="dx/ui/media-library/static"
  fi
  if [[ "$MEDIA_LIBRARY_IMAGE_FILTER" == *"CF173" ]]; then
    DAM_PATH="dx/ui/media-library/static"
  fi
  docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-media-library -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -DdigitalAssets.baseUrl=http://$ENV_HOSTNAME:3000 -DdigitalAssets.uiSuffix=/#/home/media?hcldam=true -Dexperience.api.url=http://$ENV_HOSTNAME:4000/dx/api/core/v0 -Dstatic.ui.url=http://$ENV_HOSTNAME:3000/$DAM_PATH"
fi
if [ "$CONTENT_UI_IMAGE_FILTER" != "SKIP" ]; then
  docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-headless-content -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dstatic.ui.url=http://$ENV_HOSTNAME:5000/dx/ui/content/static"
fi
if [[ "$SITE_MANAGER_IMAGE_FILTER" != "SKIP" && ! -z "$SITE_MANAGER_IMAGE_FILTER" || "$SITE_MANAGER_IMAGE_FILTER" != "" ]]; then
  docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh enable-content-sites -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dcontentsites.static.ui.url=http://$ENV_HOSTNAME:5500/dx/ui/site-manager/static"
fi

#restart WAS
docker exec $DX_CORE_CONTAINER_NAME sh -c "/opt/HCL/AppServer/profiles/cw_profile/bin/./startServer.sh server1"
