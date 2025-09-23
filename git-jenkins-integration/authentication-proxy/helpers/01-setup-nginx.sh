#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2019, 2022. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install OS dependencies for the upcoming setup

# NGINX setup comes from the EPEL-Repository
sudo yum -y install epel-release
# Install NGINX via yum
sudo yum -y install nginx
# Enable NGINX service
sudo systemctl enable --now nginx