#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 

# Extensions for using Certbot and Let's Encrypt CA certificate
yum -y install snapd
systemctl enable --now snapd.socket
ln -s /var/lib/snapd/snap /snap
systemctl start snapd.socket
systemctl start snapd
systemctl restart snapd.seeded.service
systemctl status snapd

snap install --classic certbot
snap set certbot trust-plugin-with-root=ok
snap install certbot-dns-route53
snap set certbot trust-plugin-with-root=ok
ln -s /snap/bin/certbot /usr/bin/certbot
