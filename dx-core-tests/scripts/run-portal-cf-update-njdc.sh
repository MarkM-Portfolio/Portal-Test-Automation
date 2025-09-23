#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd.2022. All Rights Reserved.        #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

if [ XXX$1 != "XXX" ]; then
    HOST_USER=$1
    HOST_IP=$2
    FTP_HOST=$3
    FTP_USER=$4
    FTP_PASSWORD=$5
    CF_VERSION=$6
    BUILD_LABEL=$7
    CONNECT_KEY=$8
    echo "HOST IP : $HOST_IP"
    echo "HOST USER : $HOST_USER"
fi

ssh -i $CONNECT_KEY -o StrictHostKeyChecking=no $HOST_USER@$HOST_IP << EOF 
    sh /portal-update-cf-njdc.sh $FTP_USER $FTP_PASSWORD $FTP_HOST wpsadmin $CF_VERSION $BUILD_LABEL
EOF
