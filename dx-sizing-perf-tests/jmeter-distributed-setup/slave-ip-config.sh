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
cd /home/centos/apache-jmeter-5.6.3/bin || exit

# File containing slave IPs, one IP per line
SlaveIPsFile="./slave_ip.txt"

# Check if the slave IPs file exists
if [ -f "$SlaveIPsFile" ]; then
    # Read each line (IP) from the file and store it in a variable
    new_hosts=$(awk 'NF' "$SlaveIPsFile" | paste -sd "," -)

    # Replace the remote_hosts property with the IPs from the file in jmeter.properties
    sed -i "/^remote_hosts=/ s/[^=]*$/$new_hosts/" jmeter.properties
    

    echo "Slave IPs added to remote_hosts in jmeter.properties."
else
    echo "Slave IPs file ($SlaveIPsFile) not found."
fi
