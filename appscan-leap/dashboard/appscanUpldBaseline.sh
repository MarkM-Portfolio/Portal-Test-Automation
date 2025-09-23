#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************


user=$1
pass=$2
dir=$3

if [ "$4" != "" ]; then
    repo_url="$4"
else
    repo_url="https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/appscan/leap-reports/baseline"
fi    


if [ -z "$dir" ]; then echo "Please specify a directory to recursively upload from!"; exit 1; fi

echo "Delete old baseline in Artifactory"
curl -u $user:$pass --silent -X DELETE  "${repo_url}"

echo "Upload new baseline to Artifactory"
find "$dir" -type f | while read f; do
    rel="$(echo "$f" | sed -e "s#$dir##" -e "s# /#/#")";
    result=$(curl -k --silent -u $user:$pass -T "$f" "${repo_url}${rel}")
done
