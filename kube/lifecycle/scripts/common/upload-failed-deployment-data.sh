#!/bin/bash

# Upload collected 'native' environment data to S3 bucket for analysis before destroying the environment.

FILESEP="_"

if [ -f ~/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip ]; then
    mkdir -p ./aws
    cd ./aws
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip -q awscliv2.zip
    sudo ./aws/install
    aws s3 cp "/home/centos/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip" s3://dx-failed-kube-deployment-logs

    if [ $? == 0 ]; then
        echo "Kubernetes environment data uploaded."
    else
        echo "Unable to upload Kubernetes environment data: $?"
    fi
else
    echo "~/data/${JOB_BASE_NAME}${FILESEP}${BUILD_ID}${FILESEP}${KUBE_FLAVOUR}.zip not found."
    #exit 1
fi