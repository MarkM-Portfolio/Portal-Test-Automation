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
# Installs and runs the media-library server and ui
#

# ML-SERVER DOCKER DEPLOYMENT

# stop and remove existing dx container running
docker stop $MEDIA_LIBRARY_CONTAINER_NAME
docker rm $MEDIA_LIBRARY_CONTAINER_NAME

# retrieve the media-library index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/media-library/ -o media-library.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' media-library.list > media-library-clean.list

# remove all irrelevant content
grep '[0-9]-[0-9]' media-library-clean.list | grep "$MEDIA_LIBRARY_IMAGE_FILTER"  > media-library-rel.list

# create a csv out of it
cat media-library-rel.list | tr -s '[:blank:]' ',' > media-library-rel.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> media-library-transformed.csv
done < media-library-rel.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr media-library-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm media-library.list media-library-clean.list media-library-rel.list media-library-rel.csv media-library-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/portal/media-library:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/portal/media-library -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/portal/media-library:$latest
fi

# clean postgresql
cd /opt/media-library
docker-compose down -v

# replace artifactory in the yaml
sed -i "s/quintana-docker.artifactory.cwp.pnp-hcl.com/$ARTIFACTORY_HOST/g" docker-compose.yaml

# find the latest matching version for postgresql
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/persistence/postgres/ -o pg.list
sed -e 's/<[^>]*>//g' pg.list > pg-clean.list
grep '[0-9]-[0-9]' pg-clean.list | grep "$MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER"  > pg-rel.list
cat pg-rel.list | tr -s '[:blank:]' ',' > pg-rel.csv
# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> pg-transformed.csv
done < pg-rel.csv
# get the latest image tag (first line of csv)
PERSISTENCE_VERSION=$(sort -t, -k2,2 -nr pg-transformed.csv | head -n 1 | cut -d ',' -f 1)
PERSISTENCE_VERSION=${PERSISTENCE_VERSION%/}

echo "Using image tag $PERSISTENCE_VERSION for the postgresql"

# replace the version in the 
sed -i "s/##version-placeholder##/$PERSISTENCE_VERSION/g" docker-compose.yaml

# start postgresql
cd /opt/media-library
sleep 10
docker-compose up -d
echo "Waiting 10s to give DB time for startup."
sleep 10

# run the media-library
execCmd="docker run -d $MEDIA_LIBRARY_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $MEDIA_LIBRARY_CONTAINER_NAME $MEDIA_LIBRARY_ENV_CONFIGURATION $ARTIFACTORY_HOST/portal/media-library:$latest"
eval "$execCmd"

# initDB
docker network connect $DOCKER_NETWORK_NAME media-library_pg-primary_1
