#!/bin/bash
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */
# This script installs the necessary pre-reqs for the acceptance test instance
# NodeJS
# NVM
# Install NodeJS
sudo yum install -y -q gcc-c++ make unzip
curl -sL https://rpm.nodesource.com/setup_12.x | sudo -E bash -
# currently latest nodejs-12.22.7-1nodesource ends up in a download error
# so we catch the output check for it and use hard coded nodejs-12.22.6-1nodesource instead as workaround
# as soon as the error disappears we take whatever we get
ret=$(sudo yum install -q -y nodejs 2>&1)
echo "$ret"
if [[ "$ret" == *"Error downloading packages:"* ]]; then
    echo " "
    echo "=========================================================================="
    echo " Download error detected install nodejs-12.22.6-1nodesource as workaround"
    echo "=========================================================================="
    sudo yum install -q -y nodejs-12.22.6-1nodesource
fi
# Echo version for verification
node -v
npm -v