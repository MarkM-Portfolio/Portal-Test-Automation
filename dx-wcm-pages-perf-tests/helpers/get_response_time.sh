# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/usr/bin/env bash

searchTerm="statisticsTable"
fileToSearch=$1
logLocation="/home/centos/wcm_rendering_performance_results.log"

# Check the filename
if [ -z "$fileToSearch" ]; then
    echo "file name not provided"
    exit 1
fi

# Search for jmeter results statistic in the given file
searchResult=$(grep "$searchTerm" "$fileToSearch")

# split the results from searched line delimited by comma and result is stored in array
searchResultArray=()
IFS=$',' read -ra searchResultArray <<< "$searchResult"

# Loop through result array to filter for fetch binary response time for image, document and video
for i in "${!searchResultArray[@]}"; do

    if [[ ${searchResultArray[$i]} == *"Total\"" ]]; then
        index=$(($i + 4))
        echo "Total avg response time : - $(printf '%.*f\n' 3 "${searchResultArray[$index]}") ms"
        echo "Total avg response time: - $(echo "${searchResultArray[$index]}/1000" | bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000" | bc -l | xargs printf "%.*f\n" 3 )," >> "${logLocation}"
        index=$(($i + 11))
        echo "Total Throughput : - $(printf '%.*f\n' 3 "${searchResultArray[$index]}") /sec"
        echo "Total Throughput : - $(echo "${searchResultArray[$index]}" | bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}" | bc -l | xargs printf "%.*f\n" 3 )," >> "${logLocation}"
    fi
done
