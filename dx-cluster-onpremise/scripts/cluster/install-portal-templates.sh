mkdir /opt/IBM/WebSphere/PortalServer/profileTemplates/
cp /tmp/dx-onpremise/profileTemplates.zip /opt/IBM/WebSphere/PortalServer/profileTemplates/
cd /opt/IBM/WebSphere/PortalServer/profileTemplates/
unzip -o profileTemplates.zip 
./installPortalTemplates.sh /opt/IBM/WebSphere/AppServer
