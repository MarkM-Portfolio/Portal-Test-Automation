# Configure LDAP to the server, the LDAP server You can be updated by the wim.tar.gz located in the dxctl folder.  The file is coped to wp_profile/config/cells/dockerCell/ and extracted there then the wimconfig.xml is updated with the IP address.
echo 'Configure LDAP to the server ...'
cd /opt/IBM/WebSphere/wp_profile
cp /tmp/dx-onpremise/configureLDAP/wim.tar.gz ./config/cells/ci-linuxstal-39sht7rxCell/
cd ./config/cells/ci-linuxstal-39sht7rxCell/
tar -xvf wim.tar.gz
