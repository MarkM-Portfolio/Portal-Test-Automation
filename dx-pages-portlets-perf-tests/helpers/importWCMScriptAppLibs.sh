# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

cd /home/centos/
unzip WCMLib.zip
unzip ScriptAppLibraryFinalTest.zip
kubectl cp WCMLib dx-deployment-core-0:/opt/HCL/wp_profile/ConfigEngine -n dxns
kubectl cp ScriptAppLibraryFinalTest dx-deployment-core-0:/opt/HCL/wp_profile/ConfigEngine -n dxns
kubectl exec -n dxns pod/dx-deployment-core-0 -- bash -c "
cd /opt/HCL/wp_profile/ConfigEngine;
./ConfigEngine.sh import-wcm-data -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dimport.directory='/opt/HCL/wp_profile/ConfigEngine/WCMLib';
./ConfigEngine.sh import-wcm-data -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -Dimport.directory='/opt/HCL/wp_profile/ConfigEngine/ScriptAppLibraryFinalTest';"