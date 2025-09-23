#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *                                                                  *
# * repo_url should look like (default):                             *
# * https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic *
# *                                                                  *
# * tgt_repo should look like (default):  appscan/reports            *
# ********************************************************************


user=$1
pass=$2
repo_url=$3
tgt_repo=$4

dir=$HOME"/appscan/repos/reports"
if [ -z "$dir" ]; then echo "Please specify a directory to recursively upload from!"; exit 1; fi

find "$dir" -type f | while read f; do
    rel="$(echo "$f" | sed -e "s#$dir##" -e "s# /#/#")";
    sha1=$(sha1sum "$f")
    sha1="${sha1:0:40}"
    printf "\n\nUploading '$f' (cs=${sha1}) to '${repo_url}/${tgt_repo}${rel}'"

    status=$(curl -k -u $user:$pass -X PUT -H "X-Checksum-Deploy:true" -H "X-Checksum-Sha1:$sha1" --write-out %{http_code} --silent --output /dev/null "${repo_url}/${tgt_repo}${rel}")
    echo "status=$status"
    # No checksum found - deploy + content
    [ ${status} -eq 404 ] && {
        curl -k -u $user:$pass -H "X-Checksum-Sha1:$sha1" -T "$f" "${repo_url}/${tgt_repo}/${rel}"
    }
done


echo "AppScan reports uploaded"