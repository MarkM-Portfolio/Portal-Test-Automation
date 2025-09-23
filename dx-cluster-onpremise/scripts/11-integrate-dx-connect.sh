#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

echo "Running DX connect config task install-dxconnect-application"
DX_PASSWORD=wpsadmin
/opt/IBM/WebSphere/wp_profile/ConfigEngine/ConfigEngine.sh install-dxconnect-application -DWasPassword=$DX_PASSWORD
/opt/IBM/WebSphere/AppServer/profiles/cw_profile/bin/startServer.sh server1
