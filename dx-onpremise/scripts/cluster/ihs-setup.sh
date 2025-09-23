
cd /opt/HCL/AppServer/bin/
./wsadmin.sh -f '/opt/HCL/AppServer/bin/configureWebserverDefinition.jacl' webserver1 IHS '/opt/HCL/HTTPServer' '/opt/HCL/HTTPServer/conf/httpd.conf' 80 MAP_ALL '/opt/HCL/ihsPlugins' managed $1 $2 linux 8008  $3 $4 -profileName dmgr01 -user $3 -password $4

cd /opt/HCL/AppServer/profiles/dmgr01/bin
./GenPluginCfg.sh

cp /opt/HCL/AppServer/profiles/dmgr01/config/cells/plugin-cfg.xml /opt/HCL/ihsPlugins/config/webserver1/


