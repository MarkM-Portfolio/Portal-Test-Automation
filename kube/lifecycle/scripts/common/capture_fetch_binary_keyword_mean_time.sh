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
njdclogFile="/home/dam_jmeter_user/dam_performance_search_keywords.log"

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

# Loop through result array to filter for fetch response time for search keyword
for i in ${!searchResultArray[@]}; do
    if [[ ${searchResultArray[$i]} == *"Keyword\"" ]]; then
        index=$i+4
        echo "mean value of keyword response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of keyword response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds"
        if [ "$ENVIRONMENT" = "openshiftnjdc" ]
            then
            printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds," >> ${njdclogFile}
        fi 
    fi
done 