#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2024. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

export SHARE_DIRECTORY=/var/nfs

mkdir $SHARE_DIRECTORY
chown nfsnobody:nfsnobody $SHARE_DIRECTORY
chmod 755 $SHARE_DIRECTORY

# Create the NFS server configuration file /etc/exports:
cat <<EOF > /etc/exports
$SHARE_DIRECTORY *(rw,sync,no_subtree_check)
EOF

# Start the NFS server:
systemctl start --now nfs-server

# Enable the NFS server to start at boot:
sudo systemctl enable --now nfs-server.service

# Add new exports to the NFS server:
exportfs -a