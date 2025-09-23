#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install OS dependencies

yum -y install unzip

# Set the public host name in the hosts file as not resolvable in some subnets
sudo -- sh -c -e "echo '$1  $2'>>/etc/hosts";

# Set additional host if provided
if [[ $# -gt 3 ]] ; then
    sudo -- sh -c -e "echo '$3  $4'>>/etc/hosts";
fi
