#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

docker_dx_core_image_id=$(docker ps -aqf "name=^k8s_core_dx-deployment-core-0")

docker exec -i ${docker_dx_core_image_id} /bin/sh -c "
cd /opt/HCL/wp_profile/ConfigEngine/;
./ConfigEngine.sh create-virtual-portal -DWasPassword=wpsadmin -DPortalAdminPwd=wpsadmin -DVirtualPortalTitle=Import-ArtifactsVP4 -DVirtualPortalContext=Import-ArtifactsVP4"
