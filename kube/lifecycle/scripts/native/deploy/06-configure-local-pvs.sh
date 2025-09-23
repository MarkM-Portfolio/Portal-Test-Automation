#!/bin/bash
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Create local kube PVs
sh ~/native-kube/createHelmPVs.sh
sh ~/native-kube/leap/createLeapHelmPVs.sh
sh ~/native-kube/people/createPeopleHelmPVs.sh