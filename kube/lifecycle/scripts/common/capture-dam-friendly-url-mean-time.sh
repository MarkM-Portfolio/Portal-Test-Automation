# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/usr/bin/env bash

searchTerm="statisticsTable"
fileToSearch=$1
ENVIRONMENT=$2
logLocation="/home/centos/dam_FriendlyUrl/dam_performance_friendlyUrl_results.log"

# Check the filename
if [ -z "$fileToSearch" ]
then
    echo "file name not provided"
    exit 1
fi

# Search for jmeter results statistic in the given file
searchResult=$(grep $searchTerm $fileToSearch)

# split the results from searched line delimited by comma and result is stored in array
searchResultArray=()
IFS=$',' searchResultArray=($searchResult)
unset IFS

# Loop through result array to filter for get api time by assetID, assetName, customURL
for i in ${!searchResultArray[@]}; do
    if [[ ${searchResultArray[$i]} == *" assetID\"" ]]; then
        index=$i+4
        echo "mean value of get api by assetID : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of get api by assetID: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds"
        if [ "$ENVIRONMENT" = "openshiftnjdc" ]
            then
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds," >> /home/dam_jmeter_user/dam_performance_friendly_url_results.log
            else
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds," >> ${logLocation}
        fi
    fi

    if [[ ${searchResultArray[$i]} == *" assetName\"" ]]; then
        index=$i+4
        echo "mean value of get api by assetName: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of get api by assetName: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) seconds"
        if [ "$ENVIRONMENT" = "openshiftnjdc" ]
            then
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) seconds," >> /home/dam_jmeter_user/dam_performance_friendly_url_results.log
            else
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) seconds," >>  ${logLocation}
        fi
    fi

    if [[ ${searchResultArray[$i]} == *"assetCustomURL\"" ]]; then
        index=$i+4
        echo "mean value of get api by custom URL: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of get api by custom URL: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds"
        if [ "$ENVIRONMENT" = "openshiftnjdc" ]
            then
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds," >> /home/dam_jmeter_user/dam_performance_friendly_url_results.log
            else
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds," >>  ${logLocation}
        fi
    fi
done
