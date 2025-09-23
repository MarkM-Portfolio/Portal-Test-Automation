# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2022. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

# Wrapping script, calling all subsequent scripts in chronolical order

echo $1

sh 01-setup-nfs-server.sh $1
sh 02-setup-docker.sh $1
# Create a new user session, to apply docker group
sudo su centos -c "sh 03-setup-monitoring.sh $1"