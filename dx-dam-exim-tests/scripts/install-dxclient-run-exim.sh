#!/bin/sh
#/*
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2023. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************
# */

cd /home/centos/ && curl -s -u${USERNAME}:${PASSWORD} ${pipelineParameters.TOOL_PACKAGE_URL} --output dxclient.zip
yes | unzip dxclient.zip && ls
cd /home/centos/dxclient && docker load < dxclient.tar.gz

  ./bin/dxclient -h
    EXPORT_START_TIME=$(date +%s)
    ./bin/dxclient manage-dam-assets export-assets -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${SERVER_HOST_SOURCE}  -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.USERNAME} -dxPassword ${pipelineParameters.PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT} -exportBinary ${pipelineParameters.EXPORT_BINARY}
    EXPORT_END_TIME=$(date +%s)
    TOTAL_EXPORT_TIME_IN_SECS=$(($EXPORT_END_TIME - $EXPORT_START_TIME))
    printf 'Total Export Time -  %dh:%dm:%ds\t' $(($TOTAL_EXPORT_TIME_IN_SECS/3600)) $(($TOTAL_EXPORT_TIME_IN_SECS%3600/60)) $(($TOTAL_EXPORT_TIME_IN_SECS%60)) 

    ./bin/dxclient manage-dam-assets validate-assets -exportPath ${pipelineParameters.EXPORT_PATH}

    IMPORT_START_TIME=$(date +%s)
    ./bin/dxclient manage-dam-assets import-assets -dxProtocol ${pipelineParameters.DX_PROTOCOL} -hostname ${SERVER_HOST_TARGET} -dxPort ${pipelineParameters.DX_PORT} -dxUsername ${pipelineParameters.USERNAME} -dxPassword ${pipelineParameters.PASSWORD} -damAPIPort ${pipelineParameters.DAM_API_PORT} -ringAPIPort ${pipelineParameters.RING_API_PORT}
    IMPORT_END_TIME=$(date +%s)
    TOTAL_IMPORT_TIME_IN_SECS=$(($IMPORT_END_TIME - $IMPORT_START_TIME))
    printf ' Total Import Time - %dh:%dm:%ds\t' $(($TOTAL_IMPORT_TIME_IN_SECS/3600)) $(($TOTAL_IMPORT_TIME_IN_SECS%3600/60)) $(($TOTAL_IMPORT_TIME_IN_SECS%60))
                           