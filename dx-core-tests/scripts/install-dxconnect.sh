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

#Install dxconnect
echo "Installing dxconnect"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
./ConfigEngine.sh reinstall-dxconnect-application
echo "./ConfigEngine.sh reinstall-dxconnect-application has completed"

#Check to see if dxconnect is deployed and running in the cw_profile's WAS admin console
echo "Verifying dxconnect is intalled and running"
#Switch to the cw_profile.  Use the wsadmin AdminApplication.checkIfAppExists feature to determine if dxconnect is deployed.  If it is, it will return true and write the response to a text file.
cd /opt/IBM/WebSphere/AppServer/profiles/cw_profile/bin
echo 'print AdminApplication.checkIfAppExists("dxconnect")' > file.txt ; ./wsadmin.sh -conntype NONE -lang jython -f file.txt 1> dxconnect_status.txt
#Now we check in the generated .txt file to see if true is returned true.  We look for 2 occurrences of true as there is an occurrence of the word already in the response
if [ "$(grep -c "true" dxconnect_status.txt)" -ge 2 ];
  then
    echo "dxconnect installed successfully!!"
  else
    echo "dxconnect failed to install"
fi


