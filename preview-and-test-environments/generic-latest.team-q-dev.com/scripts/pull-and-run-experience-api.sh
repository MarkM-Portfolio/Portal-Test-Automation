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
# The script determines the latest experience api image to use, pull it and runs it and remove the old ones
#

# stop and remove existing dx container running
docker stop $EXPERIENCE_API_CONTAINER_NAME
docker rm $EXPERIENCE_API_CONTAINER_NAME

# retrieve the  experience api index page, listing all image tags
curl $ARTIFACTORY_IMAGE_BASE_URL/portal/api/ringapi/ -o experience-api.list

# remove HTML from the output
sed -e 's/<[^>]*>//g' experience-api.list > experience-api-clean.list

# remove all relevant content
grep '[0-9]-[0-9]' experience-api-clean.list | grep "$EXPERIENCE_API_IMAGE_FILTER"  > experience-api-rel.list

# create a csv out of it
cat experience-api-rel.list | tr -s '[:blank:]' ',' > experience-api-rel.csv

# transform all dates to a sortable format
while read line; do
        var1=$(echo "$line" | cut -d ',' -f 1)
        var2=$(echo "$line" | cut -d ',' -f 2)
        var2=$(date --date="$(printf "01 %s" $var2)" +"%Y-%m-%d")
        echo "$var1,$var2" >> experience-api-transformed.csv
done < experience-api-rel.csv

# get the latest image tag (first line of csv)
latest=$(sort -t, -k2,2 -nr experience-api-transformed.csv | head -n 1 | cut -d ',' -f 1)
latest=${latest%/}
echo "Going to pull the following image: $latest"

# tidy up
rm experience-api.list experience-api-clean.list experience-api-rel.list experience-api-rel.csv experience-api-transformed.csv

if $(docker inspect $ARTIFACTORY_HOST/portal/api/ringapi:$latest > /dev/null 2>&1)
then
  echo "Latest image already pulled."
else
  echo "Require to pull latest image."
  echo "Removing old images beforehand."
  docker rmi -f $(docker images $ARTIFACTORY_HOST/portal/api/ringapi -a -q)
  #now pull the docker image
  docker pull $ARTIFACTORY_HOST/portal/api/ringapi:$latest
fi

# run the experience api
execCmd="docker run -d $EXPERIENCE_API_PORT_CONFIGURATION --network $DOCKER_NETWORK_NAME --name $EXPERIENCE_API_CONTAINER_NAME $EXPERIENCE_API_ENV_CONFIGURATION $ARTIFACTORY_HOST/portal/api/ringapi:$latest"
eval "$execCmd"
