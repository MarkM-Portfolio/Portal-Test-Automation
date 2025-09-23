#!/bin/bash
#
#********************************************************************
# Licensed Materials - Property of HCL                              *
#                                                                   *
#  Copyright HCL Technologies Ltd. 2020, 2021. All Rights Reserved. *
#                                                                   *
#  Note to US Government Users Restricted Rights:                   *
#                                                                   *
#  Use, duplication or disclosure restricted by GSA ADP Schedule    *
# *******************************************************************
# 
# Settings for the generic latest machine, if environment vars are not set, defaults will be used
#

# network settings

export DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-"dx-deployment"}

# dx-core image settings
export DX_CORE_CONTAINER_NAME=${DX_CORE_CONTAINER_NAME:-"dx-core"}
export DX_CORE_PORT_CONFIGURATION=${DX_CORE_PORT_CONFIGURATION:-"-p 10039:10039 -p 10041:10041 -p 10200:10200 -p 10201:10201 -p 10202:10202 -p 10203:10203"}
export DX_CORE_ENV_CONFIGURATION=${DX_CORE_ENV_CONFIGURATION:-""}
export DX_CORE_IMAGE_FILTER=${DX_CORE_IMAGE_FILTER:-""}
export DX_CORE_URL=${DX_CORE_URL:-"127.0.0.1:10039/wps/portal"}

# media-library settings
export MEDIA_LIBRARY_HOSTNAME=${MEDIA_LIBRARY_HOSTNAME:-"$ENV_HOSTNAME"}
export MEDIA_LIBRARY_CONTAINER_NAME=${MEDIA_LIBRARY_CONTAINER_NAME:-"media-library"}
export MEDIA_LIBRARY_PORT_CONFIGURATION=${MEDIA_LIBRARY_PORT_CONFIGURATION:-"-p 3000:3001"}
export MEDIA_LIBRARY_ENV_CONFIGURATION=${MEDIA_LIBRARY_ENV_CONFIGURATION:-"-e POSTGRES_DB_URI=postgres://medialibrary:1234@media-library_pg-primary_1/test -e EXTERNAL_RING_API_HOST=$ENV_HOSTNAME -e EXTERNAL_RING_API_PORT=4000 -e RING_API_HOST=experience-api -e RING_API_PORT=3000 -e CORS_ORIGIN=http://$ENV_HOSTNAME:10039 -e IMAGE_PROCESSOR_API_HOST=image-processor -e IMAGE_PROCESSOR_API_PORT=8080 -e EXTERNAL_IMAGE_PROCESSOR_API_HOST=$ENV_HOSTNAME -e EXTERNAL_IMAGE_PROCESSOR_API_PORT=8080 -e EXTERNAL_BASE_API_HOST=$ENV_HOSTNAME -e EXTERNAL_BASE_API_PORT=3000 -e DEBUG=@medialibrary/server:* --log-driver json-file --log-opt max-size=100m"}
export MEDIA_LIBRARY_IMAGE_FILTER=${MEDIA_LIBRARY_IMAGE_FILTER:-""}
export MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER=${MEDIA_LIBRARY_PERSISTENCE_IMAGE_FILTER:-""}

# image-service settings
export IMG_PROCESSOR_CONTAINER_NAME=${IMG_PROCESSOR_CONTAINER_NAME:-"image-processor"}
export IMG_PROCESSOR_PORT_CONFIGURATION=${IMG_PROCESSOR_PORT_CONFIGURATION:-"-p 8080:8080"}
export IMG_PROCESSOR_ENV_CONFIGURATION=${IMG_PROCESSOR_ENV_CONFIGURATION:-"-e HOST=0.0.0.0 -e DEBUG=imageprocessor:* -e CORS_ORIGIN=http://$ENV_HOSTNAME:3000,\ http://$ENV_HOSTNAME:10039  --log-driver json-file --log-opt max-size=100m"}
export IMG_PROCESSOR_IMAGE_FILTER=${IMG_PROCESSOR_IMAGE_FILTER:-""}

# experience-api settings
export EXPERIENCE_API_CONTAINER_NAME=${EXPERIENCE_API_CONTAINER_NAME:-"experience-api"}
export EXPERIENCE_API_PORT_CONFIGURATION=${EXPERIENCE_API_PORT_CONFIGURATION:-"-p 4000:3000"}
export EXPERIENCE_API_ENV_CONFIGURATION=${EXPERIENCE_API_ENV_CONFIGURATION:-"-e PORTAL_PORT=10039 -e PORTAL_HOST=dx-core -e DEBUG=ringapi-server:* -e CORS_ORIGIN=http://$ENV_HOSTNAME:3000,\ http://$ENV_HOSTNAME:5000,\ http://$ENV_HOSTNAME:10039  --log-driver json-file --log-opt max-size=100m"}
export EXPERIENCE_API_IMAGE_FILTER=${EXPERIENCE_API_IMAGE_FILTER:-""}

# content-ui settings
export CONTENT_UI_HOSTNAME=${CONTENT_UI_HOSTNAME:-"$ENV_HOSTNAME"}
export CONTENT_UI_CONTAINER_NAME=${CONTENT_UI_CONTAINER_NAME:-"content-ui"}
export CONTENT_UI_PORT_CONFIGURATION=${CONTENT_UI_PORT_CONFIGURATION:-"-p 5000:3000"}
export CONTENT_UI_ENV_CONFIGURATION=${CONTENT_UI_ENV_CONFIGURATION:-"-e EXTERNAL_RING_API_HOST=$ENV_HOSTNAME -e EXTERNAL_RING_API_PORT=4000 -e RING_API_HOST=experience-api -e RING_API_PORT=3000 -e DEBUG=@content-ui/server:* -e CORS_ORIGIN=http://$ENV_HOSTNAME:10039 --log-driver json-file --log-opt max-size=100m"}
export CONTENT_UI_IMAGE_FILTER=${CONTENT_UI_IMAGE_FILTER:-""}

# site-manager settings
export SITE_MANAGER_HOSTNAME=${SITE_MANAGER_HOSTNAME:-"$ENV_HOSTNAME"}
export SITE_MANAGER_CONTAINER_NAME=${SITE_MANAGER_CONTAINER_NAME:-"site-manager"}
export SITE_MANAGER_PORT_CONFIGURATION=${SITE_MANAGER_PORT_CONFIGURATION:-"-p 5500:3000"}
export SITE_MANAGER_ENV_CONFIGURATION=${SITE_MANAGER_ENV_CONFIGURATION:-"-e WCMREST_API_HOST=$ENV_HOSTNAME -e WCMREST_API_CONTEXT_ROOT=$SITE_MANAGER_WCMREST_API_CONTEXT_ROOT -e EXTERNAL_RING_API_HOST=$ENV_HOSTNAME -e EXTERNAL_RING_API_PORT=4000 -e RING_API_HOST=experience-api -e RING_API_PORT=3000 -e DEBUG=@site-manager/server:* -e CORS_ORIGIN=http://$ENV_HOSTNAME:10039,\ http://$ENV_HOSTNAME,\ http://$ENV_HOSTNAME:8090 --log-driver json-file --log-opt max-size=100m"}
export SITE_MANAGER_IMAGE_FILTER=${SITE_MANAGER_IMAGE_FILTER:-""}
