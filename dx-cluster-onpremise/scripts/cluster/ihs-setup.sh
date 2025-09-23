
cd /opt/IBM/WebSphere/AppServer/bin/
./wsadmin.sh -f '/opt/IBM/WebSphere/AppServer/bin/configureWebserverDefinition.jacl' webserver1 IHS '/opt/IBM/WebSphere/HTTPServer' '/opt/IBM/WebSphere/HTTPServer/conf/httpd.conf' 80 MAP_ALL '/opt/IBM/WebSphere/ihsPlugins' managed $1 $2 linux 8008  $3 $4 -profileName dmgr01 -user $3 -password $4

cd /opt/IBM/WebSphere/AppServer/profiles/dmgr01/bin
./GenPluginCfg.sh

cp /opt/IBM/WebSphere/AppServer/profiles/dmgr01/config/cells/plugin-cfg.xml /opt/IBM/WebSphere/ihsPlugins/config/webserver1/


