#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# Installs and runs the site-manager
#

# SITE-MANAGER-SERVER DOCKER DEPLOYMENT

# stop and remove existing dx container running
docker stop $SITE_MANAGER_CONTAINER_NAME
docker rm $SITE_MANAGER_CONTAINER_NAME

# retrieve the site-manager index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/site-manager/ -o site-manager.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' site-manager.list > site-manager-clean.list

# remove all irrelevant content
grep '[0-9]-[0-9]' site-manager-clean.list | grep "$SITE_MANAGER_IMAGE_FILTER"  > site-manager-rel.list

# create a csv out of it
cat site-manager-rel.list | tr -s '[:blank:]' ',' > site-manager-rel.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> site-manager-transformed.csv
done < site-manager-rel.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr site-manager-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm site-manager.list site-manager-clean.list site-manager-rel.list site-manager-rel.csv site-manager-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/portal/site-manager:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/portal/site-manager -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/portal/site-manager:$latest
fi

# run the site-manager
execCmd="docker run -d $SITE_MANAGER_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $SITE_MANAGER_CONTAINER_NAME $SITE_MANAGER_ENV_CONFIGURATION $ARTIFACTORY_HOST/portal/site-manager:$latest"
eval "$execCmd"

