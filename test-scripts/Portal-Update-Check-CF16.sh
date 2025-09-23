

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

if grep -q fixlevel=CF16 /opt/IBM/WebSphere/PortalServer/wps.properties && grep -q fixlevel=CF16 /opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties
then 
echo "Rollback from CF17 to CF16 is successful ! "
cat '/opt/IBM/WebSphere/PortalServer/wps.properties'
cat '/opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties'
else
echo "Rollback from CF17 to CF16 failed  ! "
cat '/opt/IBM/WebSphere/PortalServer/wps.properties'
cat '/opt/IBM/WebSphere/wp_profile/PortalServer/wps.properties'
exit 1
fi 


