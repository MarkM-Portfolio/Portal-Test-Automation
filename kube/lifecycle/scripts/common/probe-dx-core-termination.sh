#!/bin/bash
# Perform probe for pod termination.
POD_NAME=$1
NAMESPACE=$2
PROBE_RETRIES=$3
if [ -z "$POD_NAME" ]
then
  echo "no probe defined - let's hope the best"
else
  echo "Checking if pod $POD_NAME is restarted"
  if [ -z "$PROBE_RETRIES" ]
  then
    PROBE_RETRIES=15
  fi
  # While:
  #     - the pod has containers that are still ready
  while [[ $(kubectl get pods -n $NAMESPACE $POD_NAME -o 'jsonpath={.status.containerStatuses[?(@.ready==true)]}') ]]
  do
  echo "$POD_NAME not terminated yet in $NAMESPACE, STATUS: $(kubectl get pods -n $NAMESPACE | grep $POD_NAME | awk '{print $3}' ) -- READY : $(kubectl get pods -n $NAMESPACE | grep $POD_NAME | awk '{print $2}') , waiting another 10s, Retry another $PROBE_RETRIES time(s)"
  if [[ $PROBE_RETRIES -eq 0 ]]; then
    echo "$POD_NAME still not terminated in $NAMESPACE- terminating the script";
    exit 1;
  fi
  PROBE_RETRIES=$((PROBE_RETRIES - 1))
  sleep 10s
  done
  echo "$POD_NAME has restarted in $NAMESPACE"
fi
