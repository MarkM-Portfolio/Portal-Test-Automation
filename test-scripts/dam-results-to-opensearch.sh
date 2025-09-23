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

# DAM performance results from log file extracted and pushed to OpenSearch dashboard

workspace=$1
protocol=$2
host=$3
index=$4
username=$5
password=$6
buildTime=$7


temp_file_in="${workspace}/dx-dam-regression/dam_performance_results.temp.log"
file_in="${workspace}/dx-dam-regression/dam_performance_results.log"

uploadTime=""
buildName=""
operationsTime=""
anonymous_renderTime_tif_300kb_furl=""
anonymous_renderTime_jpg_155kb_id=""
anonymous_renderTime_pptx_2mb_furl=""
anonymous_renderTime_jpg_155kb_curl=""
anonymous_renderTime_mp4_1mb_curl=""
anonymous_renderTime_pdf_5mb_furl=""
anonymous_renderTime_mp4_100mb_furl=""
anonymous_renderTime_xlsx_250kb_id=""
anonymous_renderTime_png_500kb_id_desktop=""
anonymous_renderTime_jpg_5mb_furl_desktop=""
anonymous_renderTime_gif_2mb_furl=""
anonymous_renderTime_mp4_15mb_id=""
anonymous_renderTime_jpg_2mb_id_smartphone=""
anonymous_renderTime_pdf_171kb_id=""
anonymous_renderTime_mp4_1mb_id=""
anonymous_renderTime_docx_199kb_id=""
anonymous_renderTime_png_500kb_furl_tablet=""
anonymous_renderTime_pdf_171kb_curl=""
anonymous_renderTime_webm_2mb_furl=""
Total_ResponseTime=""
Total_Throughput=""
opensearchIndexUrl=""
opensearchIndexData={}

# prometeus results to dashboard
prometheus_cpu_result=$(cat ${workspace}/dx-dam-regression/prometheus_cpu_results.json)
echo "prometheus_result:- $prometheus_cpu_result"

