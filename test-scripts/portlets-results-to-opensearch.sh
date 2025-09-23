# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

# Pages and Portlets performance results from log file extracted and pushed to OpenSearch dashboard

workspace=$1
protocol=$2
host=$3
index=$4
username=$5
password=$6
buildTime=$7


temp_file_in="${workspace}/dx-pages-portlets-perf-tests/pnp_performance_results.temp.log"
file_in="${workspace}/dx-pages-portlets-perf-tests/pnp_performance_results.log"

totalAverageResponseTime=""
totalThroughput=""
anon_page1_resp_time=""
anon_page3_resp_time=""
anon_page2_resp_time=""
auth_page6_resp_time=""
anon_page8_resp_time=""
auth_page7_resp_time=""
auth_page8_resp_time=""
anon_page5_resp_time=""
auth_page2_resp_time=""
anon_page4_resp_time=""
auth_page3_resp_time=""
anon_page7_resp_time=""
auth_page4_resp_time=""
anon_page6_resp_time=""
auth_page5_resp_time=""
auth_page1_resp_time=""
buildName=""
opensearchIndexUrl=""
opensearchIndexData={}

# prometeus results to dashboard
prometheus_cpu_result=$(cat ${workspace}/dx-pages-portlets-perf-tests/prometheus_cpu_results.json)
echo "prometheus_result:- $prometheus_cpu_result"

prometheus_memory_result=$(cat ${workspace}/dx-pages-portlets-perf-tests/prometheus_memory_results.json)
echo "prometheus_result:- $prometheus_memory_result"
 
# Extract JSON data after colon using sed
cpu_values=$(echo "$prometheus_cpu_result" | sed 's/.*"cpuResultsInfo": //')
 
# Print the result
echo "cpuResultsInfo: $cpu_values"
 
# Extract JSON data after colon using sed
memory_values=$(echo "$prometheus_memory_result" | sed 's/.*"memoryResultsInfo": //')
 
# Print the result
echo "memoryResultsInfo: $memory_values"
# jtl error detials
jtl_error_json=$(cat ${workspace}/dx-pages-portlets-perf-tests/jtl_errors.json)
echo "jtl_error_json:- $jtl_error_json"

# get the value for key jtlErrorInfo from json
# Extract the value between the square brackets
jtlErrorInfo=$(echo "$jtl_error_json" | sed -n 's/.*"jtlErrorInfo": \[\([^]]*\)\].*/\1/p')
echo "jtlErrorInfo: $jtlErrorInfo"

# combining logs in one line
awk 'NR%2{printf "%s ",$0;next;}1' $file_in >> $temp_file_in

# || [[ -n $line ]] prevents the last line from being ignored if it doesn't end with a \n
# since read returns a non-zero exit code when it encounters EOF
resultArray=()
while IFS= read -r line || [[ -n "$line" ]]; do
    echo "Text read from file: $line"
     # split the line delimited by comma and result is stored in array
    IFS=$',' resultArray=($line)
    unset IFS
done < $temp_file_in
unset IFS

# Loop through result array to filter for uploadTime, Dam operations time, fetch binary response time for image, document and video
for i in ${!resultArray[@]}
do
    case $i in
        0)
            totalAverageResponseTime=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "totalAverageResponseTime - $totalAverageResponseTime"
            ;;
        1)
            totalThroughput=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "totalThroughput - $totalThroughput"
            ;;
        2)
            anon_page1_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page1_resp_time - $anon_page1_resp_time"
            ;;
        3)
            anon_page3_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page3_resp_time - $anon_page3_resp_time"
            ;;
        4)
            anon_page2_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page2_resp_time - $anon_page2_resp_time"
            ;;
        5)
            auth_page6_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page6_resp_time - $auth_page6_resp_time"
            ;;
        6)
            anon_page8_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page8_resp_time - $anon_page8_resp_time"
            ;;
        7)
            auth_page7_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page7_resp_time - $auth_page6_resp_time"
            ;;
        8)
            auth_page8_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page8_resp_time - $auth_page8_resp_time"
            ;;
        9)
            anon_page5_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page5_resp_time - $anon_page5_resp_time"
            ;;
        10)
            auth_page2_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page2_resp_time - $auth_page2_resp_time"
            ;;
        11)
            anon_page4_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page4_resp_time - $anon_page4_resp_time"
            ;;
        12)
            auth_page3_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page3_resp_time - $auth_page3_resp_time"
            ;;
        13)
            anon_page7_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page7_resp_time - $anon_page7_resp_time"
            ;;
        14)
            auth_page4_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page4_resp_time - $auth_page4_resp_time"
            ;;
        15)
            anon_page6_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anon_page6_resp_time - $anon_page6_resp_time"
            ;;
        16)
            auth_page5_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page5_resp_time - $auth_page5_resp_time"
            ;;
        17)
            auth_page1_resp_time=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "auth_page1_resp_time - $auth_page1_resp_time"
            ;;
        18)
            buildName=${resultArray[$i]}
            echo "buildName - $buildName"
            ;;
    esac
done

# remove temp file
rm $temp_file_in

# DAM regression test build time
echo "buildTime in script:- $buildTime"

opensearchIndexUrl="${protocol}://${host}/${index}/_doc"
echo "opensearchIndexUrl:- ${opensearchIndexUrl}"

# multi-line json for indexing
opensearchIndexData="{
\"totalAverageResponseTime\": ${totalAverageResponseTime},
\"totalThroughput\": ${totalThroughput},
\"buildName\": \"${buildName}\",
\"buildTime\": \"${buildTime}\",
\"anon_page1_resp_time\": ${anon_page1_resp_time},
\"anon_page2_resp_time\": ${anon_page2_resp_time},
\"anon_page3_resp_time\": ${anon_page3_resp_time},
\"anon_page4_resp_time\": ${anon_page4_resp_time},
\"anon_page5_resp_time\": ${anon_page5_resp_time},
\"anon_page6_resp_time\": ${anon_page6_resp_time},
\"anon_page7_resp_time\": ${anon_page7_resp_time},
\"anon_page8_resp_time\": ${anon_page8_resp_time},
\"auth_page1_resp_time\": ${auth_page1_resp_time},
\"auth_page2_resp_time\": ${auth_page2_resp_time},
\"auth_page3_resp_time\": ${auth_page3_resp_time},
\"auth_page4_resp_time\": ${auth_page4_resp_time},
\"auth_page5_resp_time\": ${auth_page5_resp_time},
\"auth_page6_resp_time\": ${auth_page6_resp_time},
\"auth_page7_resp_time\": ${auth_page7_resp_time},
\"auth_page8_resp_time\": ${auth_page8_resp_time},
\"cpuUsage\":  $cpu_values,
\"memoryUsage\": $memory_values,
\"jtl_errors\": [${jtlErrorInfo}]
}"

# opensearchIndexData json
echo "${opensearchIndexData}"

# post dam perf results to opensearch dashboard
curl -XPOST "${opensearchIndexUrl}" -H 'Content-Type: application/json' -d "${opensearchIndexData}" -u "${username}:${password}" -k

echo "Pages and Portlets results posted to OpenSearch Dashboard successfully !!"
