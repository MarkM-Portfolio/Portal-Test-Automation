#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# Installs and runs the content-ui
#

# CONTENT-UI-SERVER DOCKER DEPLOYMENT

# stop and remove existing dx container running
docker stop $CONTENT_UI_CONTAINER_NAME
docker rm $CONTENT_UI_CONTAINER_NAME

# retrieve the content-ui index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/content-ui/ -o content-ui.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' content-ui.list > content-ui-clean.list

# remove all irrelevant content
grep '[0-9]-[0-9]' content-ui-clean.list | grep "$CONTENT_UI_IMAGE_FILTER"  > content-ui-rel.list

# create a csv out of it
cat content-ui-rel.list | tr -s '[:blank:]' ',' > content-ui-rel.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> content-ui-transformed.csv
done < content-ui-rel.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr content-ui-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm content-ui.list content-ui-clean.list content-ui-rel.list content-ui-rel.csv content-ui-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/portal/content-ui:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/portal/content-ui -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/portal/content-ui:$latest
fi

# run the content-ui
execCmd="docker run -d $CONTENT_UI_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $CONTENT_UI_CONTAINER_NAME $CONTENT_UI_ENV_CONFIGURATION $ARTIFACTORY_HOST/portal/content-ui:$latest"
eval "$execCmd"

