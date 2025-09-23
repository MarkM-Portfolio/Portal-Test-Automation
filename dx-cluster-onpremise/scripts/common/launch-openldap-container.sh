#!/bin/bash
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
 
IMAGE_PATH=$1

#Verify Docker is installed
DOCKER_INSTALLED=$(docker info | grep Containers)

if [[ $? -eq 0 && $DOCKER_INSTALLED != "" ]];then
   echo "Docker is installed and accessible."

   docker load < /tmp/openldap-image.docker
   # Starting the OpenLDAP container with open port "1389". 
   docker run -d -e LOCAL=true --name=dx-open-ldap -p 1389:1389 $IMAGE_PATH

   OPENLDAP_RUNNING=$(docker ps | grep dx-open-ldap)
   CONTAINER_ID=$(docker ps | grep dx-open-ldap | awk '{ print $1 }')
   echo "CONTAINER_ID: ${CONTAINER_ID}"

   if [[ "$OPENLDAP_RUNNING" == "" ]];then
      echo "OpenLDAP Container is not running."
      exit 1
   else
      # Add extra users with `cn` schema for testing. Default users would comes with `uid` schema
      docker cp /tmp/dx-onpremise/scripts/common/configureLDAP/test_users.ldif dx-open-ldap:/home/dx_user/test_users.ldif
      docker exec -i dx-open-ldap /bin/sh -c "
      cat /home/dx_user/test_users.ldif
      cd /var/dx-openldap/bin/
      ./ldapadd -h \$HOSTNAME -p 1389 -f /home/dx_user/test_users.ldif -x -D cn=dx_user,dc=dx,dc=com -w p0rtal4u -v"
   fi
else
   echo "Docker is not responsive.  Exiting."
   exit 1
fi