#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                              #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #

#Enable friendly locale URLs
echo "Enabling friendly locale URLs"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
./ConfigEngine.sh enable-friendly-locale-urls -Dfriendly-locale-list="de,en,es,nb,no,nn,pt_BR,zh_TW,zh,iw,zh-CN"
echo "./ConfigEngine.sh enable-friendly-locale-urls has completed"



