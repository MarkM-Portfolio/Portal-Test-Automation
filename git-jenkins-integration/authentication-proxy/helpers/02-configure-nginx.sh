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
 

# Copy nginx.conf to overwrite the default config, force and no-prompt
sudo cp /home/centos/nginx.conf /etc/nginx/nginx.conf
# (Re-)start NGINX using the new config
sudo systemctl restart nginx