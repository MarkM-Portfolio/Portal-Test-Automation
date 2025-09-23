#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# The script creates the necessary docker network for the services
#

# disconnect all old containers from the network
docker network disconnect $DOCKER_NETWORK_NAME $DX_CORE_CONTAINER_NAME
docker network disconnect $DOCKER_NETWORK_NAME $IMG_PROCESSOR_CONTAINER_NAME
docker network disconnect $DOCKER_NETWORK_NAME $EXPERIENCE_API_CONTAINER_NAME
docker network disconnect $DOCKER_NETWORK_NAME $MEDIA_LIBRARY_CONTAINER_NAME
docker network disconnect $DOCKER_NETWORK_NAME $CONTENT_UI_CONTAINER_NAME
docker network disconnect $DOCKER_NETWORK_NAME media-library_pg-primary_1

# create docker network
docker network rm $(docker network ls -f name=$DOCKER_NETWORK_NAME -q)
docker network create $DOCKER_NETWORK_NAME

