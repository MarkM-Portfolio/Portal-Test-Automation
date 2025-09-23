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

#Accepts new ip as input to the script
if [ XXX$1 != "XXX" ]; then
    echo "New Host domain name : "$1
    HOST_IP=$1
fi

if [ ! -e /etc/hosts-copy ] 
then
#create a copy of the original hosts file
cp /etc/hosts /etc/hosts-copy
else
#replace with original hosts file copy
cp /etc/hosts-copy /etc/hosts
fi

#appends new ip to the hosts and comments out the original ip
sed -i "1 s/.*/&  $HOST_IP/"  /etc/hosts
sed -i '2 s/^/#/' /etc/hosts
