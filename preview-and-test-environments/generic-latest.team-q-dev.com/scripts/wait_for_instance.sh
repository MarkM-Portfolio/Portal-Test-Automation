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
# The script wait until the given AWS instance is up and running
#

do_wait_for_ping(){
   #wait for ping on remote system
   n=1
   while [[ "$(ping -c 1 -w 2 ${TARGET_IP} 2>&1)" != *"0% packet loss"* ]]; do
     echo "Ping not OK, waiting another 30s"
     sleep 30
     n=$(( n+1 ))
     if [ $n -eq 20 ]; then
       echo "Ping failed within alotted time"
       exit 1
     fi
   done
   echo "Ping OK"
} 

do_wait_for_instance(){
   #wait for the instance using ssh ls -al on remote system
   n=1
   while [[ "$(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 -i ${DEPLOY_KEY} centos@${TARGET_IP} 'echo --ok--' 2>&1)" != *"--ok--" ]]; do
     echo "Instance not running yet, waiting another 30s"
     sleep 30
     n=$(( n+1 ))
     if [ $n -eq 20 ]; then
       echo "Instance failed to initialize within alotted time"
       exit 1
     fi
   done
   echo "Instance is running"
}

do_wait_for_ping
do_wait_for_instance
