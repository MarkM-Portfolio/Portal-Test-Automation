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
# The script determines the latest image-processor image to use, pull it and runs it and remove the old ones
#

# stop and remove existing dx container running
docker stop $IMG_PROCESSOR_CONTAINER_NAME
docker rm $IMG_PROCESSOR_CONTAINER_NAME

# retrieve the image-processor index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/image-processor/ -o img-processor.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' img-processor.list > img-processor-clean.list

# remove all irrelevant content
grep '[0-9]-[0-9]' img-processor-clean.list | grep "$IMG_PROCESSOR_IMAGE_FILTER"  > img-processor-rel.list

# create a csv out of it
cat img-processor-rel.list | tr -s '[:blank:]' ',' > img-processor-rel.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> img-processor-transformed.csv
done < img-processor-rel.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr img-processor-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm img-processor.list img-processor-clean.list img-processor-rel.list img-processor-rel.csv img-processor-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/portal/image-processor:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/portal/image-processor -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/portal/image-processor:$latest
fi

# run the image-processor
execCmd="docker run -d $IMG_PROCESSOR_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $IMG_PROCESSOR_CONTAINER_NAME $IMG_PROCESSOR_ENV_CONFIGURATION $ARTIFACTORY_HOST/portal/image-processor:$latest"
eval "$execCmd"
