cd /opt/IBM/WebSphere/wp_profile/bin
./wsadmin.sh -f '/opt/IBM/WebSphere/AppServer/bin/configureWebserverDefinition.jacl' webserver1 IHS '/opt/IBM/WebSphere/HTTPServer' '/opt/IBM/WebSphere/HTTPServer/conf/httpd.conf' 80 MAP_ALL '/opt/IBM/WebSphere/ihsPlugins' managed $1 $2 linux 8008  $3 $4 -profileName wp_profile -user $3 -password $4

cd /opt/IBM/WebSphere/wp_profile/bin
./GenPluginCfg.sh

cp /opt/IBM/WebSphere/wp_profile/config/cells/plugin-cfg.xml /opt/IBM/WebSphere/ihsPlugins/config/webserver1/

cp /opt/IBM/WebSphere/wp_profile/config/cells/ci-linuxstal-39sht7rxCell/nodes/$1/servers/webserver1/plugin-key.kdb /opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-key.kdb

cp /opt/IBM/WebSphere/wp_profile/config/cells/ci-linuxstal-39sht7rxCell/nodes/$1/servers/webserver1/plugin-key.sth /opt/IBM/WebSphere/ihsPlugins/config/webserver1/plugin-key.sth
