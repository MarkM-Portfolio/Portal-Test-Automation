#!/bin/bash
echo "copy $2 into wp_profile from wp_profile/$1..."
cd /opt/HCL/wp_profile
cp -R ./$1/$2/ ./
echo "copy $1/properties/wkplc_dbd* into ConfigEngine/properties..."
cd /opt/HCL/wp_profile
cp $1/properties/wkplc_db*  ./ConfigEngine/properties/
echo 'database-transfer : Transfer db ...'
cd /opt/HCL/wp_profile/ConfigEngine
./ConfigEngine.sh database-transfer -DWasPassword=$3 || echo "DB transfer Error"
