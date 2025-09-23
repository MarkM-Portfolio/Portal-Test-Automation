#!/bin/bash
# perform readiness probe
NAMESPACE=$1
RETRIES_COUNT=$2
if [ -z "$NAMESPACE" ]
then
  echo "Invalid namespace"
else
  echo "Performing check for $NAMESPACE deletion."
  if [ -z "$RETRIES_COUNT" ]
  then
    RETRIES_COUNT=60
  fi
  while [[ $(kubectl get ns $NAMESPACE | grep -v NAME -wc ) -gt 0 ]]
  do
  echo "$NAMESPACE is in $(kubectl get ns $NAMESPACE -o=jsonpath='{.status.phase}') state. Waiting another 30s, Retry another $RETRIES_COUNT time(s)"
  if [[ $RETRIES_COUNT -eq 0 ]]; then
    echo "Failed to delete the namespace($NAMESPACE), due to some resources still alive.";
    exit 1;
  fi
  RETRIES_COUNT=$((RETRIES_COUNT - 1))
  sleep 30s
  done
  echo "Namespace($NAMESPACE) is successfully terminated."
fi
