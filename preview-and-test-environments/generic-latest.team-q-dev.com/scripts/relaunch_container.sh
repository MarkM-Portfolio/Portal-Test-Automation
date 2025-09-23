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
# The script relaunches the docker service and the restarts the image if necessary
#

do_wait_for_container(){
   #wait for the container using curl, netstat is not accurate
   n=1
   while [[ "$(curl -L -s -o /dev/null -w ''%{http_code}'' $DX_CORE_URL)" != "200" ]]
   do
        echo "Container not running yet, waiting another 30s"
        sleep 30
        n=$(( n+1 ))
        if [ $n -eq 20 ]; then
          echo "Container failed to run within alotted time"
          exit 1
        fi
   done
   echo "Container is running"
}

echo "Restart docker service and container(s) if needed"

# check if docker service is running, maybe restart
status=$(systemctl status docker | grep "Active:")
if [[ "$status" == *"Active: inactive "* ]]; then
   echo "Restart docker service"
   sudo systemctl start docker
fi

# check if container is running, maybe restart
for cid in $(docker ps -a | awk '{print $1}'); do
   status=$(docker ps | grep $cid)
   if [[ "$status" == "" ]]; then
      echo "Restart docker container $cid"
      docker start $cid
   fi
done

# wait for DX Core container running
do_wait_for_container