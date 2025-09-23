# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

# Read the timestamp from jtl file and generate the difference

JTL_FILE_NAME=$1
ENVIRONMENT=$2

if [ -z "$JTL_FILE_NAME" ]; 
then
    echo "jtl file $JTL_FILE_NAME is not provided";
    exit 1
else
    if [[ $JTL_FILE_NAME == "test_log_dam_regression"*  ]];
    then
    if [ "$ENVIRONMENT" = "openshiftnjdc" ]
    then
    startTime_ms=$(head -n2  /home/dam_jmeter_user/$JTL_FILE_NAME | tail -n1 | cut -d',' -f1)
    endTime_ms=$(tail -n1  /home/dam_jmeter_user/$JTL_FILE_NAME | cut -d',' -f1)
    else
    startTime_ms=$(head -n2 native-kube/$JTL_FILE_NAME | tail -n1 | cut -d',' -f1)
    endTime_ms=$(tail -n1 native-kube/$JTL_FILE_NAME | cut -d',' -f1)
    fi
    startTime=$(date -d @$((($startTime_ms+500)/1000)))
    endTime=$(date -d @$((($endTime_ms+500)/1000)))

    startTimeInSeconds=$(date -d "$startTime" +%s)
    endTimeInSeconds=$(date -d "$endTime" +%s)

    diff=$(( $endTimeInSeconds - $startTimeInSeconds ))
    echo "time in ms  ${startTime_ms}  and end time in ms is ${endTime_ms}"
    echo "start time is : ${startTime} and end time is ${endTime} "
    echo "Upload time is : $(( $diff % 3600 )) seconds"

    if [ "$ENVIRONMENT" = "openshiftnjdc" ]
    then
     printf "$(( $diff % 3600 )) seconds," >> /home/dam_jmeter_user/dam_performance_results_upload_asset.log
    else
     printf "$(( $diff % 3600 )) ," >> /home/centos/native-kube/dam_performance_results.log
  fi  
    fi
fi