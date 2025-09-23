#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# This script will be booted by systemd at startup to support nvme

# initialize a file system
umount /mnt
mkfs.xfs -f /dev/nvme1n1

# mount the nvme disk at /mnt
mount /dev/nvme1n1 /mnt

# create a jenkins root dir and symlink the home dir
mkdir /mnt/jenkins
chown wpbuild: /mnt/jenkins
rm -rf /home/wpbuild/jenkins
ln -s /mnt/jenkins /home/wpbuild/jenkins

#create a docker dir
mkdir /mnt/docker
chmod 777 /mnt/docker

# start docker deamon since initial autostart was disabled to support the nvme drive
systemctl start docker