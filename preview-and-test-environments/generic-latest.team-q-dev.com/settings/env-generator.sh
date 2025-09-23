#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# Generate ENV sh for forwarding to latest machine

# Get all relevant environment variables
printenv | grep DX_CORE >> $1/env.sh
printenv | grep MEDIA_LIBRARY >> $1/env.sh
printenv | grep EXPERIENCE_API >> $1/env.sh
printenv | grep IMG_PROCESSOR >> $1/env.sh
printenv | grep CONTENT_UI >> $1/env.sh
printenv | grep SITE_MANAGER >> $1/env.sh
printenv | grep ARTIFACTORY >> $1/env.sh

# Prepare env.sh to be properly escaped and formatted
sed -i 's/"/\\"/g' $1/env.sh
sed -i 's/=/="/' $1/env.sh
sed -i 's/$/"/g' $1/env.sh
sed -i 's/^/export /' $1/env.sh