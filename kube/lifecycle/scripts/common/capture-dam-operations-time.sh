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
# Capturing approximate DAM operations time for uploaded assets
NAMESPACE=$1
ENVIRONMENT=$2
OPERATIONS_EMPTY_CHECK_RETRIES=$3


  # Check the namespace
  if [ -z "$NAMESPACE" ]
  then
    echo "Namespace not provided default set to dxns"
    NAMESPACE="dxns"
  fi

   # Validate the namespace
  if [ $(kubectl get ns $NAMESPACE | grep -v NAME -wc ) -gt 0 ]
  then
    echo "Namespace found: $NAMESPACE"
  else
    echo "Invalid namespace"
    exit 1
  fi

  # Check the number of retries based on kube flavour
  if [ -z "$OPERATIONS_EMPTY_CHECK_RETRIES" ]
  then
    echo "OPERATIONS_EMPTY_CHECK_RETRIES not provided default set to 200 retries with sleep for 2 minutes"
    echo "Kube flavour is - $ENVIRONMENT"
    if [ "$ENVIRONMENT" = "openshiftnjdc" ]
      then
        OPERATIONS_EMPTY_CHECK_RETRIES=45
    else
        OPERATIONS_EMPTY_CHECK_RETRIES=150
    fi
  fi
  echo "OPERATIONS_EMPTY_CHECK_RETRIES - $OPERATIONS_EMPTY_CHECK_RETRIES"

  # Print current app schema name
  APP_SCHEMA_NAME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' | sed -n '3p' ) 
  echo "APP_SCHEMA_NAME - $APP_SCHEMA_NAME"

  # Operations framework start time from min update of media_storage table
  OPERATIONS_FRAMEWORK_START_TIME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "SELECT min(updated) FROM $APP_SCHEMA_NAME.media_storage;" | awk '{print $1 " " $2}' | sed -n '3p')
  echo "OPERATIONS_FRAMEWORK_START_TIME - $OPERATIONS_FRAMEWORK_START_TIME"

  # While:
  #     - loops until the row count in operation table is empty
  while [[ ($(kubectl exec -n $NAMESPACE  pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "SELECT EXISTS(SELECT 1 from $APP_SCHEMA_NAME.operation);" | awk '{print $1}' |sed -n '3p') != "f") ]]
  do
    if [[ $OPERATIONS_EMPTY_CHECK_RETRIES -eq 0 ]]; then
      echo "Count of failed operations failed - $(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "SELECT count(*) FROM $APP_SCHEMA_NAME.operation;" | awk '{print $1}' |sed -n '3p')"
      echo "Operations failed - $(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "SELECT * FROM $APP_SCHEMA_NAME.operation;")"
      exit 1;
    fi
    OPERATIONS_EMPTY_CHECK_RETRIES=$((OPERATIONS_EMPTY_CHECK_RETRIES - 1))
    echo "Still there are pending operations - retry count: $OPERATIONS_EMPTY_CHECK_RETRIES";
    sleep 2m
  done

  # Operations table is empty now
  echo "operations table is empty now - all operations are completed"

  # Operations framework end time from max update of media_storage table
  OPERATIONS_FRAMEWORK_END_TIME=$(kubectl exec -n $NAMESPACE pod/dx-deployment-persistence-node-0 -- psql -d dxmediadb -c "SELECT max(updated) FROM $APP_SCHEMA_NAME.media_storage;" | awk '{print $1 " " $2}' | sed -n '3p')
  echo "OPERATIONS_FRAMEWORK_END_TIME - $OPERATIONS_FRAMEWORK_END_TIME"

  # Convert string to date format in seconds
  START_TIME=$(date -d "$OPERATIONS_FRAMEWORK_START_TIME" +%s)
  END_TIME=$(date -d "$OPERATIONS_FRAMEWORK_END_TIME" +%s)

  # Calcluate difference in dates
  DIFFERENCE_TIME=$((END_TIME - START_TIME))
  echo "DIFFERENCE_TIME" $DIFFERENCE_TIME
  OPERATIONS_TIME=0;

  if [ "$DIFFERENCE_TIME" -lt 3600 ]
  then
      OPERATIONS_TIME=$(($DIFFERENCE_TIME % 3600))
  else
      OPERATIONS_TIME=$DIFFERENCE_TIME
  fi 


  echo "Time taken for DAM operations - $(($OPERATIONS_TIME)) seconds."
  # setting log paths as per the env
  if [ "$ENVIRONMENT" = "openshiftnjdc" ]
    then
     printf "$(($OPERATIONS_TIME))  seconds," >> ~/dam_performance_results_operation_time.log
    else
     printf "$(($OPERATIONS_TIME)) ," >> /home/centos/native-kube/dam_performance_results.log
  fi