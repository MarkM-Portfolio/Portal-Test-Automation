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

# Path to JMeter bin directory
cd /home/centos/apache-jmeter-5.6.3/bin

# Grant execute permission to create-rmi-keystore.sh
chmod +x create-rmi-keystore.sh

# Execute create-rmi-keystore.sh and provide user inputs for keystore creation
 ./create-rmi-keystore.sh << EOF
rmi
a
b
s
d
IN
yes
EOF