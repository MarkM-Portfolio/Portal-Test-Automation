#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
# Install OS dependencies

sudo yum install -y java-1.8.0-openjdk
sudo yum install -y expect
sudo yum install -y git
sudo yum install -y rng-tools
sudo service rngd start
sudo service rngd status