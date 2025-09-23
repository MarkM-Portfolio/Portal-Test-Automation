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
 

# Uninstall acmesh
# This will uninstall acmesh from the user's home directory ~/.acme.sh and removes the directory
$HOME/.acme.sh/acme.sh --uninstall
rm -fR $HOME/.acme.sh
