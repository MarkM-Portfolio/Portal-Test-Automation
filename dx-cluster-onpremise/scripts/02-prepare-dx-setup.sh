#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

RESPONSEFILES=('Portal85BaseResponse.xml' 'Portal85CF19Response.xml' 'Portal95Response.xml' 'BinaryPortal85BaseResponse.xml' 'BinaryPortal85CF19Response.xml' 'BinaryPortal95Response.xml' 'WASFP9055JDK806.xml' 'WASFP9055APARs.xml' 'RemoteSearch85Response.xml' 'UpdateRemoteSearch.xml')
ENVIRONMENT_VARIABLES=('CFDIR' 'ARTIFACTORY_HOST' 'DX_CORE_BUILD_VERSION')
IIM_VERSION="agent.installer.linux.gtk.x86_64_1.9.0.20190715_0328.zip"

echo "DX_CORE_BUILD_VERSION: $DX_CORE_BUILD_VERSION"
CFDIR="DX_Core/${DX_CORE_BUILD_VERSION}"
CFDIR=${CFDIR//\//\\/}
echo "CFDIR: $CFDIR"

echo "Using the following environment variables to populate the response and configuration files:"
for i in "${ENVIRONMENT_VARIABLES[@]}"
do
	echo "$i"
done

echo "Check existance for RESPONSEFILES:"
fileNotFound=0
for i in "${RESPONSEFILES[@]}"
do
    if [ -e "./helpers/$i" ]; then
        echo "./helpers/$i OK!"
    else
        echo "./helpers/$i ERROR - NOT FOUND!"
        fileNotFound=1
    fi
done
if [ "$fileNotFound" == "1" ]; then
    exit 1
fi

for i in "${RESPONSEFILES[@]}"
do
    echo "Going to populate $i with the necessary data from configured environment variables."
    for j in "${ENVIRONMENT_VARIABLES[@]}"
    do
        REPLACEMENTVAR="${!j}"
        eval "sed -i.bak 's/\$$j/${REPLACEMENTVAR}/g' ./helpers/$i"
    done
done

# only download iim if not already there, useful for local debugging
if [ ! -f "./helpers/iim_setup.zip" ]; then
    echo "Downloading IIM from the FTP Server"
    cp /tmp/msa/rtpmsa/projects/b/build.portal/builds/image_prereqs/$IIM_VERSION .
    mv $IIM_VERSION ./helpers/iim_setup.zip
fi

