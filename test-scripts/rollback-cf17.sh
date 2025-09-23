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
    HOST_IP_ADDR=$3
    echo "HOST IP : $HOST_IP"
    echo "HOST NAME : $HOST_NAME"
    echo "HOST IP ADDRESS : $HOST_IP_ADDR"
fi

echo $HOST_NAME@$HOST_IP

ssh -o StrictHostKeyChecking=no $HOST_NAME@$HOST_IP << EOF 
    sh /change-host-ip.sh $HOST_IP_ADDR
    sh /rollback.sh 
    sh /Portal-Update-Check-CF16.sh
EOF
