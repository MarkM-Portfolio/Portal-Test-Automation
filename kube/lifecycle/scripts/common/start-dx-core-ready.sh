#!/bin/bash
# perform readiness probe
POD_NAME=$1
NAMESPACE=$2
PROBE_RETRIES=$3
VERBOSE_MODE=$4
if [ -z "$POD_NAME" ]
then
  echo "no readiness probe defined - let's hope the best"
else
  echo "performing readiness probe $POD_NAME"
  if [ -z "$PROBE_RETRIES" ]
  then
    PROBE_RETRIES=200
  fi
  MAX_CONSECUTIVE_PENDING=10
  MAX_CONSECUTIVE_CONTAINERCREATE=10
  MAX_CONSECUTIVE_CRASHLOOPBACKOFF=10
  ERROR_EXIT=0
  ERROR_STATUS=""
  # While:
  #     - the pod does not exist 
  #   OR
  #     - no containers are created yet
  #   OR
  #     - the pod has containers that are not in ready status
  #
  # This loop will run until PROBE_RETRIES has count down to 0. Each loop runs about 1 minute.
  # Additional exit are coded to reduce wait time since we assume that kinda status won't recover.
  #  - Pending : If the pod keeps in this status for MAX_CONSECUTIVE_PENDING loops -> EXIT
  #  - ContainerCreating : If the pod keeps in this status for MAX_CONSECUTIVE_CONTAINERCREATE loops -> EXIT
  #  - CrashLoopBackOff : If the pod keeps in this status for MAX_CONSECUTIVE_CONTAINERCREATE loops -> EXIT 
  while [[ ! $(kubectl get pods -n $NAMESPACE $POD_NAME) || ! $(kubectl get pods -n $NAMESPACE $POD_NAME -o 'jsonpath={.status.containerStatuses[*]}') || $(kubectl get pods -n $NAMESPACE $POD_NAME -o 'jsonpath={.status.containerStatuses[?(@.ready!=true)]}') ]]
  do
    POD_STAT=$(kubectl get pods -n $NAMESPACE | grep $POD_NAME | awk '{print $3}' )
    echo "$POD_NAME not running yet in $NAMESPACE, STATUS: $POD_STAT -- READY : $(kubectl get pods -n $NAMESPACE | grep $POD_NAME | awk '{print $2}') , waiting another 1m, Retry another $PROBE_RETRIES time(s)"
    if [ "$POD_STAT" == "Pending" ]; then
       echo "pod events from $POD_NAME"
       echo "---------------------------------"
       kubectl describe pod $POD_NAME -n $NAMESPACE | grep -A20 Events
       echo "---------------------------------"
       MAX_CONSECUTIVE_PENDING=$((MAX_CONSECUTIVE_PENDING - 1))
       if [[ $MAX_CONSECUTIVE_PENDING -eq 0 ]]; then
          ERROR_EXIT=1
          ERROR_STATUS="Pending"
       fi
    else
       MAX_CONSECUTIVE_PENDING=10
    fi
    if [ "$POD_STAT" == "ContainerCreating" ]; then
       echo "pod events from $POD_NAME"
       echo "---------------------------------"
       kubectl describe pod $POD_NAME -n $NAMESPACE | grep -A20 Events
       echo "---------------------------------"
       MAX_CONSECUTIVE_CONTAINERCREATE=$((MAX_CONSECUTIVE_CONTAINERCREATE - 1))
       if [[ $MAX_CONSECUTIVE_CONTAINERCREATE -eq 0 ]]; then
          ERROR_EXIT=1
          ERROR_STATUS="ContainerCreating"
       fi
    else
       MAX_CONSECUTIVE_CONTAINERCREATE=10
    fi
    if [ "$POD_STAT" == "CrashLoopBackOff" ]; then
       echo "pod events from $POD_NAME"
       echo "---------------------------------"
       kubectl describe pod $POD_NAME -n $NAMESPACE | grep -A20 Events
       echo "---------------------------------"
       MAX_CONSECUTIVE_CRASHLOOPBACKOFF=$((MAX_CONSECUTIVE_CRASHLOOPBACKOFF - 1))
       # For CrashLoopBackOff there's no refresh of loop counter
       if [[ $MAX_CONSECUTIVE_CRASHLOOPBACKOFF -eq 0 ]]; then
          echo "stdout from $POD_NAME"
          echo "---------------------------------"
          kubectl logs --since=10m $POD_NAME -n $NAMESPACE --all-containers
          echo "---------------------------------"
          ERROR_EXIT=1
          ERROR_STATUS="CrashLoopBackOff"
       fi
    fi
    if [ "$VERBOSE_MODE" != "" ]; then
       if [ "$POD_STAT" == "Running" ]; then
          echo "stdout from $POD_NAME"
          echo "---------------------------------"
          kubectl logs --since=1m $POD_NAME -n $NAMESPACE --all-containers
          echo "---------------------------------"
       fi
    fi
    if [ "$ERROR_EXIT" == "1" ]; then
      echo "$POD_NAME still in $ERROR_STATUS state in $NAMESPACE - terminating the script";
      echo "pods in cluster (all namespaces)"
      echo "---------------------------------"
      kubectl get pods --all-namespaces
      echo "---------------------------------"
      exit 1;
    fi
    if [[ $PROBE_RETRIES -eq 0 ]]; then
      echo "$POD_NAME still not running in $NAMESPACE - terminating the script";
      exit 1;
    fi
    PROBE_RETRIES=$((PROBE_RETRIES - 1))
    sleep 60s
  done
  echo "$POD_NAME is running in $NAMESPACE"
  if [ -f $WORKSPACE/success.properties ]; then
    echo "Removing leftover success.properties file."
    rm -fv $WORKSPACE/success.properties
  fi
  touch $WORKSPACE/success.properties
  echo DX_IS_RUNNING=true > $WORKSPACE/success.properties
fi