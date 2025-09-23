#!/bin/bash
# perform readiness probe
NAMESPACE=$1
PROBE_RETRIES=$3
INTERRUPT_POD=$2
if [ -z "$NAMESPACE" ]
then
  echo "no readiness probe defined - let's hope the best"
else
  echo "performing readiness probe for DAM pods to be running state,once DAM pods is in running state start interrupting dam in $NAMESPACE."
  if [ -z "$PROBE_RETRIES" ]
  then
    PROBE_RETRIES=500
  fi
  # While:
  #     - the pod does not exist 
  #   OR
  #     - the pod has containers that are not in ready status
  #   OR
  #     - any of the queried conditions apply
  while [[(! $(kubectl get pods -n $NAMESPACE dx-deployment-persistence-0) || $(kubectl get pods -n $NAMESPACE dx-deployment-persistence-0 -o 'jsonpath={.status.containerStatuses[?(@.ready!=true)]}') )  || ( $(kubectl exec -n  $NAMESPACE  dx-deployment-persistence-0 -- psql -d dxmediadb -c 'SELECT count(1) FROM information_schema.schemata;' | awk '{print $1}' |sed -n '3p') -lt 7 ) || ($(kubectl exec -n $NAMESPACE  dx-deployment-persistence-0 -- psql -d dxmediadb -c 'SELECT schema_name FROM information_schema.schemata order by schema_name DESC LIMIT 1;' | awk '{print $1}' |sed -n '3p' |  xargs  -I _latest_schema kubectl exec -n $NAMESPACE  dx-deployment-persistence-0 -- psql -d dxmediadb -c 'SELECT status FROM _latest_schema.dbstatus;' | awk '{print $1}' |sed -n '3p') != "MIGRATING") ]]
  do
    if [[ $PROBE_RETRIES -eq 0 ]]; then
      echo "$(kubectl get pods -n $NAMESPACE  | grep $INTERRUPT_POD | awk '{print $3}' | grep -v "Running" | tr '\n' ',' ) still not able catch Migration state in $NAMESPACE- terminating the script";
      exit 1;
    fi
    PROBE_RETRIES=$((PROBE_RETRIES - 1))
    sleep 1s
  done
  echo "deployment-persistence is in $( kubectl exec -n $NAMESPACE dx-deployment-persistence-0 -- psql -d dxmediadb -c 'SELECT status FROM schema_1_1_0.dbstatus;' | awk '{print $1}' |sed -n '3p' ) state, lets try to interrupt the migration by deleting $INTERRUPT_POD pods."
  kubectl delete statefulset $INTERRUPT_POD -n $NAMESPACE
  echo "$INTERRUPT_POD in $NAMESPACE for interrupted successfully."
fi
