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

# Loop through result array to filter average time for each get apis
for i in ${!searchResultArray[@]}; do    
    if [[ ${searchResultArray[$i]} == *"ItemId\"" ]]; then
        index=$i+4
        echo "mean value of Get itemid api response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get itemid api response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) seconds"
    fi

    if [[ ${searchResultArray[$i]} == *"Renditions\"" ]]; then
        index=$i+4
        echo "mean value of Get Renditions api response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get Renditions api response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) seconds"
    fi

    if [[ ${searchResultArray[$i]} == *"RenditionID\"" ]]; then
        index=$i+4
        echo "mean value of Get RenditionID response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get RenditionID response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds"
    fi

    if [[ ${searchResultArray[$i]} == *"Versions\"" ]]; then
        index=$i+4
        echo "mean value of Get Versions response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get Versions response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds"
    fi

    if [[ ${searchResultArray[$i]} == *"VersionsID\"" ]]; then
        index=$i+4
        echo "mean value of Get VersionsID response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get VersionsID response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds"
    fi

    if [[ ${searchResultArray[$i]} == *"References\"" ]]; then
        index=$i+4
        echo "mean value of Get References response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "mean value of Get References response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) seconds"
    fi
done
