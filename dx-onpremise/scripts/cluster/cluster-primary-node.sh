# Creating cluster on primary node
was_username=wpsadmin
was_password=wpsadmin
portal_username=wpsadmin
portal_password=wpsadmin
db_username=db2inst1
db_password=diet4coke

/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties="/tmp/dx-onpremise/cluster/properties/FederateNode.properties" -DWasPassword=$was_password -DDmgrNodeName=dmgrNode01 -DDmgrCellName=dmgrCell01 -DdmgrProfilePath=/opt/HCL/AppServer/profiles/dmgr01 -DisRemoteDmgr=false add-node -DWasPassword=$was_password
/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties="/tmp/dx-onpremise/cluster/properties/post-federation.properties" -DWasUserid=$was_username -DWasPassword=$was_password -DPortalAdminId=$portal_username -DPortalAdminPwd=wpsadmin -Drelease.DbUser=$db_username -Drelease.DbPassword=$db_password cluster-node-config-post-federation -DWasPassword=$was_password -Drelease.DbPassword=$db_password -DPortalAdminPwd=$portal_password
/opt/HCL/wp_profile/ConfigEngine/ConfigEngine.sh  -DSaveParentProperties=true -DparentProperties="/tmp/dx-onpremise/cluster/properties/cluster-setup.properties" -DWasUserid=$was_username -DWasPassword=$was_password -DPortalAdminId=$portal_username -DPortalAdminPwd=wpsadmin cluster-node-config-cluster-setup -DPortalAdminPwd=$portal_password -DWasPassword=$was_password
