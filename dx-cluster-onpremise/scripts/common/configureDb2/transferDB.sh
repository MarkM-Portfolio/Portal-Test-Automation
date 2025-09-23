#!/bin/bash
username=$1 
password=$2
dbhome=$3

# Move database source file to wp_profile
echo "Move database source files to wp_profile directory"
cd /opt/IBM/WebSphere/wp_profile
cp /tmp/dx-onpremise/properties/wkplc_sourceDb.properties  ./ConfigEngine/properties/
cp /tmp/dx-onpremise/properties/wkplc_sourceDb_ascii.properties  ./ConfigEngine/properties/

cd /tmp/dx-onpremise/ && tar cvzf $dbhome.tar.gz $dbhome
echo 'copy oraclehome into wp_profile from wp_profile/dxctl...'
cd /opt/IBM/WebSphere/wp_profile
cp -R /tmp/dx-onpremise/$dbhome/ ./
echo 'copy /tmp/dx-onpremise/properties/wkplc_dbd* into ConfigEngine/properties...'
cd /opt/IBM/WebSphere/wp_profile
cp /tmp/dx-onpremise/properties/wkplc_db*  ./ConfigEngine/properties/
echo 'database-transfer : Transfer db ...'
cd /opt/IBM/WebSphere/wp_profile/ConfigEngine
(nohup ./ConfigEngine.sh database-transfer -DWasPassword=$password & ) || echo "DB transfer Error"
