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

echo 'Overlaying updated jars from wcm.impl'
cd /opt/zips/wcm.impl/wp/code/prereq.wcm/build/wcm/shared/app
find . -name '*.jar' | xargs -i cp -p '{}' /opt/IBM/WebSphere/PortalServer/wcm/prereq.wcm/wcm/shared/app/

echo 'Restarting the portal server ...'
cd /opt/IBM/WebSphere/wp_profile/bin
./stopServer.sh WebSphere_Portal -username wpsadmin -password wpsadmin
./startServer.sh WebSphere_Portal

echo 'Portal server restarted'
