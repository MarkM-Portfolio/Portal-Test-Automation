#!/bin/sh
#
 ####################################################################
 # Licensed Materials - Property of HCL                             #
 #                                                                  #
 # Copyright HCL Technologies Ltd. 2001, 2022. All Rights Reserved. #
 #                                                                  #
 # Note to US Government Users Restricted Rights:                   #
 #                                                                  #
 # Use, duplication or disclosure restricted by GSA ADP Schedule    #
 ####################################################################
 #
 
 #Farm# is currently pointed at local DB2 databases.  We need it to point to the DB2 databases that live on Farm1
 echo "Configuring database"
 sed -i 's/localhost/dx-farm-1/g' /opt/IBM/WebSphere/wp_profile/ConfigEngine/properties/wkplc_dbdomain.properties
 cd /opt/IBM/WebSphere/wp_profile/ConfigEngine/
 ./ConfigEngine.sh connect-database -DWasPassword=wpsadmin

 #Delete temp files
 echo "Deleting temp files"
 rm -rf /opt/IBM/WebSphere/wp_profile/temp/ci-linux*/WebSphere_Portal/*
 
 #Start Portal
 echo "Starting Portal"
 /opt/IBM/WebSphere/wp_profile/bin/startServer.sh WebSphere_Portal

#Change the  Portal hostname
echo "Changing the Portal hostname"
/opt/IBM/WebSphere/wp_profile/bin/wsadmin.sh -c "\$AdminTask changeHostName {-nodeName ci-linuxstal-39sht7rxNode -hostName localhost}; \$AdminConfig save" -conntype NONE

#This step is only required on the Farm3 worker.  We have to edit some WCM properties
echo "Edit WCM properties on the farm worker"
sed -i 's/remoteHostName/dx-farm-2/g' /opt/IBM/WebSphere/wp_profile/PortalServer/wcm/config/properties/prereq.wcm.properties
sed -i 's/remoteHostSIBEndpoint/10048/g' /opt/IBM/WebSphere/wp_profile/PortalServer/wcm/config/properties/prereq.wcm.properties
sed -i 's/remoteNodeName/ci-linuxstal-39sht7rxNode/g' /opt/IBM/WebSphere/wp_profile/PortalServer/wcm/config/properties/prereq.wcm.properties


#Create the Farm worker
echo "Creating the farm worker"
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine/
./ConfigEngine.sh localize-clone -DWasPassword=wpsadmin
./ConfigEngine.sh create-wcm-jms-resources-remote -DWasPassword=wpsadmin
./ConfigEngine.sh enable-farm-mode -DsystemTemp=/opt/IBM/WebSphere/wp_profile -DWasPassword=wpsadmin

#Stop Portal
echo "Stopping Portal"
/opt/IBM/WebSphere/wp_profile/bin/stopServer.sh WebSphere_Portal -username wpsadmin -password wpsadmin

#Start Portal using the new start command
echo "Starting Portal using the new command"
/opt/IBM/WebSphere/wp_profile/PortalServer/bin/start_WebSphere_Portal.sh

echo "Farm3 configuration is complete!"

