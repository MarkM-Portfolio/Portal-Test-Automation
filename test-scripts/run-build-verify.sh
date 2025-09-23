#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2019. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

if [ XXX$1 != "XXX" ]; then
    HOST_NAME=$1
    HOST_IP=$2
    HOST_DNS=$3
    DXBuildNumber=$4
    DXBuildName=$5
    echo "HOST IP : $HOST_IP"
    echo "HOST NAME : $HOST_NAME"
    echo "HOST DNS : $HOST_DNS"
    echo "BUILD NUMBER : $DXBuildNumber"
    echo "BUILD NAME : $DXBuildName"
fi

echo $HOST_NAME@$HOST_IP

ssh -o StrictHostKeyChecking=no $HOST_NAME@$HOST_IP << EOF 
    sh /change-host-ip.sh $HOST_IP
    sh /Build-Verify.sh wpbuild p0rtal4u $DXBuildNumber $DXBuildName
EO
