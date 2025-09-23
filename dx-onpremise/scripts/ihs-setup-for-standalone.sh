cd /opt/HCL/wp_profile/bin
./wsadmin.sh -f '/opt/HCL/AppServer/bin/configureWebserverDefinition.jacl' webserver1 IHS '/opt/HCL/HTTPServer' '/opt/HCL/HTTPServer/conf/httpd.conf' 80 MAP_ALL '/opt/HCL/ihsPlugins' managed $1 $2 linux 8008  $3 $4 -profileName wp_profile -user $3 -password $4

cd /opt/HCL/wp_profile/bin
./GenPluginCfg.sh

cp /opt/HCL/wp_profile/config/cells/plugin-cfg.xml /opt/HCL/ihsPlugins/config/webserver1/

cp /opt/HCL/wp_profile/config/cells/dockerCell/nodes/$1/servers/webserver1/plugin-key.kdb /opt/HCL/ihsPlugins/config/webserver1/plugin-key.kdb

cp /opt/HCL/wp_profile/config/cells/dockerCell/nodes/$1/servers/webserver1/plugin-key.sth /opt/HCL/ihsPlugins/config/webserver1/plugin-key.sth
