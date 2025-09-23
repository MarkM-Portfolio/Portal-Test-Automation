mkdir /opt/HCL/PortalServer/profileTemplates/
cp /tmp/dx-onpremise/profileTemplates.zip /opt/HCL/PortalServer/profileTemplates/
cd /opt/HCL/PortalServer/profileTemplates/
unzip -o profileTemplates.zip 
./installPortalTemplates.sh /opt/HCL/AppServer
