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
 

# Install acmesh for using Let's Encrypt CA certificate
# This will install acmesh in the user's home directory ~/.acme.sh and removes the automatically created cronjob
curl https://get.acme.sh | sh
$HOME/.acme.sh/acme.sh --uninstall-cronjob
