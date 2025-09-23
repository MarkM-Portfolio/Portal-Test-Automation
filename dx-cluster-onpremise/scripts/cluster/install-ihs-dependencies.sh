
mkdir /opt/ihsBinaries
mkdir /opt/IBM/WebSphere/ihsToolbox
mkdir /opt/IBM/WebSphere/HTTPServer
mkdir /opt/IBM/WebSphere/ihsPlugins

cd /tmp/dx-onpremise/ihs
unzip was.repo.9000.plugins.zip -d /opt/ihsBinaries/was.repo.9000.plugins/
unzip was.repo.9000.ihs.zip -d /opt/ihsBinaries/was.repo.9000.ihs/
unzip was.repo.9000.wct.zip -d /opt/ihsBinaries/was.repo.9000.wct/

cd /opt/IBM/InstallationManager/eclipse/tools

./imcl -acceptLicense input /tmp/dx-onpremise/scripts/helpers/http-server/ihs.xml -showProgress -log /tmp/dx-onpremise/scripts/helpers/http-server/ihs_log.xml

./imcl -acceptLicense input /tmp/dx-onpremise/scripts/helpers/http-server/plugins.xml -showProgress -log /tmp/dx-onpremise/scripts/helpers/http-server/plugins_log.xml

./imcl -acceptLicense input /tmp/dx-onpremise/scripts/helpers/http-server/toolbox.xml -showProgress -log /tmp/dx-onpremise/scripts/helpers/http-server/toolbox_log.xml
