#!/bin/bash
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# perform readiness probe
NAMESPACE=$1
PROBE_RETRIES=$2
if [ -z "$NAMESPACE" ]
then
  echo "no readiness probe defined - let's hope the best"
else
  echo "performing readiness probe for all pods in $NAMESPACE."
  if [ -z "$PROBE_RETRIES" ]
  then
    PROBE_RETRIES=90
  fi
  IS_POD_TERMINATING=$(kubectl get pods -n $NAMESPACE | grep "Terminating" | wc -l)
  # While:
  #     - any pod in the namespace has containers that are not in ready status
  #   OR
  #     - no containers are created yet
  while [[ ! $(kubectl get pods -n $NAMESPACE -o 'jsonpath={.items[*].status.containerStatuses[*]}') || $(kubectl get pods -n $NAMESPACE -o 'jsonpath={.items[*].status.containerStatuses[?(@.ready!=true)]}') || $IS_POD_TERMINATING != "0" ]]
  do
  IS_POD_TERMINATING=$(kubectl get pods -n $NAMESPACE | grep "Terminating" | wc -l)
  if [[ $IS_POD_TERMINATING == "0" ]]; then
    echo -e "Some containers are not running yet in $NAMESPACE:\n$(kubectl get pods -n $NAMESPACE -o 'jsonpath={range .items[*].status.containerStatuses[?(@.ready!=true)]}{.name}{"\n"}{end}')\nwaiting another 20s, Retry another $PROBE_RETRIES time(s)"
  fi
  if [[ $PROBE_RETRIES -eq 0 ]]; then
    echo "pods $(kubectl get pods -n $NAMESPACE -o 'jsonpath={range .items[*].status.containerStatuses[?(@.ready!=true)]}{.name}{",\n"}{end}') still not running in $NAMESPACE- terminating the script";
    exit 1;
  fi
  PROBE_RETRIES=$((PROBE_RETRIES - 1))
  sleep 20s
  done
  echo "readiness probe check in $NAMESPACE for all the pods is completed successfully."
fi
