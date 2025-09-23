# Configure LDAP to the server, the LDAP server You can be updated by the wim.tar.gz located in the dxctl folder.  The file is coped to wp_profile/config/cells/dmgrCell01/ and extracted there then the wimconfig.xml is updated with the IP address.
echo 'Configure LDAP to the server ...'
cd /opt/HCL/wp_profile
cp /tmp/dx-onpremise/configureLDAP/wim.tar.gz ./config/cells/dmgrCell01/
cd ./config/cells/dmgrCell01/
tar -xvf wim.tar.gz
cp ./wim/config/wimconfig.xml /opt/HCL/AppServer/profiles/dmgr01/config/cells/dmgrCell01/wim/config
