#!/bin/bash
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
EC2_INTERNAL_DNS=$1
EC2_INTERNAL_IP=$2

DB2_PASSWORD="DB2_PASSWORD_PLACEHOLDER"

#Verify Docker is installed
DOCKER_INSTALLED=$(docker info | grep Containers)

if [[ $? -eq 0 && $DOCKER_INSTALLED != "" ]];then
   echo "Docker is installed and accessible."

   docker load < /tmp/db2-image.docker
   docker run -itd --name dxdb2 --restart always -e DB2INST1_PASSWORD=$DB2_PASSWORD --privileged=true --add-host=$EC2_INTERNAL_DNS:$EC2_INTERNAL_IP --network=host --hostname=$EC2_INTERNAL_DNS -p 50000:50000 -p 50001:50001 IMAGE_NAME_PLACEHOLDER

   DB2_RUNNING=$(docker ps | grep dxdb2)

   if [[ "$DB2_RUNNING" == "" ]];then
      echo "DB2 Container is not running."
      exit 1
   fi
else
   echo "Docker is not responsive.  Exiting."
   exit 1
fi