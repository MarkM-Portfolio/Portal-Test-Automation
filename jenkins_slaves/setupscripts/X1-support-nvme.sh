#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2023. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Optional Script to enable nvme support of instances if existing

scriptDir=$(dirname -- "$(readlink -f -- "$BASH_SOURCE")")

# place boot script in home directory
cp $scriptDir/helpers/bootscript.nvme.sh /home/wpbuild/.
chmod 755 /home/wpbuild/bootscript.nvme.sh 

# add startup service
cp $scriptDir/helpers/nvme_support.service /etc/systemd/system/.
systemctl daemon-reload
systemctl enable nvme_support.service

# change docker config
systemctl disable docker
systemctl stop docker
sed -i '/ExecStart=/ s/-H/--data-root \/mnt\/docker -H/' /lib/systemd/system/docker.service