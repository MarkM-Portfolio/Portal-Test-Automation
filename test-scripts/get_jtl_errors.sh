# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/usr/bin/env bash

searchTerm="errorsTable"
fileToSearch=$1
logLocation="/home/centos/jtl_errors.json"
statisticsJsonFileLocation=$2

# Check the filename
if [ -z "$fileToSearch" ]
then
    echo "file name not provided"
    exit 1
fi

errorCount=$(jq -r '.Total.errorCount' $statisticsJsonFileLocation)
echo "errorCount in statistics.json file: $errorCount"

# check error count and if it is greated than zero , send errors to json
if [ $errorCount -gt 0 ]
then  
        # Search for jmeter results statistic in the given file
        searchResult=$(grep $searchTerm $fileToSearch)
        #echo "searchResult - : $searchResult"
        # split the results from searched line delimited by comma and result is stored in array
        searchResultArray=()
        IFS=$',' searchResultArray=($searchResult)
        unset IFS

        echo "$searchResult" >> temp.log

        # Extract the JSON content from the file
        jsonErrorData=$(grep -o '\[{.*}\]' temp.log | jq --raw-output '.[] | .data[0], .data[1]')
        # echo "jsonData: $jsonErrorData"

        IFS=$'\n' read -rd '' -a jsonErrorArray <<< "$jsonErrorData"

        # Function to sanitize strings by escaping control characters
        function sanitize_string {
            local str="$1"
            echo "$str" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/[^[:print:]]//g'
        }

        # Function to convert elements of the array into JSON format using jq
        function array_to_json {
            local typeOfErrors="$1"
            local numberOfErrors="$2"
            echo "{ \"typeOfErrors\": \"$(sanitize_string "$typeOfErrors")\", \"numberOfErrors\": $numberOfErrors }"
        }

        # Construct the JSON array
        jsonArray="{ \"jtlErrorInfo\": ["

        # Iterate through the array and add each pair of elements as a JSON object to the array
        for ((i = 0; i < ${#jsonErrorArray[@]}; i += 2)); do
            typeOfErrors="${jsonErrorArray[i]}"
            numberOfErrors="${jsonErrorArray[i + 1]}"
            
            # Add the JSON object to the array
            jsonArray+="$(array_to_json "$typeOfErrors" "$numberOfErrors"), "
        done

        # Remove the trailing comma and close the array
        jsonArray="${jsonArray%, *} ] }"

        # Print the final JSON array
        echo "$jsonArray"
        echo "$jsonArray" >> $logLocation

        # delete temp log file
        rm temp.log
else
   jtl_no_errors="{\"jtlErrorInfo\": []}"
   echo "no errors in jtl report"
   echo "$jtl_no_errors" >> $logLocation
fi 

