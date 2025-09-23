#!/bin/bash

export IMAGE_PATH=$1

if [ ! -z "$2" ]
then
    export REGEX_PATTERN=$2
    if [ ! -z "$3" ]
    then
        export ARTIFACTORY_URL="$3"
    else
        export ARTIFACTORY_URL=""
    fi
else
    export REGEX_PATTERN=""
fi

./get_latest_image.sh
