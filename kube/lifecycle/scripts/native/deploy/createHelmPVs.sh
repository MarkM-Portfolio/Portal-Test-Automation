#!/bin/sh

cd ~/native-kube

# Create multiple PVs of one label to account for scaling
MULTIPLIER=5

# All lowercase due to restrictions for Kubernetes labels
declare -a PV_RWX_LIST=(
  "core-profile"
  "dam-binaries"
)

declare -a PV_RWO_LIST=(
  "core-tranlog"
  "core-log"
  "licensemanager-data"
  "openldap-sldap"
  "openldap-ldap"
  "openldap-certificate"
  "persistencenode-database"
  "remotesearch-prsprofile"
  "db2-database"
)

for PV_LIST_ENTRY in "${PV_RWX_LIST[@]}"
do
  # Create multiple PVs of one label to account for scaling
  for ((n=1;n<=$(($MULTIPLIER));n++)); do
    DIRECTORY=$(pwd)/volumes/rwxvol-${PV_LIST_ENTRY}-${n}
    mkdir -p "${DIRECTORY}"
    cp artifacts/createLabelledRWXKubePV.yaml createLabelledRWXKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'PV_NAME'rwxvol-${PV_LIST_ENTRY}-${n}'g" createLabelledRWXKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'WORKING_DIRECTORY'${DIRECTORY}'g" createLabelledRWXKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'ADDITIONAL_LABEL'attachTo: rwxvol-${PV_LIST_ENTRY}'g" createLabelledRWXKubePV_${NAMESPACE}.yaml
    kubectl create -f createLabelledRWXKubePV_${NAMESPACE}.yaml
    # Change ownership of PVs directory to avoid permission issues in NJDC
    if [[ "${IS_NJDC_DEPLOYMENT}" == "true" ]]
    then
        sudo chown 1000:1000 "${DIRECTORY}"
    fi
  done
done

for PV_LIST_ENTRY in "${PV_RWO_LIST[@]}"
do
  # Create multiple PVs of one label to account for scaling
  for ((n=1;n<=$(($MULTIPLIER));n++)); do
    DIRECTORY=$(pwd)/volumes/rwovol-${PV_LIST_ENTRY}-${n}
    mkdir -p "${DIRECTORY}"
    cp artifacts/createLabelledRWOKubePV.yaml createLabelledRWOKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'STORAGE_CLASS'manual'g" createLabelledRWOKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'PV_NAME'rwovol-${PV_LIST_ENTRY}-${n}'g" createLabelledRWOKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'WORKING_DIRECTORY'${DIRECTORY}'g" createLabelledRWOKubePV_${NAMESPACE}.yaml
    sed -i.bck "s'ADDITIONAL_LABEL'attachTo: rwovol-${PV_LIST_ENTRY}'g" createLabelledRWOKubePV_${NAMESPACE}.yaml
    kubectl create -f createLabelledRWOKubePV_${NAMESPACE}.yaml
    # Change ownership of PVs directory to avoid permission issues in NJDC
    if [[ "${IS_NJDC_DEPLOYMENT}" == "true" ]]
    then
        sudo chown 1000:1000 "${DIRECTORY}"
    fi
  done
done