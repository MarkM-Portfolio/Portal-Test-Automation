#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# define response and configuration files that are being populated with necessary data

INSTALLBASE=/tmp/im
WAS_ADMIN=wpsadmin
WAS_PASSWORD=wpsadmin
DX_ADMIN=wpsadmin
DX_PASSWORD=wpsadmin

/opt/HCL/wp_profile/bin/./startServer.sh WebSphere_Portal
echo "Portal Installation completed and Running successfully."
