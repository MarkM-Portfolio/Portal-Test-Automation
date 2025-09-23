#!/bin/bash

# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication, or disclosure restricted by GSA ADP Schedule   *
# ********************************************************************

# Change directory to /home/centos/
cd /home/centos/apache-jmeter-5.6.3/bin

# Using sed to replace the line '#server.rmi.ssl.disable=false' with 'server.rmi.ssl.disable=true'
sed -i 's/#server.rmi.ssl.disable=false/server.rmi.ssl.disable=true/' jmeter.properties