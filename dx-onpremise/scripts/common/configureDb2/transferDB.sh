#!/bin/bash
username=$1 
password=$2
dbhome=$3

cd /tmp/dx-onpremise/ && tar cvzf $dbhome.tar.gz $dbhome
echo 'copy oraclehome into wp_profile from wp_profile/dxctl...'
cd /opt/HCL/wp_profile
cp -R /tmp/dx-onpremise/$dbhome/ ./
echo 'copy /tmp/dx-onpremise/properties/wkplc_dbd* into ConfigEngine/properties...'
cd /opt/HCL/wp_profile
cp /tmp/dx-onpremise/properties/wkplc_db*  ./ConfigEngine/properties/
echo 'database-transfer : Transfer db ...'
cd /opt/HCL/wp_profile/ConfigEngine
(nohup ./ConfigEngine.sh database-transfer -DWasPassword=$password & ) || echo "DB transfer Error"
