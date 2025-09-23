# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

#!/bin/bash

SERVER_HOST=$1
NAMESPACE=$2
# copy files to required directory
oc project $NAMESPACE
oc cp ./XMLAccess.sh dx-deployment-core-0:/opt/HCL/wp_profile
oc cp ./pageExport3.xml dx-deployment-core-0:/opt/HCL/wp_profile

# navigate to folder which has all the test reports and run XMLAccess script
oc exec -it dx-deployment-core-0 -- bash -c "cd /home/dx_user/; mkdir XMLAccess; cd /opt/HCL/wp_profile/; sh XMLAccess.sh pageExport3.xml '' $SERVER_HOST"
