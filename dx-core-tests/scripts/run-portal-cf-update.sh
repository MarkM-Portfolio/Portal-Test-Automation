#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2020. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

if [ XXX$1 != "XXX" ]; then
    HOST_USER=$1
    HOST_IP=$2
    HOST_DNS=$3
    FTP_HOST=$4
    FTP_USER=$5
    FTP_PASSWORD=$6
    CF_VERSION=$7
    BUILD_LABEL=$8
    CONNECT_KEY=$9
    echo "HOST IP : $HOST_IP"
    echo "HOST USER : $HOST_USER"
    echo "HOST DNS : $HOST_DNS"
fi

ssh -i $CONNECT_KEY -o StrictHostKeyChecking=no $HOST_USER@$HOST_IP << EOF 
    sh /change-host-ip.sh $HOST_IP
    sh /portal-update-cf.sh $FTP_USER $FTP_PASSWORD $FTP_HOST wpsadmin $CF_VERSION $BUILD_LABEL
EOF
