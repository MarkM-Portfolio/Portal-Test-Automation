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
#   using develop as filter reduces the possible images to _pjs_ and not _on_develop_
# ARTIFACTORY_URL (defaults to https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker)


#defaults
image="dxen"
regex="develop"
artifactoryUrl="https://artifactory.cwp.pnp-hcl.com/artifactory/list/quintana-docker"

if [[ "$IMAGE_PATH" != "" ]]; then
    image=$IMAGE_PATH
fi

if [[ "$REGEX_PATTERN" != "" ]]; then
    regex=$REGEX_PATTERN
fi
if [[ "$ARTIFACTORY_URL" != "" ]]; then
    artifactoryUrl=$ARTIFACTORY_URL
fi

# echo "################"
# echo "# image : $image"
# echo "# regex : $regex"
# echo "# artifactoryUrl : $artifactoryUrl"
# echo "################"


if [ "$regex" == "develop" ]; then
    linearray=($(curl --silent "$artifactoryUrl/$image/" | grep $regex | grep "_pjs_" | grep -v "_on_develop_" | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//' | sed 's/<//'))
else
    linearray=($(curl --silent "$artifactoryUrl/$image/" | grep $regex | grep -v "_debug" |tr -s '[:blank:]' ',' | cut -d ',' -f 2 | sed 's/.*">//' | sed 's/\/.*>//' | sed 's/<//'))
fi

sortedTargs=$(for a in "${linearray[@]}"; do echo ":$a"; done | sort -V)
latestImageTag=${sortedTargs##*:}

echo -n $latestImageTag
