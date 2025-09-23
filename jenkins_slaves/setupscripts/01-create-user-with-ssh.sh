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

# Add the necessary users for the setup
# wpbuild:wpbuild

# Also provides SSH Pub-Keys so the jenkins master can access

groupadd wpbuild
adduser -g wpbuild wpbuild
mkdir -p /home/wpbuild/.ssh
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC6XIEwUYKrdpG2qg+GeI61lwUEETrX8iW346K9s9thyoCF/T+J/lLkj5luQiS7eqvhXSy/JnM0U/OAkVdkTe/f4Zf3J4dHjovUcXHqOrfRievf4Hxk1iSWAO1cnc6r2WEU1+9g/n6BBYXgdVaW0UtZc5ydgGmIgUk2NISwdofYzI4lqX3n3VYJR6l5kBvVjZsYdOpe6N8jcVfnY4OuLPyTKos2ElXhFPiATsqmoyKDkA4lOfNln3g8QUlMeRZDnCxrGgSu3VxXY0/nEXvX6hlu0y7pcbgjlh2Jo6XYqKV830ICJGAtyVB4bnkK+qxj1tpKuyVTJkMaL3vFYIphCeXF wpbuild@buildwp4" > /home/wpbuild/.ssh/authorized_keys
echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDeQByDfEl5PgexQGfvGgIENamBVlyDBn8b9I+YGZQL5vwSbx+J7WkYo3sBzUP9xq4X8bV7qSmC3bVfrPpiGKFdkj1Ya6HDbZLKpnA5hVqtWluRtbsMHWNH7QuPYKtGNGeyUeHl8IVPRSmQE2iUCl7sSFU6BoV5gGvdvMq88GqI9Nph0JqkUbAzsJqF7FQdQgj2dZpsFmwYFYjeveyVbWx3x+WbU2mLyPZOid9YhdldeSQHHq+qeYooZ3JrbGR/qjF0yGtHKHKx1AVFrvSYleeABEDspveRlupsrJSHelS34FjVW9SSQETzrdBWIkbkuVhhiigmu5lyyDfF6Rx64OrD wpbuild" >> /home/wpbuild/.ssh/authorized_keys
ssh-keygen -t rsa -P "" -f /home/wpbuild/.ssh/id_rsa -C "wpbuild@$1"
cat /home/wpbuild/.ssh/id_rsa.pub >> /home/wpbuild/.ssh/authorized_keys

# Set SSH config to avoid failures due to reuse of IP addresses and due to timeouts across subnets
cat <<EOF > /home/wpbuild/.ssh/config
Host *
  ServerAliveInterval 30
  ServerAliveCountMax 5
  StrictHostKeyChecking no
  UserKnownHostsFile=/dev/null
EOF

chown -R wpbuild: /home/wpbuild/.ssh
chmod 700 /home/wpbuild/.ssh
chmod 600 /home/wpbuild/.ssh/authorized_keys
