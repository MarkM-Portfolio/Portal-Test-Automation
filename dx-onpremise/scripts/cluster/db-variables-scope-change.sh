was_password=$1

cp /tmp/dx-onpremise/wkplc_dbtype.properties /opt/HCL/wp_profile/ConfigEngine/properties/
cp /tmp/dx-onpremise/wkplc_dbdomain.properties /opt/HCL/wp_profile/ConfigEngine/properties/
/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DWasPassword=$was_password action-cluster-create-database-variables-on-secondary-node -DWasPassword=$was_password
