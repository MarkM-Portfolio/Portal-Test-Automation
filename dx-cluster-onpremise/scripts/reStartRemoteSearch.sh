echo 'Restarting the remote search server ...'
sudo /opt/HCL/AppServer/profiles/prs_profile/bin/stopServer.sh server1 -username $1 -password $2
sudo /opt/HCL/AppServer/profiles/prs_profile/bin/startServer.sh server1
