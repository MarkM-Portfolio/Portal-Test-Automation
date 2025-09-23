#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2020, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Define JDK version by using $1 as passed in number, e.g. 11

# Install OS dependencies
if [ "$1" == "8" ]
then
echo "Installing OpenJDK 8"
yum -y install java-1.8.0-openjdk
yum -y install java-1.8.0-openjdk-devel
else
echo "Installing OpenJDK 11"
yum -y install java-11-openjdk
yum -y install java-11-openjdk-devel
fi
yum -y install git
yum -y install unzip
yum -y install zip

# Extensions for SSL usage
yum -y install epel-release
yum -y install iptables-services
