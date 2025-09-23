echo 'Restarting the portal server ...'
cd /opt/HCL/wp_profile/bin
./stopServer.sh WebSphere_Portal -username $1 -password $2
./startServer.sh WebSphere_Portal 
