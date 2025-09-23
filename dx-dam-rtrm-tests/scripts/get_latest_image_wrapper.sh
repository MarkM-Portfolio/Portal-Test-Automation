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

if [ ! -z "$4" ]
then
    export IS_OLD_PATTERN="$4"
fi

./get_latest_image.sh
