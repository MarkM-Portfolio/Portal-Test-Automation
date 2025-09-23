#!/bin/bash

# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication, or disclosure restricted by GSA ADP Schedule   *
# ********************************************************************

# Change directory to /home/centos/
cd /home/centos/
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "mkdir /opt/HCL/wp_profile/perftest"
kubectl -n dxns cp PerfTestLibs.zip dx-deployment-core-0:/opt/HCL/wp_profile/perftest
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "
cd /opt/HCL/wp_profile/perftest; 
unzip -q PerfTestLibs.zip -d PerfTestLibs;
"
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "
  cd /opt/HCL/wp_profile/ConfigEngine;
  ./ConfigEngine.sh import-wcm-data -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dimport.directory='/opt/HCL/wp_profile/perftest/PerfTestLibs'; > /dev/null 2>&1 &
  "