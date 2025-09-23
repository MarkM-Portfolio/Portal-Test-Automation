# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

export http_proxy=$1
export https_proxy=$1
printenv | sort

### Configure firewall ###
# Install firewalld and enable service
sudo https_proxy=$1 http_proxy=$1 yum install -y firewalld
sudo systemctl enable --now firewalld.service

# Configure firewall rules and reload configuration
sudo firewall-cmd --permanent --zone=public --add-service=ssh
sudo firewall-cmd --permanent --zone=public --add-service=nfs

# Apply configuration
sudo firewall-cmd --reload

### NFS Setup ###
# Install NFS utils and enable server service
sudo https_proxy=$1 http_proxy=$1 yum install -y nfs-utils
sudo systemctl enable --now nfs-server.service

# Increase NFS Server Thread size to 64
echo "
[nfsd]
threads=64
" | sudo tee /etc/nfs.conf
sudo systemctl restart nfs-server.service

# Check if NVMe exists and configure it
if [ -e /dev/nvme0n1 ]
then
  # Create partition on NVMe SSD
  # Passing in parameters to fdisk via echo and pipe
  # Indention needs to be ignored, since following lines are passed through directly to fdisk
echo "n
p
1


w
"| sudo fdisk /dev/nvme0n1

  # Create EXT4 filesystem on NVMe SSD
  sudo mkfs.ext4 /dev/nvme0n1p1

  # Mount NVMe SSD as storage drive
  sudo mkdir -p /mnt/storage
  sudo mount /dev/nvme0n1p1 /mnt/storage
fi

# Create test share directory and configure it for NFS
sudo mkdir -p /mnt/storage/default
sudo chown -R nfsnobody: /mnt/storage/default
sudo chmod -R 777 /mnt/storage/default
sudo rm -rf /etc/exports.d/*
echo "/mnt/storage/default *(rw,sync,root_squash,no_subtree_check)" | sudo tee /etc/exports.d/default.exports
exportfs -a

### FIO baseline performance test, single thread ###
# Install FIO
sudo https_proxy=$1 http_proxy=$1 yum install -y fio
# Run single test, mixed RW, 75/25 ratio
fio --randrepeat=1 --ioengine=libaio --direct=1 --gtod_reduce=1 --name=test --bs=4k --iodepth=64 --readwrite=randrw --rwmixread=75 --size=4G --filename=/mnt/storage/default/fio --numjobs=4
