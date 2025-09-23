# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

# this script can accept up to 3 environment parameters:
# IMAGE_PATH the path to the image in artifactory: e.g. "dxen", "portal/api/ringapi"
# REGEX (defaults to develop) is the regex used to pre-filter the existing images
# ARTIFACTORY_URL (defaults to https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker)


#defaults
regex="rivendell_master"
artifactoryUrl="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-generic-prod/portal/packaging/production/"

if [[ "$1" != "" ]]; then
    image=$1
fi

if [[ "$2" != "" ]]; then
    regex=$2
fi
if [[ "$3" != "" ]]; then
    artifactoryUrl=$3
fi

# echo "################"
# echo "# image : $image"
# echo "# regex : $regex"
# echo "# artifactoryUrl : $artifactoryUrl"
# echo "################"

if [[ $artifactoryUrl == *".gcr."* ]]; then
    linearray=($(gcloud container images list-tags $artifactoryUrl/$image | grep $regex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//'))
elif [[ $artifactoryUrl == *".ecr."* ]]; then
    registryId=$(echo $artifactoryUrl | cut -d'.' -f1)
    region=$(echo $artifactoryUrl | cut -d'.' -f4)
    linearray=($(aws ecr list-images --registry-id $registryId --repository-name $image --region $region --output text | grep $regex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 3 | sed 's/.*">//' | sed 's/\/.*>//'))
elif [[ $artifactoryUrl == *".azurecr."* ]]; then
    acrName=$(echo $artifactoryUrl | cut -d'.' -f1)
    linearray=($(az acr repository show-tags --name  $acrName --repository $image --output table | grep $regex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 3 | sed 's/.*">//' | sed 's/\/.*>//'))
else
    linearray=($(curl --silent "$artifactoryUrl/$image/" | grep $regex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//'))
fi

sortedTargs=$(for a in "${linearray[@]}"; do echo ":$a"; done | sort -V)
latestImageTag=${sortedTargs##*:}

echo -n $latestImageTag