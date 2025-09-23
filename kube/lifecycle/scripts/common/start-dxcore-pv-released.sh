#!/bin/bash
# perform PV Release Check
PV=$1
if [ -z "$PV" ]
then
  echo "no readiness probe defined - let's hope the best"
else
  echo "performing PV $PV Release Check."
  if [ -z "$PROBE_RETRIES" ]
  then
    PROBE_RETRIES=50
  fi
  echo "Persistent Volume $PV Status: $(kubectl get pv $PV  |grep $PV | awk '{print $5}' ) "
  while [[ $(kubectl get pv $PV  |grep $PV | awk '{print $5}' ) != "Released" ]]
  do
  echo "$PV not Released yet, waiting another 5s, Retry another $PROBE_RETRIES time(s) for persistent volume $PV to Release.."
  if [[ $PROBE_RETRIES -eq 0 ]]; then
    echo "$PV still not Released - terminating the script";
    exit 1;
  fi
  PROBE_RETRIES=$((PROBE_RETRIES - 1))
  sleep 5s
  done
  echo "$PV is Released"
fi