prometheus_memory_result=$(cat ${workspace}/dx-dam-regression/prometheus_memory_results.json)
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
jtl_error_json=$(cat ${workspace}/dx-dam-regression/jtl_errors.json)
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
            uploadTime=${resultArray[$i]}
            echo "uploadTime - $uploadTime"
            ;;
        1)
            operationsTime=${resultArray[$i]}
            echo "operationsTime - $operationsTime"
            ;;
        2)
            buildName=${resultArray[$i]}
            echo "buildName - $buildName"
            ;;
        3)
            Total_ResponseTime=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "Total_ResponseTime - $Total_ResponseTime"
            ;;
        4)
            Total_Throughput=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "Total_Throughput - $Total_Throughput"
            ;;
        5)
            anonymous_renderTime_webm_2mb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_webm_2mb_furl - $anonymous_renderTime_webm_2mb_furl"
            ;;
        6)
            anonymous_renderTime_tif_300kb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_tif_300kb_furl - $anonymous_renderTime_tif_300kb_furl"
            ;;
        7)
            anonymous_renderTime_jpg_155kb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_jpg_155kb_id - $anonymous_renderTime_jpg_155kb_id"
            ;;
        8)
            anonymous_renderTime_pptx_2mb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_pptx_2mb_furl - $anonymous_renderTime_pptx_2mb_furl"
            ;;
        9)
            anonymous_renderTime_jpg_155kb_curl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_jpg_155kb_curl - $anonymous_renderTime_jpg_155kb_curl"
            ;;
        10)
            anonymous_renderTime_mp4_1mb_curl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_mp4_1mb_curl - $anonymous_renderTime_mp4_1mb_curl"
            ;;
        11)
            anonymous_renderTime_pdf_5mb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_pdf_5mb_furl - $anonymous_renderTime_pdf_5mb_furl"
            ;;
        12)
            anonymous_renderTime_mp4_100mb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_mp4_100mb_furl - $anonymous_renderTime_mp4_100mb_furl"
            ;;
        13)
            anonymous_renderTime_xlsx_250kb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_xlsx_250kb_id - $anonymous_renderTime_xlsx_250kb_id"
            ;;
        14)
            anonymous_renderTime_png_500kb_id_desktop=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_png_500kb_id_desktop - $anonymous_renderTime_png_500kb_id_desktop"
            ;;
        15)
            anonymous_renderTime_jpg_5mb_furl_desktop=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_jpg_5mb_furl_desktop - $anonymous_renderTime_jpg_5mb_furl_desktop"
            ;;
        16)
            anonymous_renderTime_gif_2mb_furl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_gif_2mb_furl - $anonymous_renderTime_gif_2mb_furl"
            ;;
        17)
            anonymous_renderTime_mp4_15mb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_mp4_15mb_id - $anonymous_renderTime_mp4_15mb_id"
            ;;
        18)
            anonymous_renderTime_jpg_2mb_id_smartphone=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_jpg_2mb_id_smartphone - $anonymous_renderTime_jpg_2mb_id_smartphone"
            ;;
        19)
            anonymous_renderTime_pdf_171kb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_pdf_171kb_id - $anonymous_renderTime_pdf_171kb_id"
            ;;
        20)
            anonymous_renderTime_mp4_1mb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_mp4_1mb_id - $anonymous_renderTime_mp4_1mb_id"
            ;;
        21)
            anonymous_renderTime_docx_199kb_id=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_docx_199kb_id - $anonymous_renderTime_docx_199kb_id"
            ;;
        22)
            anonymous_renderTime_png_500kb_furl_tablet=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_png_500kb_furl_tablet - $anonymous_renderTime_png_500kb_furl_tablet"
            ;;
        23)
            anonymous_renderTime_pdf_171kb_curl=$(echo ${resultArray[$i]} | tr -d ' ')
            echo "anonymous_renderTime_pdf_171kb_curl - $anonymous_renderTime_pdf_171kb_curl"
    
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
\"uploadTime\": ${uploadTime},
\"operationsTime\": ${operationsTime},
\"buildName\": \"${buildName}\",
\"buildTime\": \"${buildTime}\",
\"anonymous_renderTime_tif_300kb_furl\": ${anonymous_renderTime_tif_300kb_furl},
\"anonymous_renderTime_jpg_155kb_id\": ${anonymous_renderTime_jpg_155kb_id},
\"anonymous_renderTime_pptx_2mb_furl\": ${anonymous_renderTime_pptx_2mb_furl},
\"anonymous_renderTime_jpg_155kb_curl\": ${anonymous_renderTime_jpg_155kb_curl},
\"anonymous_renderTime_mp4_1mb_curl\": ${anonymous_renderTime_mp4_1mb_curl},
\"anonymous_renderTime_pdf_5mb_furl\": ${anonymous_renderTime_pdf_5mb_furl},
\"anonymous_renderTime_mp4_100mb_furl\": ${anonymous_renderTime_mp4_100mb_furl},
\"anonymous_renderTime_xlsx_250kb_id\": ${anonymous_renderTime_xlsx_250kb_id},
\"anonymous_renderTime_png_500kb_id_desktop\": ${anonymous_renderTime_png_500kb_id_desktop},
\"anonymous_renderTime_jpg_5mb_furl_desktop\": ${anonymous_renderTime_jpg_5mb_furl_desktop},
\"anonymous_renderTime_gif_2mb_furl\": ${anonymous_renderTime_gif_2mb_furl},
\"anonymous_renderTime_mp4_15mb_id\": ${anonymous_renderTime_mp4_15mb_id},
\"anonymous_renderTime_jpg_2mb_id_smartphone\": ${anonymous_renderTime_jpg_2mb_id_smartphone},
\"anonymous_renderTime_pdf_171kb_id\": ${anonymous_renderTime_pdf_171kb_id} ,
\"anonymous_renderTime_mp4_1mb_id\": ${anonymous_renderTime_mp4_1mb_id},
\"anonymous_renderTime_docx_199kb_id\": ${anonymous_renderTime_docx_199kb_id},
\"anonymous_renderTime_png_500kb_furl_tablet\": ${anonymous_renderTime_png_500kb_furl_tablet},
\"anonymous_renderTime_pdf_171kb_curl\": ${anonymous_renderTime_pdf_171kb_curl},
\"anonymous_renderTime_webm_2mb_furl\": ${anonymous_renderTime_webm_2mb_furl},
\"Total_ResponseTime\": ${Total_ResponseTime},
\"Total_Throughput\": ${Total_Throughput},
\"jtl_errors\": [${jtlErrorInfo}],
\"cpuUsage\":  $cpu_values,
\"memoryUsage\": $memory_values
}"

# opensearchIndexData json
echo "${opensearchIndexData}"

# post dam perf results to opensearch dashboard
curl -XPOST "${opensearchIndexUrl}" -H 'Content-Type: application/json' -d "${opensearchIndexData}" -u "${username}:${password}" -k

echo "DAM results posted to OpenSearch Dashboard successfully !!"