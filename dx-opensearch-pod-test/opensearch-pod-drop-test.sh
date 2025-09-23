#!/bin/bash

# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication, or disclosure restricted by GSA ADP Schedule   *
# ********************************************************************

POD_NAME="$1"
NAMESPACE="$2"

# Change directory to /home/centos/
cd /home/centos/

kubectl get all -n $NAMESPACE

if [ "$POD_NAME" == "search-middleware-query" ]; then
    POD_NAME=$(kubectl get pods -n $NAMESPACE | grep 'dx-search-search-middleware-query' | awk '{print $1}' )
fi

echo "POD_NAME => $POD_NAME"

kubectl delete pod $POD_NAME -n $NAMESPACE

kubectl get all -n $NAMESPACE

