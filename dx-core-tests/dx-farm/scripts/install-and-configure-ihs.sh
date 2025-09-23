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

#Unzip the IHS Binaries
echo "Extracting the IHS Binaries"
mkdir /opt/ihsBinaries
mkdir /opt/IBM/HTTPServer

cd /tmp/dx-farm/ihs
unzip was.repo.9000.plugins.zip -d /opt/ihsBinaries/was.repo.9000.plugins/
unzip was.repo.9000.ihs.zip -d /opt/ihsBinaries/was.repo.9000.ihs/
unzip ibm-java-sdk-8.0-7.0-linux-x64-installmgr.zip -d  /opt/ihsBinaries/ibm-java-sdk-8.0-7.0-linux-x64-installmgr

#Install IHS, Plugin and the required JDK
echo "Installing IHS"
cd /opt/IBM/InstallationManager/eclipse/tools
./imcl -acceptLicense input /tmp/dx-farm/helpers/IHS_response.xml -showProgress -log /tmp/dx-farm/helpers/ihs_log.xml
echo "IBM HTTP Server Installation is complete!"

#Generate the Plugin
echo "Generating the Plugin"
cd /opt/IBM/WebSphere/wp_profile/bin
./GenPluginCfg.sh

#Overwrite the plugin config file with one we've already modified for our needs
mkdir -p /opt/IBM/WebSphere/Plugins/config/IHS/
cp /tmp/dx-farm/helpers/plugin-cfg.xml /opt/IBM/WebSphere/Plugins/config/IHS/
mv /opt/IBM/WebSphere/wp_profile/config/cells/plugin-cfg.xml /opt/IBM/WebSphere/wp_profile/config/cells/plugin-cfg-original.xml
cp /tmp/dx-farm/helpers/plugin-cfg.xml /opt/IBM/WebSphere/wp_profile/config/cells/

#Modify the HTTP config file
echo "Modifying the httpd.conf file"
cd /opt/IBM/HTTPServer/conf
cp httpd.conf httpd_backup.conf
echo 'LoadModule was_ap24_module /opt/IBM/WebSphere/Plugins/bin/64bits/mod_was_ap24_http.so' >> httpd.conf
echo 'WebSpherePluginConfig /opt/IBM/WebSphere/Plugins/config/IHS/plugin-cfg.xml'  >> httpd.conf

#Start the HTTP Server
/opt/IBM/HTTPServer/bin/apachectl start

echo "IBM HTTP Server Configuration is complete!"

