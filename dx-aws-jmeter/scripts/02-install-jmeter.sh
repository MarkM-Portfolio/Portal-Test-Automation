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
 
# Install JMeter

JMETER_BINARY_VERSION=$1
echo "JMETER_BINARY_FILE_NAME: $JMETER_BINARY_VERSION.tgz"
cd /home/centos/
curl -LJO "https://dlcdn.apache.org//jmeter/binaries/${JMETER_BINARY_VERSION}.tgz"
tar -zxvf "$JMETER_BINARY_VERSION.tgz"

cd $JMETER_BINARY_VERSION/bin
./jmeter.sh -v