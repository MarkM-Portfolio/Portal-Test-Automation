#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

source ~/.bash_profile

docker rm $(docker stop $(docker ps -a -q --filter name=dx_core))
docker_dx_core_image_id=$(docker images | grep "dxen" | awk '{ print $3 }')
docker run -d -u 0 -p 10039:10039 -p 10041:10041 -p 10200:10200 -p 10203:10203 -p  10202:10202 -p 10025:10025 -p 2809:2809 --name dx_core ${docker_dx_core_image_id}

echo "Setting up CTF"
cd /opt/git
docker cp ctf.care.full.install.zip dx_core:/home/dx_user
docker exec -i dx_core /bin/sh -c "
cd /home/dx_user;
mkdir shared;
cd shared;
unzip -qq ../ctf.care.full.install.zip;
curl -O https://artifactory.cwp.pnp-hcl.com/artifactory/quintana-generic/dx-build-output/ctf/CTF_BUILD_LABEL.zip;
cd care;
unzip -qq ../CTF_BUILD_LABEL.zip;
mkdir -p /home/dx_user/shared/care/ctf/lib;
mkdir -p /home/dx_user/shared/care/ctf/prereq;
chmod 755 -R /opt/HCL/ConfigEngine/;
cd /home/dx_user/shared/care/rt/ref;
. ./copysetup.sh;
cd /home/dx_user/shared/care;
sed -i \"s/WebSphere/HCL/g\" setupenv.properties;
. ./setupenv.sh;
cd /home/dx_user/shared/care/rt;
ant -Ddeploy=true care.deploy.ctf"

echo "Updating hostname and port values in ctf.properties"
docker exec -i dx_core /bin/sh -c "sed -i 's/@hostname@/INSTANCE_IP/g' /opt/HCL/ctf.properties"

echo "Building pluto.ext.ide.testrunner"
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/ && wsbld

echo "Copy servlet.war"
docker cp /opt/git/pluto.ext.ide.testrunner.servlet.war dx_core:/opt/HCL/

sleep 60s

echo "Deploy pluto_ext_ide_testrunner_servlet_war"
docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/AppServer/bin;
./wsadmin.sh  -lang jython -c \"AdminApp.install('/opt/HCL/pluto.ext.ide.testrunner.servlet.war', '[ -MapWebModToVH [[ .* .* default_host ]]   -appname pluto_ext_ide_testrunner_servlet_war]')\" -user wpsadmin -password wpsadmin"

echo "Deploying and starting the application"
docker cp /tmp/context_root.py dx_core:/opt/HCL/
docker cp /tmp/security_mapping.py dx_core:/opt/HCL/
docker exec -i dx_core /bin/sh -c "
cd /opt/HCL/AppServer/bin;
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/HCL/context_root.py;
./wsadmin.sh -user wpsadmin -password wpsadmin -lang jython -f /opt/HCL/security_mapping.py;
./wsadmin.sh -c \"AdminControl.invoke(AdminControl.queryNames('type=ApplicationManager,*'),'startApplication','pluto_ext_ide_testrunner_servlet_war')\" -lang jython -user wpsadmin -password wpsadmin"

echo "Running the IUT tests"
cd /opt/git/git-dx-clone/wcm.ext/wp/code/pluto.ext.ide.testrunner/test && wsbld do-run-tests


