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
 
# Set up NVMe drive
mkfs.xfs -f /dev/nvme1n1
echo "/dev/nvme1n1 /mnt xfs defaults 0 0" >> /etc/fstab
mount -a

# Move the docker directory to the NVMe drive
mkdir /mnt/docker
chmod 777 /mnt/docker

systemctl stop docker
sed -i '/ExecStart=/ s/-H/--data-root \/mnt\/docker -H/' /lib/systemd/system/docker.service
systemctl daemon-reload
systemctl start docker

# Create a jenkins root dir and symlink the home dir
mkdir /mnt/jenkins
chown wpbuild: /mnt/jenkins
rm -rf /home/wpbuild/jenkins
ln -s /mnt/jenkins /home/wpbuild/jenkins
