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
ENVIRONMENT=$2
accessType=$3
logLocation="/home/centos/dam_AnonymousRendering/dam_anonymous_performance_binary_fetch_results.log"

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

# Loop through result array to filter for fetch binary response time for total(avg response time and throughput), image, document and video
for i in ${!searchResultArray[@]}; do
    if [[ ${searchResultArray[$i]} == *"idjpg155kb\"" ]]; then
        index=$i+4
        echo "idjpg155kb avg response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idjpg155kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"curljpg155kb\"" ]]; then
        index=$i+4
        echo "curljpg155kb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "curljpg155kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idmp41.1mb\"" ]]; then
        index=$i+4
        echo "idmp41.1mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idmp41.1mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"curlmp41.1mb\"" ]]; then
        index=$i+4
        echo "curlmp41.1mb avg response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "curlmp41.1mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idpdf171kb\"" ]]; then
        index=$i+4
        echo "idpdf171kb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idpdf171kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"curlpdf171kb\"" ]]; then
        index=$i+4
        echo "curlpdf171kb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "curlpdf171kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"iddocx199kb\"" ]]; then
        index=$i+4
        echo "iddocx199kbavg response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "iddocx199kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furljpg5mbD\"" ]]; then
        index=$i+4
        echo "furljpg5mbD avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furljpg5mbD avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlpng500kbT\"" ]]; then
        index=$i+4
        echo "furlpng500kbT avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlpng500kbT avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlgif2mb\"" ]]; then
        index=$i+4
        echo "furlgif2mb response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlgif2mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furltif300kb\"" ]]; then
        index=$i+4
        echo "furltif300kb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furltif300kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlpdf5mb\"" ]]; then
        index=$i+4
        echo "furlpdf5mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlpdf5mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idxlsx250kb\"" ]]; then
        index=$i+4
        echo "idxlsx250kb response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idxlsx250kb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlpptx2.7mb\"" ]]; then
        index=$i+4
        echo "furlpptx2.7mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlpptx2.7mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idpng500kbD\"" ]]; then
        index=$i+4
        echo "idpng500kbD avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idpng500kbD avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idjpg2mbS\"" ]]; then
        index=$i+4
        echo "idjpg2mbS response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idjpg2mbS avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"idmp415mb\"" ]]; then
        index=$i+4
        echo "idmp415mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "idmp415mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlmp4100mb\"" ]]; then
        index=$i+4
        echo "furlmp4100mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlmp4100mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"furlwebm2mb\"" ]]; then
        index=$i+4
        echo "furlwebm2mb avg response time: - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "furlwebm2mb avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l| xargs printf "%.*f\n" 3) ," >> ${logLocation}
    fi

    if [[ ${searchResultArray[$i]} == *"Total\"" ]]; then
        index=$i+4
        echo "Total avg response time : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) ms"
        echo "Total avg response time: - $(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) "
        printf "$(echo "${searchResultArray[$index]}/1000"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
        index=$i+11
        echo "Throughput : - $(printf '%.*f\n' 3 ${searchResultArray[$index]}) per sec"
        printf "$(echo "${searchResultArray[$index]}"|bc -l | xargs printf "%.*f\n" 3 ) ," >> ${logLocation}
    fi
done 