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

# Add a admUser user for the setup
# default: centos:centos

# Also provides SSH Pub-Keys so it can be accessed
admUser="centos"
if [ "$1" != "" ]; then
   admUser=$1
fi

groupadd ${admUser}
adduser -g ${admUser} ${admUser}
mkdir -p /home/${admUser}/.ssh
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0ygAFZcplnnWUFWlP0sJeOyXPbDO8e7cQN0DumqVLBVykOmopn7T1EPK1o9/h4/ulTHq8+OVMT4L05wykkm9uPd0TLMHL61iruIrF+vz3EtQksJTXCeIfmaDqHKVDPOcfJL103E36puBwgolhnnPMAJPCOBpZBfQ7HWgjZsB9q8i6ouG3zzK209iJKMcDWN2gFxpNXYNu0QSQSZSabHJiM9caoI9QO+I41iMC2ZV8EYNiWxrHFesMdD1Vq/XFYLVg02C2VwKJbE6hBPkfZ6M41GQg0h2qcWLzkf4jZDYHCFQQTKgZtecggatJHgS0iGiX27m+EEM/cjB0lKcslyvJ test-automation-deployments" > /home/${admUser}/.ssh/authorized_keys

usermod -a -G adm ${admUser}
usermod -a -G systemd-journal ${admUser}
usermod -a -G wheel ${admUser}

chmod 640  /etc/sudoers
echo " " >> /etc/sudoers
echo "## Allow ${admUser} to run sudo without passwor " >> /etc/sudoers
echo "${admUser}    ALL=(ALL)       NOPASSWD:ALL" >> /etc/sudoers
chmod 440  /etc/sudoers

# Set SSH config to avoid failures due to reuse of IP addresses and due to timeouts across subnets
cat <<EOF > /home/${admUser}/.ssh/config
Host *
  ServerAliveInterval 30
  ServerAliveCountMax 5
  StrictHostKeyChecking no
  UserKnownHostsFile=/dev/null
EOF

chown -R centos: /home/${admUser}/.ssh
chmod 700 /home/${admUser}/.ssh
chmod 600 /home/${admUser}/.ssh/authorized_keys
