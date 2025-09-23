#!/bin/sh

#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2021. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #


# Validate the Windows CF update
if [ XXX$1 != "XXX" ]; then
    CF_VERSION=$1
fi


if grep -q fixlevel=$CF_VERSION check1.properties && grep -q fixlevel=$CF_VERSION check2.properties
then 
    echo "$CF_VERSION Installation Successful - Sweet !"
else
    echo "$CF_VERSION Installation Failed -- Too Bad !"
    exit 1
fi 