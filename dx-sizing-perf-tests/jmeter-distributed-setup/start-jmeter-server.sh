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

# Change directory 
cd /home/centos/apache-jmeter-5.6.3/bin || exit

# Enabling JMeter server process in the background with nohup
nohup ./jmeter-server >/dev/null 2>&1 &
