#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#OLD
#docker_dx_core_image_id=$(docker ps -aqf "name=^k8s_core_dx-deployment-core-0")

echo "AdminTask.retrieveSignerFromPort('[-host $1 -port 443 -keyStoreName NodeDefaultTrustStore -certificateAlias $2]')
AdminConfig.save()" > /tmp/retrieveSignerFromPort.py
kubectl cp /tmp/retrieveSignerFromPort.py -n dxns dx-deployment-core-0:/opt/HCL/
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "
cd /opt/HCL/wp_profile/bin;
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/HCL/retrieveSignerFromPort.py;"