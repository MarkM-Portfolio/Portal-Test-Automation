#!/bin/bash

FILESEP="_"

# list of kubernetes commands/resources to gather data
# cmds=("node deployment statefulset pod secret service route mapping configmap event pvc")
cmds=("node deployment statefulset pod secret service route mapping configmap pvc")
dxpodname=""

KUBE_CMD="kubectl"

if [[ ("$($KUBE_CMD get all)" == *"NAME"* )  ]]
then
    ns=$($KUBE_CMD get ns | grep ${NAMESPACE})

    if [[ ! -z "$ns" ]]
    then
        echo "Collecting information for Kubernetes resources."

        # create and work in output directory
        mkdir -p $WORKSPACE/data
        cd $WORKSPACE/data

        # collect the Kubernetes environment data
        dx_crd=$($KUBE_CMD get crds | grep dxdeployment | awk '{print $1}')
        ml_crd=$($KUBE_CMD get crds | grep digitalasset | awk '{print $1}')
        pvlist=$($KUBE_CMD get pv | grep -w ${NAMESPACE} | awk '{print $1}')

        $KUBE_CMD describe crd $dx_crd > dx_crd.yaml
        $KUBE_CMD describe crd $ml_crd > ml_crd.yaml

        for pv in $pvlist
        do
            $KUBE_CMD describe pv ${pv} > ${pv}.yaml
        done

        for cmd in $cmds
        do
            mkdir ./$cmd
            resources=$($KUBE_CMD get $cmd -n ${NAMESPACE} | awk '{print $1}' | sed 's/NAME//g')
            for resource in $resources
            do
                echo "Collecting information for $cmd $resource."
                $KUBE_CMD describe $cmd $resource -n ${NAMESPACE} > ./$cmd/$resource.yaml
                if [[ $cmd == "pod" ]]
                then
                    $KUBE_CMD logs $resource -n ${NAMESPACE} > ./$cmd/$resource.log.out
                    if [[ "$resource" == "dx-deployment-core-0" ]] || [[ "$resource" == "dx-deployment-0" ]]
                        then dxpodname=$resource
                        echo DX podname :: $dxpodname
                    fi
                fi
            done

        done

        # collect the DX logs from the PV
        mkdir -p ./dxlogs/server
        mkdir -p ./dxlogs/configengine
        $KUBE_CMD cp -n $NAMESPACE $dxpodname:/opt/HCL/logs/ ./dxlogs/server/
        $KUBE_CMD cp -n $NAMESPACE $dxpodname:/opt/HCL/wp_profile/ConfigEngine/log/ ./dxlogs/configengine/
        #zip -r dx.logs.zip . -i ./dxlogs/*
        #rm -rf ./dxlogs

    # grab the Jenkins environment variables
    env > jenkins_env.out

    # compress the data
    zip -r $JOB_BASE_NAME$FILESEP$BUILD_ID$FILESEP$KUBE_FLAVOUR.zip *

    else
        echo "$NAMESPACE does not exist."
    fi

else
    echo "$KUBE_CMD command not found.  Unable to collect data. Exiting"
    exit
fi
